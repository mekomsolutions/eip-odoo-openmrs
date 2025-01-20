/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.processors.ServiceRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ProcedureRouting extends RouteBuilder {

    private static final String SERVICE_REQUEST_ID = "service.request.id";

    @Autowired
    private ServiceRequestProcessor serviceRequestProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:fhir-procedure")
                .routeId("procedure-to-sale-order-router")
                .filter(body().isNotNull())
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getMessage().getBody(ServiceRequest.class);
                    exchange.setProperty(Constants.FHIR_RESOURCE_TYPE, serviceRequest.fhirType());
                    exchange.setProperty(
                            SERVICE_REQUEST_ID, serviceRequest.getIdElement().getIdPart());
                    Bundle bundle = new Bundle();
                    bundle.addEntry().setResource(serviceRequest);
                    exchange.getMessage().setBody(bundle);
                })
                .to("direct:procedure-to-sale-order-processor")
                .end();

        from("direct:procedure-to-sale-order-processor")
                .routeId("procedure-to-sale-order-processor")
                .process(serviceRequestProcessor)
                .log(LoggingLevel.INFO, "Processing Procedure")
                .end();
        // spotless:on
    }
}
