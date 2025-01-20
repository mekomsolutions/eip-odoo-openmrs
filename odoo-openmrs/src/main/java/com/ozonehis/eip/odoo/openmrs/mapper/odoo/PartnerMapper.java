/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.mapper.odoo;

import com.ozonehis.eip.odoo.openmrs.client.OdooUtils;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CountryHandler;
import com.ozonehis.eip.odoo.openmrs.handlers.odoo.CountryStateHandler;
import com.ozonehis.eip.odoo.openmrs.mapper.ToOdooMapping;
import com.ozonehis.eip.odoo.openmrs.model.Partner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Setter;
import org.hl7.fhir.r4.model.Element;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Setter
@Component
public class PartnerMapper implements ToOdooMapping<Patient, Partner> {

    private static final String ADDRESS_EXTENSION_URL = "http://fhir.openmrs.org/ext/address";

    private static final String ADDRESS1_EXTENSION = "http://fhir.openmrs.org/ext/address#address1";

    private static final String ADDRESS2_EXTENSION = "http://fhir.openmrs.org/ext/address#address2";

    @Autowired
    private CountryHandler countryHandler;

    @Autowired
    private CountryStateHandler countryStateHandler;

    @Override
    public Partner toOdoo(Patient patient) {
        if (patient == null) {
            return null;
        }
        Partner partner = new Partner();
        partner.setPartnerRef(patient.getIdPart());
        partner.setPartnerActive(patient.getActive());
        String patientName = getPatientName(patient).orElse("");
        String patientIdentifier = getPreferredPatientIdentifier(patient).orElse("");
        partner.setPartnerComment(patientIdentifier);
        partner.setPartnerName(patientName);
        partner.setPartnerBirthDate(
                OdooUtils.convertEEEMMMddDateToOdooFormat(patient.getBirthDate().toString()));

        addAddress(patient, partner);
        return partner;
    }

    protected Optional<String> getPreferredPatientIdentifier(Patient patient) {
        return patient.getIdentifier().stream()
                .filter(identifier -> identifier.getUse() == Identifier.IdentifierUse.OFFICIAL)
                .findFirst()
                .map(Identifier::getValue);
    }

    protected Optional<String> getPatientName(Patient patient) {
        return patient.getName().stream()
                .findFirst()
                .map(name -> name.getGiven().get(0) + " " + name.getFamily());
    }

    protected void addAddress(Patient patient, Partner partner) {
        if (patient.hasAddress()) {
            patient.getAddress().forEach(fhirAddress -> {
                partner.setPartnerCity(fhirAddress.getCity());
                partner.setPartnerCountryId(countryHandler.getCountryId(fhirAddress.getCountry()));
                partner.setPartnerZip(fhirAddress.getPostalCode());
                partner.setPartnerStateId(countryStateHandler.getStateId(fhirAddress.getState()));
                if (fhirAddress.getType() != null) {
                    partner.setPartnerType(fhirAddress.getType().getDisplay());
                }

                if (fhirAddress.hasExtension()) {
                    List<Extension> extensions = fhirAddress.getExtension();
                    List<Extension> addressExtensions = extensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS_EXTENSION_URL))
                            .findFirst()
                            .map(Element::getExtension)
                            .orElse(new ArrayList<>());

                    addressExtensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS1_EXTENSION))
                            .findFirst()
                            .ifPresent(extension -> partner.setPartnerStreet(
                                    extension.getValue().toString()));

                    addressExtensions.stream()
                            .filter(extension -> extension.getUrl().equals(ADDRESS2_EXTENSION))
                            .findFirst()
                            .ifPresent(extension -> partner.setPartnerStreet2(
                                    extension.getValue().toString()));
                }
            });
        }
    }
}
