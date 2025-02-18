/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.ozonehis.camel.test.infra.odoo.services.OdooService;
import com.ozonehis.camel.test.infra.odoo.services.OdooServiceFactory;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.component.OdooComponent;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CountryHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CountryStateHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderLineHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.UomHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.ObservationHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.PatientHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.SaleOrderMapper;
import com.ozonehis.eip.odoo.openmrs.processors.MedicationRequestProcessor;
import com.ozonehis.eip.odoo.openmrs.processors.PatientProcessor;
import com.ozonehis.eip.odoo.openmrs.processors.ServiceRequestProcessor;
import com.ozonehis.eip.odoo.openmrs.processors.SupplyRequestProcessor;
import com.ozonehis.eip.odoo.openmrs.routes.MedicationRequestRouting;
import com.ozonehis.eip.odoo.openmrs.routes.PatientRouting;
import com.ozonehis.eip.odoo.openmrs.routes.ProcedureRouting;
import com.ozonehis.eip.odoo.openmrs.routes.ServiceRequestRouting;
import com.ozonehis.eip.odoo.openmrs.routes.SupplyRequestRouting;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.Getter;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.CamelContextExtension;
import org.apache.camel.test.infra.core.DefaultCamelContextExtension;
import org.apache.camel.test.infra.core.annotations.ContextFixture;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@Getter
@ActiveProfiles("test")
@CamelSpringBootTest
@SpringBootTest(classes = {TestSpringConfiguration.class})
public abstract class BaseRouteIntegrationTest {

    @Autowired
    private Environment environment;

    private OdooClient odooClient;

    private static final String ODOO_SERVER_URL = "http://localhost:8069";

    private static final String ODOO_DATABASE = "odoo";

    private static final String ODOO_USERNAME = "admin";

    private static final String ODOO_PASSWORD = "admin";

    private static final String odooCustomerWeightField = "x_customer_weight";

    private static final String odooCustomerDobField = "x_customer_dob";

    protected static final List<String> orderDefaultAttributes =
            asList("id", "client_order_ref", "partner_id", "state", "order_line");

    protected static final List<String> partnerDefaultAttributes =
            asList("id", "name", "ref", "street", "street2", "city", "zip", "active", "comment");

    protected WireMockServer wireMockServer = new WireMockServer(8080);

    protected void mockOpenmrsFhirServer() {
        // Mock OpenMRS FHIR API metadata endpoint
        wireMockServer.start();
        configureFor("localhost", 8080);
        stubFor(get(urlMatching("/openmrs/ws/fhir2/R4/metadata"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJSON("metadata.json"))));
    }

    @RegisterExtension
    protected static CamelContextExtension contextExtension = new DefaultCamelContextExtension();

    @RegisterExtension
    protected static final OdooService odooService = OdooServiceFactory.createSingletonService();

    @ContextFixture
    public void configureContext(CamelContext context) {
        OdooComponent odooComponent = context.getComponent("odoo", OdooComponent.class);
        odooComponent.setOdooUtils(getOdooUtils());
        odooComponent.setOdooClient(getOdooClient());
    }

    protected static OdooClient createOdooClient() {
        return new OdooClient(ODOO_SERVER_URL, ODOO_DATABASE, ODOO_USERNAME, ODOO_PASSWORD);
    }

    public OdooClient getOdooClient() {
        if (odooClient == null) {
            odooClient = createOdooClient();
        }
        return odooClient;
    }

    public OdooUtils getOdooUtils() {
        OdooUtils odooUtils = new OdooUtils();
        odooUtils.setEnvironment(environment);
        return odooUtils;
    }

