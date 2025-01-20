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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.ozonehis.eip.odoo.openmrs.Constants;
import com.ozonehis.eip.odoo.openmrs.client.OdooClient;
import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.ProductHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.SaleOrderLineHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.UomHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odoo.openmrs.model.Product;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import com.ozonehis.eip.odoo.openmrs.model.SaleOrderLine;
import com.ozonehis.eip.odoo.openmrs.model.Uom;
import java.util.HashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.EIPException;
import org.springframework.core.env.Environment;

class SaleOrderLineHandlerTest {

    @Mock
    private OdooClient odooClient;

    @Mock
    private ProductHandler productHandler;

    @Mock
    private UomHandler uomHandler;

    @Mock
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    @InjectMocks
    private SaleOrderLineHandler saleOrderLineHandler;

    private OdooUtils odooUtils;

    private static AutoCloseable mocksCloser;

    private static final int ORDER_ID = 14;

    private static final int PRODUCT_ID = 15;

    private static final String PRODUCT_UOM_ID = "16";

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
        saleOrderLineHandler.setOdooUtils(odooUtils);
    }

    @Test
    public void shouldReturnSaleOrderLineWhenOnlyOneSaleOrderLineExistsWithOrderIdAndProductId() {
        // Setup
        Map<String, Object> saleOrderLineMap =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Object[] saleOrderLines = {saleOrderLineMap};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Act
        SaleOrderLine result = saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID);

        // Verify
        assertNotNull(result);
        assertEquals(ORDER_ID, result.getSaleOrderLineOrderId());
        assertEquals("Aspirin 81 mg | 10 Tablet", result.getSaleOrderLineName());
        assertEquals(5.0f, result.getSaleOrderLineProductUomQty());
    }

    @Test
    public void shouldThrowErrorWhenMultipleSaleOrderLineExistsWithSameOrderIdAndProductId() {
        // Setup
        Map<String, Object> saleOrderLineMap1 =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Map<String, Object> saleOrderLineMap2 =
                getSaleOrderLineMap(2, "Aspirin 361 mg | 5 Tablet", ORDER_ID, PRODUCT_ID, 7.0f, PRODUCT_UOM_ID);

        Object[] saleOrderLines = {saleOrderLineMap1, saleOrderLineMap2};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Verify
        assertThrows(EIPException.class, () -> saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID));
    }

    @Test
    public void shouldReturnNullWhenNoSaleOrderLineExistsWithOrderIdAndProductId() {
        // Setup
        Object[] saleOrderLines = {};

        // Mock behavior
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);

        // Act
        SaleOrderLine result = saleOrderLineHandler.getSaleOrderLineIfExists(ORDER_ID, PRODUCT_ID);

        // Verify
        assertNull(result);
    }

    @Test
    public void shouldReturnNullWhenProductAndSaleOrderLineExistsAndResourceIsServiceRequest() {
        // Setup
        Resource resource = new ServiceRequest();
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();
        Map<String, Object> saleOrderLineMap =
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID);
        Object[] saleOrderLines = {saleOrderLineMap};

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(saleOrderLines);
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNull(result);
    }

    @Test
    public void shouldReturnSaleOrderLineWhenProductExistsAndResourceIsServiceRequest() {
        // Setup
        Resource resource = new ServiceRequest();
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(new Object[] {});
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNotNull(result);
        assertEquals(5.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(1, saleOrderLine.getSaleOrderLineProductUom());
    }

    @Test
    public void shouldReturnSaleOrderLineWhenProductExistsAndResourceIsMedicationRequest() {
        // Setup
        Resource resource = new MedicationRequest();
        Uom uom = new Uom();
        uom.setUomResId(999);
        SaleOrder saleOrder = new SaleOrder();
        saleOrder.setOrderId(ORDER_ID);
        Product product = new Product();
        product.setProductResId(PRODUCT_ID);
        SaleOrderLine saleOrderLine = getSaleOrderLine();

        // Mock behaviour
        when(productHandler.getProduct(resource)).thenReturn(product);
        when(odooClient.searchAndRead(
                        Constants.SALE_ORDER_LINE_MODEL,
                        asList(asList("order_id", "=", ORDER_ID), asList("product_id", "=", PRODUCT_ID)),
                        null))
                .thenReturn(new Object[] {});
        when(saleOrderLineMapper.toOdoo(resource)).thenReturn(saleOrderLine);
        when(uomHandler.getUom((String) saleOrderLine.getSaleOrderLineProductUom()))
                .thenReturn(uom);

        // Act
        SaleOrderLine result = saleOrderLineHandler.buildSaleOrderLineIfProductExists(resource, saleOrder);

        // Verify
        assertNotNull(result);
        assertEquals(5.0f, saleOrderLine.getSaleOrderLineProductUomQty());
        assertEquals(999, saleOrderLine.getSaleOrderLineProductUom());
    }

    public Map<String, Object> getSaleOrderLineMap(
            int id, String name, int orderId, int productId, float productUomQty, String productUom) {
        Map<String, Object> saleOrderMap = new HashMap<>();
        saleOrderMap.put("id", id);
        saleOrderMap.put("name", name);
        saleOrderMap.put("order_id", orderId);
        saleOrderMap.put("product_id", productId);
        saleOrderMap.put("product_uom_qty", productUomQty);
        saleOrderMap.put("product_uom", productUom);
        return saleOrderMap;
    }

    public SaleOrderLine getSaleOrderLine() {
        return odooUtils.convertToObject(
                getSaleOrderLineMap(1, "Aspirin 81 mg | 10 Tablet", ORDER_ID, PRODUCT_ID, 5.0f, PRODUCT_UOM_ID),
                SaleOrderLine.class);
    }
}
