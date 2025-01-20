/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.openmrs.eip.fhir.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class MedicationRequestProcessor implements Processor {

    @Autowired
    private SaleOrderHandler saleOrderHandler;

    @Autowired
    private PartnerHandler partnerHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            MedicationRequest medicationRequest = null;
            Medication medication = null;

            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof MedicationRequest) {
                    medicationRequest = (MedicationRequest) resource;
                } else if (resource instanceof Medication) {
                    medication = (Medication) resource;
                }
            }

            if (patient == null || encounter == null || medicationRequest == null || medication == null) {
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient, Encounter, MedicationRequest and Medication",
                        exchange);
            } else {
                log.debug("Processing MedicationRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage().getHeader(Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String encounterVisitUuid = encounter.getPartOf().getReference().split("/")[1];
                Partner partner = partnerHandler.createOrUpdatePartner(producerTemplate, patient);
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (!medicationRequest.getStatus().equals(MedicationRequest.MedicationRequestStatus.CANCELLED)) {
                        SaleOrder saleOrder = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
                        if (saleOrder != null) {
                            saleOrderHandler.updateSaleOrderIfExistsWithSaleOrderLine(
                                    medicationRequest,
                                    saleOrder,
                                    encounterVisitUuid,
                                    partner.getPartnerId(),
                                    patient.getIdPart(),
                                    producerTemplate);
                        } else {
                            saleOrderHandler.createSaleOrderWithSaleOrderLine(
                                    medicationRequest,
                                    encounter,
                                    partner,
                                    encounterVisitUuid,
                                    patient.getIdPart(),
                                    producerTemplate);
                        }
                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                        saleOrderHandler.deleteSaleOrderLine(medicationRequest, encounterVisitUuid, producerTemplate);
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS
                    saleOrderHandler.deleteSaleOrderLine(medicationRequest, encounterVisitUuid, producerTemplate);
                    saleOrderHandler.cancelSaleOrderWhenNoSaleOrderLine(
                            partner.getPartnerId(), encounterVisitUuid, producerTemplate);
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing MedicationRequest", exchange, e);
        }
    }
}
