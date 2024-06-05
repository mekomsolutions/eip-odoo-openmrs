/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.handlers;

import static java.util.Arrays.asList;

import com.ozonehis.eip.odooopenmrs.Constants;
import com.ozonehis.eip.odooopenmrs.client.OdooClient;
import com.ozonehis.eip.odooopenmrs.client.OdooUtils;
import com.ozonehis.eip.odooopenmrs.mapper.odoo.SaleOrderLineMapper;
import com.ozonehis.eip.odooopenmrs.model.Product;
import com.ozonehis.eip.odooopenmrs.model.SaleOrder;
import com.ozonehis.eip.odooopenmrs.model.SaleOrderLine;
import com.ozonehis.eip.odooopenmrs.model.Uom;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.xmlrpc.XmlRpcException;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.openmrs.eip.EIPException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class SaleOrderLineHandler {

    @Autowired
    private OdooClient odooClient;

    @Autowired
    private ProductHandler productHandler;

    @Autowired
    private UomHandler uomHandler;

    @Autowired
    private SaleOrderLineMapper<Resource> saleOrderLineMapper;

    public SaleOrderLine buildSaleOrderLineIfProductExists(Resource resource, SaleOrder saleOrder) {
        Product product = productHandler.getProduct(resource);
        log.debug("SaleOrderLineHandler: Fetched Product {}", product);
        if (product == null) { // TODO: Check should we allow product not found
            log.debug("SaleOrderLineHandler: No product found");
            return null;
        }

        // Check if Sale order line order exists in Sale order
        SaleOrderLine fetchedSaleOrderLine =
                getSaleOrderLineIfExists(saleOrder.getOrderId(), product.getProductResId());
        if (fetchedSaleOrderLine != null) {
            log.debug(
                    "SaleOrderLineHandler: Sale order line already exists for sale order {} Skipping create new sale order line",
                    saleOrder);
            return null;
        }

        SaleOrderLine saleOrderLine = saleOrderLineMapper.toOdoo(resource);
        saleOrderLine.setSaleOrderLineProductId(product.getProductResId());
        saleOrderLine.setSaleOrderLineOrderId(saleOrder.getOrderId());
        if (resource instanceof MedicationRequest) {
            String uomExternalId = (String) saleOrderLine.getSaleOrderLineProductUom();
            Uom uom = uomHandler.getUom(uomExternalId);
            log.debug("SaleOrderLineHandler: Fetched Uom {}", uom);
            // Store Uom res_id in productUom to display unit in Odoo
            saleOrderLine.setSaleOrderLineProductUom(uom.getUomResId());
        } else if (resource instanceof ServiceRequest) {
            // Hardcoded to 1 so that `Units` is shown for ServiceRequest
            saleOrderLine.setSaleOrderLineProductUom(1);
        }

        return saleOrderLine;
    }

    public SaleOrderLine getSaleOrderLineIfExists(int saleOrderId, int productId) {
        try {
            Object[] records = odooClient.searchAndRead(
                    Constants.SALE_ORDER_LINE_MODEL,
                    asList(asList("order_id", "=", saleOrderId), asList("product_id", "=", productId)),
                    null);
            if (records == null) {
                throw new EIPException(String.format(
                        "Got null response while fetching for Sale order line with sale order id %s product id %s",
                        saleOrderId, productId));
            } else if (records.length == 1) {
                SaleOrderLine saleOrderLine =
                        OdooUtils.convertToObject((Map<String, Object>) records[0], SaleOrderLine.class);
                log.debug(
                        "Sale order line exists with sale order id {} product id {} sale order line {}",
                        saleOrderId,
                        productId,
                        saleOrderLine);
                return saleOrderLine;
            } else if (records.length == 0) {
                log.warn("No Sale order line found with sale order id {} product id {}", saleOrderId, productId);
                return null;
            } else {
                log.warn("Multiple Sale order line found with sale order id {} product id {}", saleOrderId, productId);
                throw new EIPException(String.format(
                        "Multiple Sale order line found with sale order id %s product id %s", saleOrderId, productId));
            }
        } catch (XmlRpcException | MalformedURLException e) {
            log.error(
                    "Error occurred while fetching sale order line with sale order id {} product id {} error {}",
                    saleOrderId,
                    productId,
                    e.getMessage(),
                    e);
            throw new CamelExecutionException("Error occurred while fetching sale order line", null, e);
        }
    }

    public void sendSaleOrderLine(ProducerTemplate producerTemplate, String endpointUri, SaleOrderLine saleOrderLine) {
        Map<String, Object> saleOrderLineHeaders = new HashMap<>();
        if (endpointUri.contains("update") || endpointUri.contains("delete")) {
            saleOrderLineHeaders.put(
                    com.ozonehis.eip.odooopenmrs.Constants.HEADER_ODOO_ID_ATTRIBUTE_VALUE,
                    List.of(saleOrderLine.getSaleOrderLineId()));
        }
        producerTemplate.sendBodyAndHeaders(endpointUri, saleOrderLine, saleOrderLineHeaders);
    }
}
