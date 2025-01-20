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
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.UomHandler;
import com.ozonehis.eip.odoo.openmrs.model.Uom;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.EIPException;
import org.springframework.core.env.Environment;

class UomHandlerTest {
    @Mock
    private OdooClient odooClient;

    private OdooUtils odooUtils;

    @InjectMocks
    private UomHandler uomHandler;

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
        uomHandler.setOdooUtils(odooUtils);
    }

    @Test
    public void shouldReturnUomWhenOnlyOneUomExistsWithId() {
        // Setup
        String externalId = "198AAAAAAAAAAA";

        Map<String, Object> uomMap = getUomMap(1, "198AAAAAAAAAAA", 123, "Tablet");
        Object[] uoms = {uomMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", externalId)),
                        null))
                .thenReturn(uoms);

        // Act
        Uom result = uomHandler.getUom(externalId);

        // Verify
        assertNotNull(result);
        assertEquals(123, result.getUomResId());
        assertEquals("198AAAAAAAAAAA", result.getUomName());
        assertEquals(1, result.getUomId());
    }

    @Test
    public void shouldThrowErrorWhenMultipleUomExistsWithSameIds() {
        // Setup
        String externalId = "208AAAAAAAAAAA";

        Map<String, Object> uomMap1 = getUomMap(2, "208AAAAAAAAAAA", 323, "Units");
        Map<String, Object> uomMap2 = getUomMap(3, "208AAAAAAAAAAA", 523, "Tablet");
        Object[] uoms = {uomMap1, uomMap2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", externalId)),
                        null))
                .thenReturn(uoms);

        // Verify
        assertThrows(EIPException.class, () -> uomHandler.getUom(externalId));
    }

    @Test
    public void shouldThrowErrorWhenNoUomFoundWithId() {
        // Setup
        String externalId = "198AAAAAAAAAAA";

        Object[] uoms = {};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.IR_MODEL,
                        asList(asList("model", "=", Constants.UOM_MODEL), asList("name", "=", externalId)),
                        null))
                .thenReturn(uoms);

        // Verify
        assertThrows(EIPException.class, () -> uomHandler.getUom(externalId));
    }

    public Map<String, Object> getUomMap(int id, String name, int resId, String displayName) {
        Map<String, Object> uomMap = new HashMap<>();
        uomMap.put("id", id);
        uomMap.put("name", name);
        uomMap.put("res_id", resId);
        uomMap.put("display_name", displayName);
        return uomMap;
    }
}
