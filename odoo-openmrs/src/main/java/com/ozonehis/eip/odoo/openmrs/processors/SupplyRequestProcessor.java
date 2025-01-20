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
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.EncounterHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.openmrs.PatientHandler;
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
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.openmrs.eip.fhir.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SupplyRequestProcessor implements Processor {

    @Autowired
    private SaleOrderHandler saleOrderHandler;

    @Autowired
    private PartnerHandler partnerHandler;

    @Autowired
    private PatientHandler patientHandler;

    @Autowired
    private EncounterHandler encounterHandler;

    @Override
    public void process(Exchange exchange) {
        try (ProducerTemplate producerTemplate = exchange.getContext().createProducerTemplate()) {
            Bundle bundle = exchange.getMessage().getBody(Bundle.class);
            List<Bundle.BundleEntryComponent> entries = bundle.getEntry();

            Patient patient = null;
            Encounter encounter = null;
            SupplyRequest supplyRequest = null;
            for (Bundle.BundleEntryComponent entry : entries) {
                Resource resource = entry.getResource();
                if (resource instanceof Patient) {
                    patient = (Patient) resource;
                } else if (resource instanceof Encounter) {
                    encounter = (Encounter) resource;
                } else if (resource instanceof SupplyRequest) {
                    supplyRequest = (SupplyRequest) resource;
                }
            }

            if (supplyRequest == null) {
                throw new CamelExecutionException("Invalid Bundle. Bundle must contain SupplyRequest", exchange);
            }
            log.info("Processing SupplyRequest for Patient with UUID {}", supplyRequest.getId());
            if (patient == null) {
                patient = patientHandler.getPatientByPatientID(
                        supplyRequest.getDeliverTo().getReference().split("/")[1]);
            }
            if (encounter == null) {
                encounter = encounterHandler.getEncounterByEncounterID(
                        supplyRequest.getReasonReference().get(0).getReference().split("/")[1]);
            }
            if (patient == null || encounter == null) {
                throw new CamelExecutionException(
                        "Invalid Bundle. Bundle must contain Patient and Encounter", exchange);
            } else {
                log.debug("Processing SupplyRequest for Patient with UUID {}", patient.getIdPart());
                String eventType = exchange.getMessage().getHeader(Constants.HEADER_FHIR_EVENT_TYPE, String.class);
                if (eventType == null) {
                    throw new IllegalArgumentException("Event type not found in the exchange headers.");
                }
                String encounterVisitUuid = encounter.getPartOf().getReference().split("/")[1];
                Partner partner = partnerHandler.createOrUpdatePartner(producerTemplate, patient);
                if ("c".equals(eventType) || "u".equals(eventType)) {
                    if (supplyRequest.getStatus().equals(SupplyRequest.SupplyRequestStatus.ACTIVE)) {
                        SaleOrder saleOrder = saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(encounterVisitUuid);
                        if (saleOrder != null) {
                            saleOrderHandler.updateSaleOrderIfExistsWithSaleOrderLine(
                                    supplyRequest,
                                    saleOrder,
                                    encounterVisitUuid,
                                    partner.getPartnerId(),
                                    patient.getIdPart(),
                                    producerTemplate);
                        } else {
                            saleOrderHandler.createSaleOrderWithSaleOrderLine(
                                    supplyRequest,
                                    encounter,
                                    partner,
                                    encounterVisitUuid,
                                    patient.getIdPart(),
                                    producerTemplate);
                        }
                    } else {
                        // Executed when MODIFY option is selected in OpenMRS
                        saleOrderHandler.deleteSaleOrderLine(supplyRequest, encounterVisitUuid, producerTemplate);
                    }
                } else if ("d".equals(eventType)) {
                    // Executed when DISCONTINUE option is selected in OpenMRS
                    saleOrderHandler.deleteSaleOrderLine(supplyRequest, encounterVisitUuid, producerTemplate);
                    saleOrderHandler.cancelSaleOrderWhenNoSaleOrderLine(
                            partner.getPartnerId(), encounterVisitUuid, producerTemplate);
                } else {
                    throw new IllegalArgumentException("Unsupported event type: " + eventType);
                }
            }
        } catch (Exception e) {
            throw new CamelExecutionException("Error processing SupplyRequest", exchange, e);
        }
    }
}
