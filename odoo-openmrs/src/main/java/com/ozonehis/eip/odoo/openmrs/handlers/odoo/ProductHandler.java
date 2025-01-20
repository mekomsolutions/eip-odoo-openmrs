/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.odoo;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ProductHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private OdooUtils odooUtils;

    public Product getProduct(Resource resource) {
        String externalId = getProductExternalId(resource);

        Object[] records = odooClient.searchAndRead(
                Constants.IR_MODEL,
                asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", externalId)),
                null);
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while fetching for Product with id %s", externalId));
        } else if (records.length == 1) {
            log.debug("Product exists with id {} record {}", externalId, records[0]);
            return odooUtils.convertToObject((Map<String, Object>) records[0], Product.class);
        } else if (records.length == 0) {
            log.warn("No Product found with id {}", externalId);
            return null;
        } else {
            log.warn("Multiple Products exists with id {}", externalId);
            throw new EIPException(String.format("Multiple Products exists with id %s", externalId));
        }
    }

    private String getProductExternalId(Resource resource) {
        if (resource instanceof ServiceRequest serviceRequest) {
            String id = serviceRequest.getCode().getCodingFirstRep().getCode();
            log.debug("ProductHandler: ServiceRequest code {}", id);
            return id;
        } else if (resource instanceof MedicationRequest medicationRequest) {
            String id =
                    medicationRequest.getMedicationReference().getReference().split("/")[1];
            log.debug("ProductHandler: MedicationRequest reference {}", id);
            return id;
        } else if (resource instanceof SupplyRequest supplyRequest) {
            String id = supplyRequest.getItemReference().getReference().split("/")[1];
            log.debug("ProductHandler: SupplyRequest reference {}", id);
            return id;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported resource type: " + resource.getClass().getName());
        }
    }
}
