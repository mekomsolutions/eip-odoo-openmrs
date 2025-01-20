/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.PartnerMapper;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.Collections;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class PatientProcessorTest extends BaseProcessorTest {

    @Mock
    private PartnerMapper partnerMapper;

    @Mock
    private PartnerHandler partnerHandler;

    @InjectMocks
    private PatientProcessor patientProcessor;

    private static AutoCloseable mocksCloser;

    private static final String ADDRESS_ID = "12377e18-a051-487b-8dd3-4cffcddb2a9c";

    private static final String PATIENT_ID = "866f25bf-d930-4886-9332-75443047e38e";

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldProcessPatientWithCreateEventType() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setAddress(Collections.singletonList(address));

        Partner partner = new Partner();
        partner.setPartnerRef(PATIENT_ID);

        Exchange exchange = createExchange(patient, "c");

        // Mock behavior
        when(partnerMapper.toOdoo(patient)).thenReturn(partner);
        when(partnerHandler.getPartnerByID(partner.getPartnerRef())).thenReturn(null);

        // Act
        patientProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(partnerMapper, times(1)).toOdoo(patient);
    }

    @Test
    void shouldProcessPatientWithUpdateEventType() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setAddress(Collections.singletonList(address));

        Partner partner = new Partner();
        partner.setPartnerRef(PATIENT_ID);

        Partner fetchedPartner = new Partner();
        fetchedPartner.setPartnerId(12);

        Exchange exchange = createExchange(patient, "u");

        // Mock behavior
        when(partnerMapper.toOdoo(patient)).thenReturn(partner);
        when(partnerHandler.getPartnerByID(partner.getPartnerRef())).thenReturn(fetchedPartner);

        // Act
        patientProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "u");
        verify(partnerMapper, times(1)).toOdoo(patient);
    }

    @Test
    void shouldProcessPatientWithDeleteEventType() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Address address = new Address();
        address.setId(ADDRESS_ID);
        address.setUse(Address.AddressUse.HOME);
        patient.setAddress(Collections.singletonList(address));

        Partner partner = new Partner();
        partner.setPartnerRef(PATIENT_ID);

        Partner fetchedPartner = new Partner();
        fetchedPartner.setPartnerId(12);

        Exchange exchange = createExchange(patient, "d");

        // Mock behavior
        when(partnerMapper.toOdoo(patient)).thenReturn(partner);
        when(partnerHandler.getPartnerByID(partner.getPartnerRef())).thenReturn(fetchedPartner);

        // Act
        patientProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "d");
        verify(partnerMapper, times(1)).toOdoo(patient);
    }
}
