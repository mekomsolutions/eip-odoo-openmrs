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

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderHandler;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import java.util.Date;
import org.apache.camel.Exchange;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

class EncounterProcessorTest extends BaseProcessorTest {
    @Mock
    private SaleOrderHandler saleOrderHandler;

    @InjectMocks
    private EncounterProcessor encounterProcessor;

    private static AutoCloseable mocksCloser;

    private static final String ENCOUNTER_REFERENCE_ID = "Encounter/1234";

    private static final String ENCOUNTER_ID = "abc-1234";

    @BeforeEach
    void setup() {
        mocksCloser = openMocks(this);
    }

    @AfterAll
    static void close() throws Exception {
        mocksCloser.close();
    }

    @Test
    void shouldProcessEncounterWhenSaleOrderExists() {
        // Arrange
        Encounter encounter = new Encounter();
        Period period = new Period();
        period.setEnd(new Date());
        encounter.setPeriod(period);
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        encounter.setId(ENCOUNTER_ID);

        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(123);

        Exchange exchange = createExchange(encounter, "c");

        // Mock behavior
        when(saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID)).thenReturn(saleOrder);

        // Act
        encounterProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "u");
        assertEquals(exchange.getProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER), false);
        verify(saleOrderHandler, times(1)).getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID);
    }

    @Test
    void shouldSkipEncounterWhenSaleOrderDoesNotExists() {
        // Arrange
        Encounter encounter = new Encounter();
        Period period = new Period();
        period.setEnd(new Date());
        encounter.setPeriod(period);
        encounter.setPartOf(new Reference(ENCOUNTER_REFERENCE_ID));
        encounter.setId(ENCOUNTER_ID);

        Exchange exchange = createExchange(encounter, "c");

        // Mock behavior
        when(saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID)).thenReturn(null);

        // Act
        encounterProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        assertEquals(exchange.getProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER), true);
        verify(saleOrderHandler, times(1)).getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID);
    }

    @Test
    void shouldSkipEncounterWhenEncounterIsNull() {
        // Arrange
        Encounter encounter = null;

        Exchange exchange = createExchange(encounter, "c");

        // Mock behavior
        when(saleOrderHandler.getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID)).thenReturn(null);

        // Act
        encounterProcessor.process(exchange);

        // Assert
        assertEquals(exchange.getMessage().getHeader(HEADER_FHIR_EVENT_TYPE), "c");
        assertEquals(exchange.getProperty(Constants.EXCHANGE_PROPERTY_SKIP_ENCOUNTER), true);
        verify(saleOrderHandler, times(0)).getDraftSaleOrderIfExistsByVisitId(ENCOUNTER_ID);
    }
}
