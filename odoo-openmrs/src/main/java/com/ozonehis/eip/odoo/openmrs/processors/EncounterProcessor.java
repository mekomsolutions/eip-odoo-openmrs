/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EncounterProcessor implements Processor {

    @Autowired
    private SaleOrderHandler saleOrderHandler;

    @Override
    public void process(Exchange exchange) {
        Message message = exchange.getMessage();
        Encounter encounter = message.getBody(Encounter.class);
        if (encounter != null && encounter.hasPeriod() && encounter.getPeriod().hasEnd()) {
            String encounterVisitUuid = encounter.getIdPart(); // TODO: Check if this should be referenceId
            SaleOrder saleOrder = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
            if (saleOrder != null) {
                Map<String, Object> headers = new HashMap<>();
                // Check if Sale Order needs to be moved from `draft` state to `sale` state
                // saleOrder.setOrderState("sale");
                headers.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(saleOrder.getOrderId()));
                headers.put(HEADER_FHIR_EVENT_TYPE, "u");

                exchange.getMessage().setHeaders(headers);
                exchange.getMessage().setBody(saleOrder);
                exchange.setProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER, false);
            } else {
                exchange.setProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER, true);
            }
        } else {
            exchange.setProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER, true);
        }
    }
}
