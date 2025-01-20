/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@Component
public class ObservationHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public Observation getObservationBySubjectIDAndConceptID(String subjectID, String conceptID) {
        Bundle bundle = openmrsFhirClient
                .search()
                .forResource(Observation.class)
                .where(Observation.SUBJECT.hasId(subjectID))
                .and(Observation.CODE.exactly().code(conceptID))
                .sort()
                .descending(Observation.DATE)
                .returnBundle(Bundle.class)
                .execute();

        log.debug("ObservationHandler: Observation getObservationBySubjectID {}", bundle.getId());

        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Observation.class::isInstance)
                .map(Observation.class::cast)
                .findFirst()
                .orElse(null);
    }
}
