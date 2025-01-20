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
import com.ozonehis.eip.odoo.openmrs.model.Uom;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class UomHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private OdooUtils odooUtils;

    public Uom getUom(String externalId) {
        Object[] records = odooClient.searchAndRead(
                Constants.IR_MODEL,
                asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", externalId)),
                null);
        if (records == null) {
            throw new EIPException(String.format("Got null response while fetching for Uom with id %s", externalId));
        } else if (records.length == 1) {
            log.debug("Uom exists with id {} record {}", externalId, records[0]);
            return odooUtils.convertToObject((Map<String, Object>) records[0], Uom.class);
        } else if (records.length == 0) {
            log.warn("No Uom found with id {}", externalId);
            throw new EIPException(String.format("No Uom found with id %s", externalId));
        } else {
            log.warn("Multiple Uom exists with id {}", externalId);
            throw new EIPException(String.format("Multiple Uom exists with id %s", externalId));
        }
    }
}