    protected @Nonnull CamelContext getContextWithRouting(CamelContext context) throws Exception {
        OdooUtils odooUtils = getOdooUtils();

        CountryHandler countryHandler = new CountryHandler();
        countryHandler.setOdooClient(getOdooClient());

        CountryStateHandler countryStateHandler = new CountryStateHandler();
        countryStateHandler.setOdooClient(getOdooClient());

        SaleOrderLineMapper<Resource> saleOrderLineMapper = new SaleOrderLineMapper<>();

        UomHandler uomHandler = new UomHandler();
        uomHandler.setOdooClient(getOdooClient());
        uomHandler.setOdooUtils(odooUtils);

        ProductHandler productHandler = new ProductHandler();
        productHandler.setOdooClient(getOdooClient());
        productHandler.setOdooUtils(odooUtils);

        SaleOrderMapper saleOrderMapper = new SaleOrderMapper();

        SaleOrderLineHandler saleOrderLineHandler = new SaleOrderLineHandler();
        saleOrderLineHandler.setOdooClient(getOdooClient());
        saleOrderLineHandler.setProductHandler(productHandler);
        saleOrderLineHandler.setUomHandler(uomHandler);
        saleOrderLineHandler.setSaleOrderLineMapper(saleOrderLineMapper);
        saleOrderLineHandler.setOdooUtils(odooUtils);

        PartnerMapper partnerMapper = new PartnerMapper();
        partnerMapper.setCountryHandler(countryHandler);
        partnerMapper.setCountryStateHandler(countryStateHandler);

        PartnerHandler partnerHandler = new PartnerHandler();
        partnerHandler.setOdooClient(getOdooClient());
        partnerHandler.setPartnerMapper(partnerMapper);
        partnerHandler.setOdooUtils(odooUtils);
        partnerHandler.setOdooCustomerDobField(odooCustomerDobField);
        partnerHandler.setPartnerDefaultAttributes(partnerDefaultAttributes);

        // Setup IGenericClient
        FhirContext fhirContext = FhirContext.forR4();
        String serverBase = "http://localhost:8080/openmrs/ws/fhir2/R4";
        IGenericClient client = fhirContext.newRestfulGenericClient(serverBase);

        String username = "admin";
        String password = "Admin123";
        BasicAuthInterceptor authInterceptor = new BasicAuthInterceptor(username, password);
        client.registerInterceptor(authInterceptor);

        PatientHandler patientHandler = new PatientHandler(client);
        patientHandler.setOpenmrsFhirClient(client);

        EncounterHandler encounterHandler = new EncounterHandler();
        encounterHandler.setOpenmrsFhirClient(client);

        ObservationHandler observationHandler = new ObservationHandler();
        observationHandler.setOpenmrsFhirClient(client);

        SaleOrderHandler saleOrderHandler = new SaleOrderHandler();
        saleOrderHandler.setOdooClient(getOdooClient());
        saleOrderHandler.setSaleOrderLineHandler(saleOrderLineHandler);
        saleOrderHandler.setSaleOrderMapper(saleOrderMapper);
        saleOrderHandler.setProductHandler(productHandler);
        saleOrderHandler.setObservationHandler(observationHandler);
        saleOrderHandler.setWeightConcept("5089AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        saleOrderHandler.setOdooCustomerWeightField(odooCustomerWeightField);
        saleOrderHandler.setOdooCustomerDobField(odooCustomerDobField);
        saleOrderHandler.setOrderDefaultAttributes(orderDefaultAttributes);
        saleOrderHandler.setOdooUtils(odooUtils);

        PatientProcessor patientProcessor = new PatientProcessor();
        patientProcessor.setPartnerHandler(partnerHandler);
        patientProcessor.setMapper(partnerMapper);

        PatientRouting patientRouting = new PatientRouting();
        patientRouting.setPatientProcessor(patientProcessor);
        patientRouting.setPatientSyncEnabled(true);

        MedicationRequestProcessor medicationRequestProcessor = new MedicationRequestProcessor();
        medicationRequestProcessor.setSaleOrderHandler(saleOrderHandler);
        medicationRequestProcessor.setPartnerHandler(partnerHandler);

        MedicationRequestRouting medicationRequestRouting = new MedicationRequestRouting();
        medicationRequestRouting.setMedicationRequestProcessor(medicationRequestProcessor);

        ServiceRequestProcessor serviceRequestProcessor = new ServiceRequestProcessor();
        serviceRequestProcessor.setSaleOrderHandler(saleOrderHandler);
        serviceRequestProcessor.setPartnerHandler(partnerHandler);

        ServiceRequestRouting serviceRequestRouting = new ServiceRequestRouting();
        serviceRequestRouting.setServiceRequestProcessor(serviceRequestProcessor);

        ProcedureRouting procedureRouting = new ProcedureRouting();
        procedureRouting.setServiceRequestProcessor(serviceRequestProcessor);

        SupplyRequestProcessor supplyRequestProcessor = new SupplyRequestProcessor();
        supplyRequestProcessor.setSaleOrderHandler(saleOrderHandler);
        supplyRequestProcessor.setPartnerHandler(partnerHandler);
        supplyRequestProcessor.setEncounterHandler(encounterHandler);
        supplyRequestProcessor.setPatientHandler(patientHandler);

        SupplyRequestRouting supplyRequestRouting = new SupplyRequestRouting();
        supplyRequestRouting.setSupplyRequestProcessor(supplyRequestProcessor);

        context.addRoutes(patientRouting);
        context.addRoutes(medicationRequestRouting);
        context.addRoutes(serviceRequestRouting);
        context.addRoutes(procedureRouting);
        context.addRoutes(supplyRequestRouting);

        return context;
    }

    protected boolean hasRoute(CamelContext context, String routeId) {
        return context.getRoute(routeId) != null;
    }

    /**
     * Send a body and headers to an endpoint.
     *
     * @param endpoint the endpoint to send the body to.
     * @param body     the body to send.
     * @param headers  the headers to send.
     */
    protected void sendBodyAndHeaders(String endpoint, Object body, Map<String, Object> headers) {
        contextExtension
                .getProducerTemplate()
                .sendBodyAndHeaders(contextExtension.getContext().getEndpoint(endpoint), body, headers);
    }

    /**
     * Load resource from a file path.
     *
     * @param filePath the file path of the resource to load.
     * @param resource resource object
     * @param <T>      The type of the resource to load e.g., Patient, Encounter, etc.
     * @return resource object
     */
    @SuppressWarnings("unchecked")
    protected <T extends Resource> T loadResource(String filePath, T resource) {
        FhirContext ctx = FhirContext.forR4();
        return (T) ctx.newJsonParser().parseResource(resource.getClass(), readJSON(filePath));
    }

    /**
     * Read JSON file from the classpath.
     *
     * @param filePath the file path of the JSON file to read.
     * @return JSON content as a string
     */
    protected String readJSON(String filePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
        if (is == null) {
            throw new IllegalArgumentException("File not found! " + filePath);
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
