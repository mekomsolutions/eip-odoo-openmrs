/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmrs.eip.fhir.Constants.HEADER_FHIR_EVENT_TYPE;

import com.ozonehis.eip.odoo.openmrs.model.SaleOrder;
import com.ozonehis.eip.odoo.openmrs.routes.partner.CreatePartnerRoute;
import com.ozonehis.eip.odoo.openmrs.routes.partner.DeletePartnerRoute;
import com.ozonehis.eip.odoo.openmrs.routes.partner.UpdatePartnerRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorder.CreateSaleOrderRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorder.DeleteSaleOrderRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorder.UpdateSaleOrderRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorderline.CreateSaleOrderLineRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorderline.DeleteSaleOrderLineRoute;
import com.ozonehis.eip.odoo.openmrs.routes.saleorderline.UpdateSaleOrderLineRoute;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.test.infra.core.annotations.RouteFixture;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SupplyRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SupplyRequestToSaleOrderIntegrationTest extends BaseRouteIntegrationTest {

    private SupplyRequest supplyRequest;

    private static final String ENCOUNTER_PART_OF_UUID = "97552cbc-8e75-4c4b-b17f-3e746d0cfceb";

    private static final String ENCOUNTER_UUID = "4d6d21cc-a6a5-4714-9c44-631d9d4cb3fc";

    private static final String PATIENT_UUID = "3ee4f5fc-6299-4c0e-a56e-dad957118edc";

    @BeforeEach
    public void initializeData() {
        supplyRequest = loadResource("fhir.supplyrequest/supply-request.json", new SupplyRequest());
        // Deleting any existing sale order
        Object[] result =
                getOdooClient().search(Constants.SALE_ORDER_MODEL, asList("client_order_ref", "!=", "any uuid"));
        for (Object id : result) {
            getOdooClient().delete(Constants.SALE_ORDER_MODEL, Collections.singletonList((Integer) id));
        }

        // Mock OpenMRS FHIR metadata endpoint
        mockOpenmrsFhirServer();
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @RouteFixture
    public void createRouteBuilder(CamelContext context) throws Exception {
        context = getContextWithRouting(context);

        context.addRoutes(new CreatePartnerRoute());
        context.addRoutes(new UpdatePartnerRoute());
        context.addRoutes(new DeletePartnerRoute());
        context.addRoutes(new CreateSaleOrderRoute());
        context.addRoutes(new UpdateSaleOrderRoute());
        context.addRoutes(new DeleteSaleOrderRoute());
        context.addRoutes(new CreateSaleOrderLineRoute());
        context.addRoutes(new UpdateSaleOrderLineRoute());
        context.addRoutes(new DeleteSaleOrderLineRoute());
    }

    @Test
    @DisplayName("Should verify has sale order routes.")
    public void shouldVerifySaleOrderRoutes() {
        assertTrue(hasRoute(contextExtension.getContext(), "supplyrequest-to-sale-order-router"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-create-sale-order-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-update-sale-order-route"));
        assertTrue(hasRoute(contextExtension.getContext(), "odoo-delete-sale-order-route"));
    }

    @Test
    @DisplayName("Should create sale order with in Odoo given supply request")
    public void shouldCreateSaleOrderInOdooGivenServiceRequestBundle() {
        // Setup
        stubFor(get(urlMatching("/openmrs/ws/fhir2/R4/Observation\\?.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJSON("fhir.bundle/empty-bundle.json"))));

        stubFor(get(urlMatching("/openmrs/ws/fhir2/R4/Encounter/" + ENCOUNTER_UUID))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJSON("fhir.encounter/encounter.json"))));

        stubFor(get(urlMatching("/openmrs/ws/fhir2/R4/Patient/" + PATIENT_UUID))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(readJSON("fhir/patient/patient-3.json"))));

        // Act
        var headers = new HashMap<String, Object>();
        headers.put(HEADER_FHIR_EVENT_TYPE, "c");
        Bundle bundle = new Bundle();
        bundle.addEntry().setResource(supplyRequest);
        sendBodyAndHeaders("direct:supplyrequest-to-sale-order-processor", bundle, headers);

        // Verify sale order created
        Object[] result = getOdooClient()
                .searchAndRead(
                        Constants.SALE_ORDER_MODEL,
                        List.of(asList("client_order_ref", "=", ENCOUNTER_PART_OF_UUID), asList("state", "=", "draft")),
                        orderDefaultAttributes);

        assertNotNull(result);
        assertNotNull(result[0]);

        SaleOrder createdSaleOrder = getOdooUtils().convertToObject((Map<String, Object>) result[0], SaleOrder.class);

        assertNotNull(createdSaleOrder);
        assertEquals(ENCOUNTER_PART_OF_UUID, createdSaleOrder.getOrderClientOrderRef());
        assertEquals("draft", createdSaleOrder.getOrderState());
        // TODO: Add assert for Sale order line (Error: Product and Uom doesn't exist in Odoo)
    }
}
