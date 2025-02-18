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
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class PartnerHandler {

    @Value("${odoo.customer.dob.field}")
    private String odooCustomerDobField;

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private PartnerMapper partnerMapper;

    @Autowired
    private OdooUtils odooUtils;

    public List<String> partnerDefaultAttributes;

    public Partner getPartnerByID(String partnerRefID) {
        partnerDefaultAttributes = asList("id", "name", "ref", "street", "street2", "city", "zip", "active", "comment");
        Object[] records = odooClient.searchAndRead(
                Constants.PARTNER_MODEL, List.of(asList("ref", "=", partnerRefID)), partnerDefaultAttributes);
        if (records == null) {
            throw new EIPException(
                    String.format("Got null response while searching for Partner with reference id %s", partnerRefID));
        } else if (records.length == 1) {
            log.debug("Partner exists with reference id {} record {}", partnerRefID, records[0]);
            return odooUtils.convertToObject((Map<String, Object>) records[0], Partner.class);
        } else if (records.length == 0) {
            log.warn("No Partner found with reference id {}", partnerRefID);
            return null;
        } else {
            log.warn("Multiple Partners exists with reference id {}", partnerRefID);
            throw new EIPException(String.format("Multiple Partners exists with reference id%s", partnerRefID));
        }
    }

    public Partner createOrUpdatePartner(ProducerTemplate producerTemplate, Patient patient) {
        Partner fetchedPartner = getPartnerByID(patient.getIdPart());
        if (fetchedPartner != null && fetchedPartner.getPartnerId() > 0) {
            int partnerId = fetchedPartner.getPartnerId();
            log.info("Partner with reference id {} already exists, updating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            partner.setPartnerId(partnerId);
            sendPartner(producerTemplate, "direct:odoo-update-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        } else {
            log.info("Partner with reference id {} does not exist, creating...", patient.getIdPart());
            Partner partner = partnerMapper.toOdoo(patient);
            sendPartner(producerTemplate, "direct:odoo-create-partner-route", partner);
            return getPartnerByID(partner.getPartnerRef());
        }
    }

    public void sendPartner(ProducerTemplate producerTemplate, String endpointUri, Partner partner) {
        Map<String, Object> headers = new HashMap<>();
        if (endpointUri.contains("update")) {
            headers.put(Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(partner.getPartnerId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, partner, headers);
    }
}
