/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaleOrder implements OdooResource {

    @JsonProperty("id")
    private Integer orderId;

    @JsonProperty("client_order_ref")
    private String orderClientOrderRef;

    @JsonProperty("state")
    private String orderState;

    @JsonProperty("partner_id")
    private Object orderPartnerId; // Can be used as a list or Integer

    @JsonProperty("order_line")
    private List<Integer> orderLine;

    @JsonProperty("type_name")
    private String orderTypeName;

    @JsonProperty("odoo.customer.weight.field")
    @JsonIgnore
    private String partnerWeight;

    @JsonProperty("odoo.customer.dob.field")
    @JsonIgnore
    private String partnerBirthDate;
}
