package com.ozonehis.eip.route.prp;

import static com.ozonehis.eip.route.OdooTestConstants.URI_PRP_HANDLER;
import static org.openmrs.eip.mysql.watcher.WatcherConstants.PROP_EVENT;

import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.support.DefaultExchange;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.eip.mysql.watcher.Event;

public class OdooPrpHandlerRouteTest extends BasePrpRouteTest {
	
	protected static final String ROUTE_ID = "odoo-prp-handler";
	
	@EndpointInject("mock:odoo-person-handler")
	private MockEndpoint mockPersonHandlerEndpoint;
	
	@EndpointInject("mock:obs-to-odoo-resource")
	private MockEndpoint mockObsToOdooResEndpoint;
	
	@Before
	public void setup() throws Exception {
		mockPersonHandlerEndpoint.reset();
		mockObsToOdooResEndpoint.reset();
		advise(ROUTE_ID, new AdviceWithRouteBuilder() {
			
			@Override
			public void configure() {
				interceptSendToEndpoint("direct:odoo-person-handler").skipSendToOriginalEndpoint()
				        .to(mockPersonHandlerEndpoint);
				interceptSendToEndpoint("direct:obs-to-odoo-resource").skipSendToOriginalEndpoint()
				        .to(mockObsToOdooResEndpoint);
			}
			
		});
	}
	
	@Test
	public void shouldCallPersonHandlerForAPersonEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent("person", "1", "person-uuid", "c");
		exchange.setProperty(PROP_EVENT, event);
		mockPersonHandlerEndpoint.expectedMessageCount(1);
		mockObsToOdooResEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PRP_HANDLER, exchange);
		
		mockPersonHandlerEndpoint.assertIsSatisfied();
		mockObsToOdooResEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldCallObsHandlerForAnObsEvent() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent("obs", "1", "obs-uuid", "c");
		exchange.setProperty(PROP_EVENT, event);
		mockPersonHandlerEndpoint.expectedMessageCount(0);
		mockObsToOdooResEndpoint.expectedMessageCount(1);
		
		producerTemplate.send(URI_PRP_HANDLER, exchange);
		
		mockPersonHandlerEndpoint.assertIsSatisfied();
		mockObsToOdooResEndpoint.assertIsSatisfied();
	}
	
	@Test
	public void shouldIgnoreEventsForOtherEntities() throws Exception {
		Exchange exchange = new DefaultExchange(camelContext);
		Event event = createEvent("encounter", "1", "encounter-uuid", "c");
		exchange.setProperty(PROP_EVENT, event);
		mockPersonHandlerEndpoint.expectedMessageCount(0);
		mockObsToOdooResEndpoint.expectedMessageCount(0);
		
		producerTemplate.send(URI_PRP_HANDLER, exchange);
		
		mockPersonHandlerEndpoint.assertIsSatisfied();
		mockObsToOdooResEndpoint.assertIsSatisfied();
	}
	
}
