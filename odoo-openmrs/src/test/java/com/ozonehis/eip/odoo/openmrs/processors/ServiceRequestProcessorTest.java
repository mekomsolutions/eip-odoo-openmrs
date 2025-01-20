/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.handlers.odoo.PartnerHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.ArrayList;
import java.util.List;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class ServiceRequestProcessorTest extends BaseProcessorTest {
    private static final String ENCOUNTER_REFERENCE_ID = "Encounter/1234";

    private static final String ENCOUNTER_VISIT_ID = "1234";

    private static final int PARTNER_ID = 12;

    private static final String PATIENT_ID = "patient-id-987";

    @Mock
    private SaleOrderHandler saleOrderHandler;

    @Mock
    private PartnerHandler partnerHandler;

    @InjectMocks
    private ServiceRequestProcessor serviceRequestProcessor;

    private static AutoCloseable mocksCloser;

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldUpdateSaleOrderWithSaleOrderLineWhenPartnerAndSaleOrderExists() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        SaleOrder saleOrder = new SaleOrder();

        Exchange exchange = createExchange(bundle, "u");

        Partner partner = new Partner();
        partner.setPartnerId(PARTNER_ID);

        // Mock behavior
        when(partnerHandler.createOrUpdatePartner(any(), eq(patient))).thenReturn(partner);
        when(saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_VISIT_ID))
                .thenReturn(saleOrder);

        // Act
        serviceRequestProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "u");
        verify(saleOrderHandler, times(1))
                .updateSaleOrderIfExistsWithSaleOrderLine(
                        eq(serviceRequest),
                        eq(saleOrder),
                        eq(ENCOUNTER_VISIT_ID),
                        eq(PARTNER_ID),
                        eq(PATIENT_ID),
                        any());
        verify(saleOrderHandler, times(0))
                .createSaleOrderWithSaleOrderLine(
                        eq(serviceRequest), eq(encounter), eq(partner), eq(ENCOUNTER_VISIT_ID), eq(PATIENT_ID), any());
    }

    @Test
    void shouldCreateSaleOrderAndSaleOrderLineWhenPartnerExists() {
        // Arrange
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        Encounter encounter = new Encounter();
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        ServiceRequest serviceRequest = new ServiceRequest();
        serviceRequest.setStatus(ServiceRequest.ServiceRequestStatus.ACTIVE);
        serviceRequest.setIntent(ServiceRequest.ServiceRequestIntent.ORDER);

        Bundle bundle = new Bundle();
        List<Bundle.BundleEntryComponent> entries = new ArrayList<>();
        entries.add(new Bundle.BundleEntryComponent().setResource(patient));
        entries.add(new Bundle.BundleEntryComponent().setResource(encounter));
        entries.add(new Bundle.BundleEntryComponent().setResource(serviceRequest));
        bundle.setEntry(entries);

        Exchange exchange = createExchange(bundle, "c");

        Partner partner = new Partner();
        partner.setPartnerId(PARTNER_ID);

        // Mock behavior
        when(partnerHandler.createOrUpdatePartner(any(), eq(patient))).thenReturn(partner);
        when(saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_VISIT_ID))
                .thenReturn(null);

        // Act
        serviceRequestProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        verify(saleOrderHandler, times(0))
                .updateSaleOrderIfExistsWithSaleOrderLine(
                        eq(serviceRequest), any(), eq(ENCOUNTER_VISIT_ID), eq(PARTNER_ID), eq(PATIENT_ID), any());
        verify(saleOrderHandler, times(1))
                .createSaleOrderWithSaleOrderLine(
                        eq(serviceRequest), eq(encounter), eq(partner), eq(ENCOUNTER_VISIT_ID), eq(PATIENT_ID), any());
    }
}
