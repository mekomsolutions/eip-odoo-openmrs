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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Partner implements OdooResource {

    @JsonProperty("id")
    private Integer partnerId;

    @JsonProperty("name")
    private String partnerName;

    @JsonProperty("ref")
    private String partnerRef;

    @JsonProperty("type")
    private String partnerType;

    @JsonProperty("street")
    private String partnerStreet;

    @JsonProperty("street2")
    private String partnerStreet2;

    @JsonProperty("city")
    private String partnerCity;

    @JsonProperty("zip")
    private String partnerZip;

    @JsonProperty("country_id")
    private Integer partnerCountryId;

    @JsonProperty("state_id")
    private Integer partnerStateId;

    @JsonProperty("active")
    private Boolean partnerActive;

    @JsonProperty("comment")
    private String partnerComment; // Stores Patient Identifier

    @JsonProperty("odoo.customer.dob.field")
    @JsonIgnore
    private String partnerBirthDate;
}
