/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.routes.partner.CreatePartnerRoute;
import com.ozonehis.eip.odoo.openmrs.routes.partner.DeletePartnerRoute;
import com.ozonehis.eip.odoo.openmrs.routes.partner.UpdatePartnerRoute;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.annotations.RouteFixture;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PatientToPartnerIntegrationTest extends BaseRouteIntegrationTest {

    private Patient patient1;

    private static final String PATIENT_1_UUID = "df7182cb-eb6e-4160-9f70-2efb0b6d5d74";

    private static final String ADDRESS_1_UUID = "50c6b3fd-65aa-4628-aa56-6b02287f77ca";

    private static final String PATIENT_IDENTIFIER_1_VALUE = "10000GX";

    private Patient patient2;

    private static final String PATIENT_2_UUID = "d238321f-40ba-4dea-b307-3fa95336bc9f";

    private static final String ADDRESS_2_UUID = "c09315e5-a8d0-4325-98ea-cd6730cb9944";

    private static final String PATIENT_IDENTIFIER_2_VALUE = "100008E";

    @RouteFixture
    public void createRouteBuilder(CamelContext context) throws Exception {
        context = getContextWithRouting(context);

        context.addRoutes(new CreatePartnerRoute());
        context.addRoutes(new UpdatePartnerRoute());
        context.addRoutes(new DeletePartnerRoute());
    }

    @BeforeEach
    public void initializeData() {
        patient1 = loadResource("fhir/patient/patient-1.json", new Patient());
        patient2 = loadResource("fhir/patient/patient-2.json", new Patient());
    }

    @Test
    @DisplayName("should verify partner routes.")
    public void verifyHasPartnerRoutes() {
        assertTrue(hasRoute(contextExtension.getContext(), "patient-to-partner-router"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-create-partner-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-update-partner-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-delete-partner-route"));
    }

    @Test
    @DisplayName("should create partner in Odoo given FHIR patient.")
    public void shouldCreatePartnerInOdooGivenFhirPatient() {
        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:patient-to-partner-router", patient1, headers);

        // verify
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL, List.of(asList("ref", "=", PATIENT_1_UUID)), partnerDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        Partner createdPartner = getOdooUtils().convertToObject((Map<String, Object>) result[0], Partner.class);

        assertNotNull(createdPartner);
        assertEquals("Richard Jones", createdPartner.getPartnerName());
        assertEquals(PATIENT_1_UUID, createdPartner.getPartnerRef());
        assertEquals("City2062", createdPartner.getPartnerCity());
        assertEquals(PATIENT_IDENTIFIER_1_VALUE, createdPartner.getPartnerComment());
        //        assertEquals("1939-02-14", createdPartner.getPartnerBirthDate());
    }

    @Test
    @DisplayName("should update partner in Odoo given updated FHIR patient.")
    public void shouldUpdatePartnerInOdooGivenFhirPatient() {
        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:patient-to-partner-router", patient2, headers);

        // verify
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL, List.of(asList("ref", "=", PATIENT_2_UUID)), partnerDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        Partner createdPartner = getOdooUtils().convertToObject((Map<String, Object>) result[0], Partner.class);

        assertNotNull(createdPartner);
        assertEquals("Joshua Johnson", createdPartner.getPartnerName());
        assertEquals(PATIENT_2_UUID, createdPartner.getPartnerRef());
        assertEquals("City6442", createdPartner.getPartnerCity());
        assertEquals(PATIENT_IDENTIFIER_2_VALUE, createdPartner.getPartnerComment());

        // Update patient
        patient2 = loadResource("fhir/patient/patient-2-updated.json", new Patient());
        headers.put(HEADER_FHIR_EVENT_TYPE, "u");
        sendBodyAndHeaders("direct:patient-to-partner-router", patient2, headers);

        // verify
        result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL, List.of(asList("ref", "=", PATIENT_2_UUID)), partnerDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        Partner updatedPartner = getOdooUtils().convertToObject((Map<String, Object>) result[0], Partner.class);

        assertNotNull(updatedPartner);
        assertEquals("Test James", updatedPartner.getPartnerName());
        assertEquals(PATIENT_2_UUID, updatedPartner.getPartnerRef());
        assertEquals("Nairobi", updatedPartner.getPartnerCity());
        assertEquals(PATIENT_IDENTIFIER_2_VALUE, updatedPartner.getPartnerComment());
        //        assertEquals("2019-09-25", updatedPartner.getPartnerBirthDate());
    }

    @Test
    @DisplayName("should delete Partner in Odoo given deleted FHIR patient.")
    public void shouldDeletePartnerInOdooGivenDeletedFhirPatient() {
        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        sendBodyAndHeaders("direct:patient-to-partner-router", patient1, headers);

        // verify
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL, List.of(asList("ref", "=", PATIENT_1_UUID)), partnerDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        Partner createdPartner = getOdooUtils().convertToObject((Map<String, Object>) result[0], Partner.class);

        assertNotNull(createdPartner);
        assertEquals("Richard Jones", createdPartner.getPartnerName());
        assertEquals(PATIENT_1_UUID, createdPartner.getPartnerRef());
        assertEquals("City2062", createdPartner.getPartnerCity());
        assertEquals(PATIENT_IDENTIFIER_1_VALUE, createdPartner.getPartnerComment());

        // Delete patient
        headers.put(HEADER_FHIR_EVENT_TYPE, "d");
        sendBodyAndHeaders("direct:patient-to-partner-router", patient1, headers);

        // verify
        result = getOdooClient()
                .searchAndRead(
                        Constants.PARTNER_MODEL, List.of(asList("ref", "=", PATIENT_1_UUID)), partnerDefaultAttributes);

        assertNotNull(result);
        assertEquals(0, result.length);
    }
}
