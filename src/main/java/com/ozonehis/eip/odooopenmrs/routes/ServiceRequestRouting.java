/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.processors.ServiceRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class ServiceRequestRouting extends RouteBuilder {

    private static final String SERVICE_REQUEST_TO_QUOTATION_ROUTER = "service-request-to-quotation-router";

    private static final String SERVICE_REQUEST_TO_QUOTATION_PROCESSOR = "service-request-to-quotation-processor";

    private static final String SERVICE_REQUEST_ID = "service.request.id";

    private static final String SERVICE_REQUEST_INCLUDE_PARAMS = "ServiceRequest:encounter,ServiceRequest:patient";

    private static final String SEARCH_PARAMS =
            "id=${exchangeProperty." + SERVICE_REQUEST_ID + "}&resource=${exchangeProperty."
                    + Constants.FHIR_RESOURCE_TYPE + "}&include=" + SERVICE_REQUEST_INCLUDE_PARAMS;

    @Autowired
    private ServiceRequestProcessor serviceRequestProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:fhir-servicerequest")
                .routeId(SERVICE_REQUEST_TO_QUOTATION_ROUTER)
                .process(exchange -> {
                    ServiceRequest serviceRequest = exchange.getMessage().getBody(ServiceRequest.class);
                    exchange.setProperty(Constants.FHIR_RESOURCE_TYPE, serviceRequest.fhirType());
                    exchange.setProperty(
                            SERVICE_REQUEST_ID, serviceRequest.getIdElement().getIdPart());
                    exchange.getMessage().setBody(serviceRequest);
                })
                .toD("odoo://?" + SEARCH_PARAMS)
                .to("direct:service-request-to-quotation-processor")
                .end();

        from("direct:service-request-to-quotation-processor")
                .routeId(SERVICE_REQUEST_TO_QUOTATION_PROCESSOR)
                .process(serviceRequestProcessor)
                .log(
                        LoggingLevel.DEBUG,
                        "ServiceRequest with ID ${exchangeProperty." + SERVICE_REQUEST_ID + "} processed.")
                .end();
        // spotless:on
    }
}
