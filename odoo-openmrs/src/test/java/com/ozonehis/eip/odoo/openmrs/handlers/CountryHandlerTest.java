/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CountryHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class CountryHandlerTest {
    @Mock
    private OdooClient odooClient;

    @InjectMocks
    private CountryHandler countryHandler;

    private static AutoCloseable mocksCloser;

    @AfterAll
    public static void close() throws Exception {
        mocksCloser.close();
    }

    @BeforeEach
    public void setup() {
        mocksCloser = openMocks(this);
    }

    @Test
    public void shouldReturnCountryIdWhenOnlyOneCountryExistsWithId() {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {10};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryHandler.getCountryId(countryName);

        // Verify
        assertNotNull(id);
        assertEquals(10, id);
    }

    @Test
    public void shouldThrowErrorWhenMultipleCountriesExistsWithSameId() {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {10, 11};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Verify
        assertThrows(EIPException.class, () -> countryHandler.getCountryId(countryName));
    }

    @Test
    public void shouldReturnNullWhenNoCountryFoundWithId() {
        // Setup
        String countryName = "India";
        Integer[] countryIds = {};

        // Mock behavior
        when(odooClient.search(Constants.COUNTRY_MODEL, asList("name", "=", countryName)))
                .thenReturn(countryIds);

        // Act
        Integer id = countryHandler.getCountryId(countryName);

        // Verify
        assertNull(id);
    }
}
