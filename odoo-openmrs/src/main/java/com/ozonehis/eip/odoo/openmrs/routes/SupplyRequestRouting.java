/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.routes;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.processors.SupplyRequestProcessor;
import lombok.Setter;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class SupplyRequestRouting extends RouteBuilder {

    private static final String SUPPLY_REQUEST_ID = "supply.request.id";

    @Autowired
    private SupplyRequestProcessor supplyRequestProcessor;

    @Override
    public void configure() {
        // spotless:off
        from("direct:fhir-supplyrequest")
            .routeId("supplyrequest-to-sale-order-router")
            .filter(body().isNotNull())
            .process(exchange -> {
                    SupplyRequest supplyRequest = exchange.getMessage().getBody(SupplyRequest.class);
                    exchange.setProperty(Constants.FHIR_RESOURCE_TYPE, supplyRequest.fhirType());
                    exchange.setProperty(
                            SUPPLY_REQUEST_ID, supplyRequest.getIdElement().getIdPart());
                    Bundle bundle = new Bundle();
                    bundle.addEntry().setResource(supplyRequest);
                    exchange.getMessage().setBody(bundle);
                })
            .to("direct:supplyrequest-to-sale-order-processor")
                .end();

        from("direct:supplyrequest-to-sale-order-processor")
            .routeId("supplyrequest-to-sale-order-processor")
            .process(supplyRequestProcessor)
            .log(LoggingLevel.INFO, "Processing SupplyRequest")
                .end();
        // spotless:on
    }
}
