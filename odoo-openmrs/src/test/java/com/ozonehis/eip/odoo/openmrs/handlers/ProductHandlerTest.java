/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;

class ProductHandlerTest {
    public static final String MEDICATION_REQUEST_ID = "1cdda1ce-7f98-4bc7-9d20-8b4e953d972a";

    public static final String MEDICATION_ID = "c95ab33a-8558-4705-8a7a-3b4e580270c7";

    private static final String SERVICE_REQUEST_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String SUPPLY_REQUEST_ID = "9gv050e1-s1be-5b4c-b107-c48d2db47r11";

    private static final String ENCOUNTER_ID = "4d6d21cc-a6a5-4714-9c44-631d9d4cb3fc";

    private static final String TABLET_UNIT = "Tablet";

    private static final String COMPLETE_BLOOD_COUNT_CODE = "446ac22c-24f2-40ad-98f4-65f026f434e9";

    private static final String ADHESIVE_CODE = "90165e66-615d-4219-ba2c-50ed46a51bff";

    private static final String QUANTITY_CODE = "162396AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";

    private static final String ADHESIVE_DISPLAY = "Adhesive 5cm x 9m";

    private static final String COMPLETE_BLOOD_COUNT_DISPLAY = "Complete Blood Count";

    private static final String COMPLETE_BLOOD_COUNT_ID = "3d597a15-b08f-4c35-8a42-0c15f9a19fa6";

    private static final String PRACTITIONER_ID = "e5ca6578-fb37-4900-a054-c68db82a551c";

    private static final String PATIENT_ID = "3ee4f5fc-6299-4c0e-a56e-dad957118edc";

    @Mock
    private OdooClient odooClient;

    private OdooUtils odooUtils;

    @InjectMocks
    private ProductHandler productHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
        Environment mockEnvironment = Mockito.mock(Environment.class);
        when(mockEnvironment.getProperty("odoo.customer.weight.field")).thenReturn("x_customer_weight");
        odooUtils = new OdooUtils();
        odooUtils.setEnvironment(mockEnvironment);
        productHandler.setOdooUtils(odooUtils);
    }

    @Test
    public void shouldReturnProductWhenOnlyOneProductExistsWithMedicationRequestId() {
        // Setup
        MedicationRequest medicationRequest = getMedicationRequest();

        Map<String, Object> productMap = getProductMap(1, "198AAAAAAAAAAA", 123, "Aspirin");
        Object[] products = {productMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", MEDICATION_ID)),
                        null))
                .thenReturn(products);

        // Act
        Product result = productHandler.getProduct(medicationRequest);

        // Verify
        assertNotNull(result);
        assertEquals(123, result.getProductResId());
        assertEquals("198AAAAAAAAAAA", result.getProductName());
        assertEquals(1, result.getProductId());
        assertEquals("Aspirin", result.getProductDisplayName());
    }

    @Test
    public void shouldReturnProductWhenOnlyOneIdFoundMatchingServiceRequestId() {
        // Setup
        ServiceRequest serviceRequest = getServiceRequest();

        Map<String, Object> productMap = getProductMap(1, "198AAAAAAAAAAA", 123, "Aspirin");
        Object[] products = {productMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(
                                asList("model", "=", Constants.PRODUCT_MODEL),
                                asList("name", "=", COMPLETE_BLOOD_COUNT_CODE)),
                        null))
                .thenReturn(products);

        // Act
        Product result = productHandler.getProduct(serviceRequest);

        // Verify
        assertNotNull(result);
        assertEquals(123, result.getProductResId());
        assertEquals("198AAAAAAAAAAA", result.getProductName());
        assertEquals(1, result.getProductId());
        assertEquals("Aspirin", result.getProductDisplayName());
    }

    @Test
    public void shouldReturnProductWhenOnlyOneIdFoundMatchingSupplyRequestId() {
        // Setup
        SupplyRequest supplyRequest = getSupplyRequest();

        Map<String, Object> productMap =
                getProductMap(1, "162396AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 123, "Adhesive 5cm x 9m");
        Object[] products = {productMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", ADHESIVE_CODE)),
                        null))
                .thenReturn(products);

        // Act
        Product result = productHandler.getProduct(supplyRequest);

        // Verify
        assertNotNull(result);
        assertEquals(123, result.getProductResId());
        assertEquals("162396AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", result.getProductName());
        assertEquals(1, result.getProductId());
        assertEquals("Adhesive 5cm x 9m", result.getProductDisplayName());
    }

    @Test
    public void shouldThrowErrorWhenResourceIsUnsupported() {
        // Setup
        Resource resource = new Patient();

        Map<String, Object> productMap = getProductMap(1, "198AAAAAAAAAAA", 123, "Aspirin");
        Object[] products = {productMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.PRODUCT_MODEL), asList("name", "=", SERVICE_REQUEST_ID)),
                        null))
                .thenReturn(products);
        // Verify
        assertThrows(IllegalArgumentException.class, () -> productHandler.getProduct(resource));
    }

    public Map<String, Object> getProductMap(int id, String name, int resId, String displayName) {
        Map<String, Object> uomMap = new HashMap<>();
        uomMap.put("id", id);
        uomMap.put("name", name);
        uomMap.put("res_id", resId);
        uomMap.put("display_name", displayName);
        return uomMap;
    }

    public MedicationRequest getMedicationRequest() {
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
        return medicationRequest;
    }

    public ServiceRequest getServiceRequest() {
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

        return serviceRequest;
    }

    public SupplyRequest getSupplyRequest() {
        SupplyRequest supplyRequest = new SupplyRequest();
        supplyRequest.setId(SUPPLY_REQUEST_ID);
        supplyRequest.setItem(
                new Reference().setReference("MedicalSupply/" + ADHESIVE_CODE).setDisplay(ADHESIVE_DISPLAY));
        supplyRequest.setReasonReference(Collections.singletonList(
                new Reference().setType("Encounter").setReference("Encounter/" + ENCOUNTER_ID)));
        supplyRequest.setQuantity(new Quantity().setValue(10).setCode(QUANTITY_CODE));
        supplyRequest.setRequester(new Reference().setReference(PRACTITIONER_ID).setDisplay("Nurse Jane"));
        supplyRequest.setDeliverTo(
                new Reference().setReference("Patient/" + PATIENT_ID).setDisplay("Tim"));
        supplyRequest.setStatus(SupplyRequest.SupplyRequestStatus.ACTIVE);

        return supplyRequest;
    }
}
