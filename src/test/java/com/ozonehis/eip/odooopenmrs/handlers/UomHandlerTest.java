package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.model.Uom;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.xmlrpc.XmlRpcException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.openmrs.eip.EIPException;

class UomHandlerTest {
    @Mock
    private OdooClient odooClient;

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
    }

    @Test
    public void shouldReturnUomWhenOnlyOneIdFound() throws MalformedURLException, XmlRpcException {
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
    public void shouldThrowErrorWhenMultipleIdsAreFoundWithSameExternalId()
            throws MalformedURLException, XmlRpcException {
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
    public void shouldThrowErrorWhenNoUomFoundWithExternalId() throws MalformedURLException, XmlRpcException {
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
