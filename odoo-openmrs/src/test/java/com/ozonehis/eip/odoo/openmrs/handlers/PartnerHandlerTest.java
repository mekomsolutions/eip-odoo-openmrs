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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.ProducerTemplate;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.EIPException;
import org.springframework.core.env.Environment;

class PartnerHandlerTest {

    private static final String PARTNER_REF_ID = "4ed050e1-c1be-4b4c-b407-c48d2db49b87";

    private static final String PATIENT_ID = "5td050e1-c1be-4b4c-b407-c48d2db49b65";

    private static final String PARTNER_IDENTIFIER_ID = "12d050e1-c1be-4b4c-b407-c48d2db49b78";

    @Mock
    private OdooClient odooClient;

    @Mock
    private PartnerMapper partnerMapper;

    @InjectMocks
    private PartnerHandler partnerHandler;

    private OdooUtils odooUtils;

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
        partnerHandler.setOdooUtils(odooUtils);
    }

    @Test
    public void shouldReturnPartnerWhenOnlyOneGetPartnerByID() {
        // Setup
        Map<String, Object> partner = getPartnerMap();

        Object[] partners = {partner};

        // Mock behavior
        when(odooClient.searchAndRead(
                        eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", PARTNER_REF_ID))), any()))
                .thenReturn(partners);

        // Act
        Partner fetchedPartner = partnerHandler.getPartnerByID(PARTNER_REF_ID);

        // Verify
        assertNotNull(fetchedPartner);
        assertEquals("John Doe", fetchedPartner.getPartnerName());
        assertEquals(PARTNER_REF_ID, fetchedPartner.getPartnerRef());
        assertEquals(PARTNER_IDENTIFIER_ID, fetchedPartner.getPartnerComment());
        assertEquals(12, fetchedPartner.getPartnerId());
    }

    @Test
    public void shouldThrowErrorWhenMultiplePartnersWithSameIdExists() {
        // Setup
        Map<String, Object> partner1 = getPartnerMap();
        Map<String, Object> partner2 = getPartnerMap();

        Object[] partners = {partner1, partner2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", PARTNER_REF_ID))), any()))
                .thenReturn(partners);

        // Verify
        assertThrows(EIPException.class, () -> partnerHandler.getPartnerByID(PARTNER_REF_ID));
    }

    @Test
    public void shouldReturnPartnerIdAndUpdatePartnerWhenGetPartnerByID() {
        // Setup
        Patient patient = new Patient();
        patient.setId(PARTNER_REF_ID);

        Map<String, Object> headers = new HashMap<>();
        headers.put(
                Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE, List.of(getPartner().getPartnerId()));

        Map<String, Object> partner = getPartnerMap();
        Object[] partners = {partner};

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(odooClient.searchAndRead(
                        eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", patient.getIdPart()))), any()))
                .thenReturn(partners);
        when(partnerMapper.toOdoo(patient)).thenReturn(getPartner());

        // Act
        Partner result = partnerHandler.createOrUpdatePartner(producerTemplate, patient);

        // Verify
        assertEquals(12, result.getPartnerId());
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders(eq("direct:odoo-update-partner-route"), eq(getPartner()), eq(headers));
    }

    @Test
    public void shouldReturnPartnerIdAndCreatePartnerWhenPartnerDoesNotExists() {
        // Setup
        Patient patient = new Patient();
        patient.setId(PARTNER_REF_ID);

        Map<String, Object> headers = new HashMap<>();

        // Mock behavior
        ProducerTemplate producerTemplate = Mockito.mock(ProducerTemplate.class);
        when(odooClient.searchAndRead(
                        eq(Constants.PARTNER_MODEL), eq(List.of(asList("ref", "=", patient.getIdPart()))), any()))
                .thenReturn(new Object[] {})
                .thenReturn(new Object[] {getPartnerMap()});
        when(partnerMapper.toOdoo(patient)).thenReturn(getPartner());

        // Act
        Partner result = partnerHandler.createOrUpdatePartner(producerTemplate, patient);

        // Verify
        assertEquals(12, result.getPartnerId());
        verify(producerTemplate, times(1))
                .sendBodyAndHeaders(eq("direct:odoo-create-partner-route"), eq(getPartner()), eq(headers));
    }

    private Map<String, Object> getPartnerMap() {
        Map<String, Object> partnerMap = new HashMap<>();
        partnerMap.put("id", 12);
        partnerMap.put("name", "John Doe");
        partnerMap.put("ref", PARTNER_REF_ID);
        partnerMap.put("city", "Berlin");
        partnerMap.put("active", true);
        partnerMap.put("comment", PARTNER_IDENTIFIER_ID);
        return partnerMap;
    }

    private Partner getPartner() {
        Partner partner = new Partner();
        partner.setPartnerId(12);
        partner.setPartnerName("John Doe");
        partner.setPartnerRef(PARTNER_REF_ID);
        partner.setPartnerCity("Berlin");
        partner.setPartnerActive(true);
        partner.setPartnerComment(PARTNER_IDENTIFIER_ID);
        return partner;
    }
}
