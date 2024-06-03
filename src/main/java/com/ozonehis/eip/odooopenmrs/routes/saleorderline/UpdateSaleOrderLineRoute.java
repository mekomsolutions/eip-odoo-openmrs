/*
 * Copyright © 2024, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odooopenmrs.routes.saleorderline;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class UpdateSaleOrderLineRoute extends RouteBuilder {

    @Override
    public void configure() {
        // spotless:off
        from("direct:odoo-update-sale-order-line-route")
                .routeId("odoo-update-sale-order-line-route")
                .to("odoo://write/sale.order.line")
                .log(LoggingLevel.INFO, "Sale order line updated.")
                .end();
        // spotless:on
    }
}
