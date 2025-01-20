/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.odoo;

import com.ozonehis.eip.odoo.openmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SaleOrderLineMapper<R extends Resource> implements ToOdooMapping<R, SaleOrderLine> {

    @Override
    public SaleOrderLine toOdoo(R resource) {
        SaleOrderLine saleOrderLine = new SaleOrderLine();
        if (resource instanceof ServiceRequest serviceRequest) {
            saleOrderLine.setSaleOrderLineProductUomQty(1.0f); // default quantity is 1 for serviceRequests.
            String requesterDisplay = serviceRequest.getRequester().getDisplay();
            String serviceDisplay = serviceRequest.getCode().getText();
            saleOrderLine.setSaleOrderLineName(serviceDisplay + " | Orderer: " + requesterDisplay);

        } else if (resource instanceof MedicationRequest medicationRequest) {
            if (medicationRequest.hasDispenseRequest()) {
                if (medicationRequest.getDispenseRequest().hasQuantity()) {
                    Quantity quantity = medicationRequest.getDispenseRequest().getQuantity();
                    saleOrderLine.setSaleOrderLineProductUomQty(
                            quantity.getValue().floatValue());
                    saleOrderLine.setSaleOrderLineProductUom(quantity.getCode());
                }
            }

            String requesterDisplay = medicationRequest.getRequester().getDisplay();
            String medicationDisplay =
                    medicationRequest.getMedicationReference().getDisplay();
            saleOrderLine.setSaleOrderLineName(medicationDisplay + " | "
                    + constructDosageInstructionsText(medicationRequest) + " | Orderer: " + requesterDisplay);

        } else if (resource instanceof SupplyRequest supplyRequest) {
            log.info("In SaleOrderLineMapper {}", supplyRequest.getQuantity());

            if (supplyRequest.hasQuantity()) {
                Quantity quantity = supplyRequest.getQuantity();
                saleOrderLine.setSaleOrderLineProductUomQty(quantity.getValue().floatValue());
                saleOrderLine.setSaleOrderLineProductUom(quantity.getCode());
            }

            String requesterDisplay = supplyRequest.getRequester().getDisplay();
            Reference supplyRequestReference = (Reference) supplyRequest.getItem();
            String supplyRequestDisplay = supplyRequestReference.getDisplay();
            saleOrderLine.setSaleOrderLineName(supplyRequestDisplay + " | Orderer: " + requesterDisplay);
        } else {
            throw new IllegalArgumentException("Sale Order Mapper Unsupported resource type: "
                    + resource.getClass().getName());
        }
        return saleOrderLine;
    }

    protected String constructDosageInstructionsText(MedicationRequest medicationRequest) {
        Dosage dosage = medicationRequest.getDosageInstructionFirstRep();
        StringBuilder dosageInstructions = new StringBuilder();

        String dispenseQty = String.valueOf(
                medicationRequest.getDispenseRequest().getQuantity().getValue());
        String dispenseQtyUnit = String.valueOf(
                medicationRequest.getDispenseRequest().getQuantity().getUnit());

        if (dispenseQty != null && dispenseQtyUnit != null) {
            dosageInstructions
                    .append(dispenseQty)
                    .append(" ")
                    .append(dispenseQtyUnit)
                    .append(" | ");
        }

        if (dosage.hasDoseAndRate()) {
            dosageInstructions
                    .append(dosage.getDoseAndRateFirstRep().getDoseQuantity().getValue())
                    .append(" ")
                    .append(dosage.getDoseAndRateFirstRep().getDoseQuantity().getUnit());
        }

        appendWithConditionalHyphen(
                dosageInstructions, dosage.hasRoute() ? dosage.getRoute().getText() : null);
        appendWithConditionalHyphen(
                dosageInstructions,
                dosage.hasTiming() && dosage.getTiming().getCode() != null
                        ? dosage.getTiming().getCode().getText()
                        : null);
        appendWithConditionalHyphen(
                dosageInstructions,
                dosage.hasTiming()
                                && dosage.getTiming().getRepeat().getDuration() != null
                                && dosage.getTiming().getRepeat().getDurationUnit() != null
                        ? dosage.getTiming().getRepeat().getDuration().toString() + " "
                                + dosage.getTiming()
                                        .getRepeat()
                                        .getDurationUnit()
                                        .getDisplay()
                        : null);

        appendWithConditionalHyphen(dosageInstructions, dosage.getText());

        return dosageInstructions.toString();
    }

    private void appendWithConditionalHyphen(StringBuilder builder, String text) {
        if (text != null && !builder.isEmpty() && !builder.toString().endsWith("| ")) {
            builder.append(" - ");
        }
        if (text != null) {
            builder.append(text);
        }
    }
}
