/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.odoo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import java.util.Collections;
import java.util.List;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.hl7.fhir.r4.model.Timing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SaleOrderLineMapperTest {

    public static final String MEDICATION_REQUEST_ID = "1cdda1ce-7f98-4bc7-9d20-8b4e953d972a";

    public static final String MEDICATION_ID = "c95ab33a-8558-4705-8a7a-3b4e580270c7";

    private static final String SERVICE_REQUEST_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String TABLET_UNIT = "Tablet";

    private static final String COMPLETE_BLOOD_COUNT_CODE = "446ac22c-24f2-40ad-98f4-65f026f434e9";

    private static final String COMPLETE_BLOOD_COUNT_DISPLAY = "Complete Blood Count";

    private static final String COMPLETE_BLOOD_COUNT_ID = "3d597a15-b08f-4c35-8a42-0c15f9a19fa6";

    private static final String PRACTITIONER_ID = "e5ca6578-fb37-4900-a054-c68db82a551c";

    private static final String SUPPLY_REQUEST_ID = "9gv050e1-s1be-5b4c-b107-c48d2db47r11";

    private static final String ENCOUNTER_ID = "4d6d21cc-a6a5-4714-9c44-631d9d4cb3fc";

    private static final String ADHESIVE_CODE = "90165e66-615d-4219-ba2c-50ed46a51bff";

    private static final String QUANTITY_CODE = "162396AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private static final String ADHESIVE_DISPLAY = "Adhesive 5cm x 9m";

    private static final String PATIENT_ID = "3ee4f5fc-6299-4c0e-a56e-dad957118edc";

    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    @BeforeEach
    public void setUp() {
        saleOrderLineMapper = new SaleOrderLineMapper<>();
    }

    @Test
    public void shouldMapServiceRequestToSaleOrderLine() {
        // setup
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setId(SERVICE_REQUEST_ID);

        // code
        CodeableConcept codeableConcept = new CodeableConcept();
        Coding coding = new Coding();
        coding.setCode(COMPLETE_BLOOD_COUNT_CODE);
        coding.setDisplay(COMPLETE_BLOOD_COUNT_DISPLAY);
        coding.setId(COMPLETE_BLOOD_COUNT_ID);
        codeableConcept.addCoding(coding);
        codeableConcept.setText(COMPLETE_BLOOD_COUNT_DISPLAY);
        serviceRequest.setCode(codeableConcept);

        Reference requester = new Reference();
        requester.setId("1cdda1ce-7f98-4bc7-9d20-8b4e953d972a");
        requester.setReference("Practitioner/" + PRACTITIONER_ID);
        requester.setDisplay("John Doe");
        serviceRequest.setRequester(requester);

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(serviceRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(1.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(COMPLETE_BLOOD_COUNT_DISPLAY + " | Orderer: John Doe", saleOrderLine.getSaleOrderLineName());
    }

    @Test
    public void shouldMapMedicationRequestToSaleOrderLine() {
        // setup
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId(MEDICATION_REQUEST_ID);

        // dispense request
        medicationRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setQuantity(new Quantity().setValue(10)));
        medicationRequest.setMedication(new Reference("Medication/" + MEDICATION_ID).setDisplay("medication"));
        medicationRequest.setRequester(new Reference().setDisplay("requester"));
        MedicationRequest.MedicationRequestDispenseRequestComponent dispenseRequest =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

        // quantity
        Quantity quantity = new Quantity();
        quantity.setValue(10);
        quantity.setUnit(TABLET_UNIT);
        quantity.setCode("15AAAAAAAAAAA");
        dispenseRequest.setQuantity(quantity);
        medicationRequest.setDispenseRequest(dispenseRequest);

        // dosage instruction
        Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
        Quantity doseQuantity = new Quantity();
        doseQuantity.setValue(7);
        doseQuantity.setUnit(TABLET_UNIT);
        doseAndRate.setDose(doseQuantity);
        Dosage dosage = new Dosage();
        dosage.setDoseAndRate(List.of(doseAndRate));

        Timing timing = new Timing();
        timing.setCode(new CodeableConcept().setText("thrice daily"));
        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
        repeat.setFrequency(3);
        repeat.setDuration(10);
        repeat.setDurationUnit(Timing.UnitsOfTime.D);
        timing.setRepeat(repeat);
        dosage.setTiming(timing);
        medicationRequest.setDosageInstruction(List.of(dosage));

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(medicationRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(10.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals("15AAAAAAAAAAA", saleOrderLine.getSaleOrderLineProductUom());
        assertEquals(
                "medication | 10 Tablet | 7 Tablet - thrice daily - 10 day | Orderer: requester",
                saleOrderLine.getSaleOrderLineName());
    }

    @Test
    public void shouldMapSupplyRequestToSaleOrderLine() {
        // setup
        SupplyRequest supplyRequest = new SupplyRequest();
        supplyRequest.setId(SUPPLY_REQUEST_ID);
        supplyRequest.setItem(
                new Reference().setReference("MedicalSupply/" + ADHESIVE_CODE).setDisplay(ADHESIVE_DISPLAY));
        supplyRequest.setReasonReference(Collections.singletonList(
                new Reference().setType("Encounter").setReference("Encounter/" + ENCOUNTER_ID)));
        supplyRequest.setQuantity(new Quantity().setValue(10).setCode(QUANTITY_CODE));
        supplyRequest.setRequester(new Reference().setReference(PRACTITIONER_ID).setDisplay("John Doe"));
        supplyRequest.setDeliverTo(
                new Reference().setReference("Patient/" + PATIENT_ID).setDisplay("Tim"));
        supplyRequest.setStatus(SupplyRequest.SupplyRequestStatus.ACTIVE);

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(supplyRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(10.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(ADHESIVE_DISPLAY + " | Orderer: John Doe", saleOrderLine.getSaleOrderLineName());
    }

    @Test
    public void shouldMapMedicationRequestWithFreeTextToSaleOrderLine() {
        // setup
        MedicationRequest medicationRequest = new MedicationRequest();
        medicationRequest.setId(MEDICATION_REQUEST_ID);

        // dispense request
        medicationRequest.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setQuantity(new Quantity().setValue(10)));
        medicationRequest.setMedication(new Reference("Medication/" + MEDICATION_ID).setDisplay("medication"));
        medicationRequest.setRequester(new Reference().setDisplay("requester"));
        MedicationRequest.MedicationRequestDispenseRequestComponent dispenseRequest =
                new MedicationRequest.MedicationRequestDispenseRequestComponent();

        // quantity
        Quantity quantity = new Quantity();
        quantity.setValue(7);
        quantity.setUnit(TABLET_UNIT);
        quantity.setCode("15AAAAAAAAAAA");
        dispenseRequest.setQuantity(quantity);
        medicationRequest.setDispenseRequest(dispenseRequest);

        // dosage instruction
        Dosage.DosageDoseAndRateComponent doseAndRate = new Dosage.DosageDoseAndRateComponent();
        Dosage dosage = new Dosage();
        dosage.setDoseAndRate(List.of(doseAndRate));
        dosage.setText("Take 2 pills every 20 minutes");

        Timing timing = new Timing();
        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();
        repeat.setFrequency(3);
        repeat.setDuration(10);
        repeat.setDurationUnit(Timing.UnitsOfTime.D);
        timing.setRepeat(repeat);
        dosage.setTiming(timing);
        medicationRequest.setDosageInstruction(List.of(dosage));

        // Act
        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(medicationRequest);

        // verify
        assertNotNull(saleOrderLine);
        assertEquals(7.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals("15AAAAAAAAAAA", saleOrderLine.getSaleOrderLineProductUom());
        assertEquals(
                "medication | 7 Tablet | 10 day - Take 2 pills every 20 minutes | Orderer: requester",
                saleOrderLine.getSaleOrderLineName());
    }

    @Test
    @DisplayName("Should throw exception for unsupported resource type")
    public void shouldThrowExceptionForUnsupportedResourceType() {
        Resource unsupportedResource = new Patient();
        assertThrows(IllegalArgumentException.class, () -> saleOrderLineMapper.toOdoo(unsupportedResource));
    }
}
