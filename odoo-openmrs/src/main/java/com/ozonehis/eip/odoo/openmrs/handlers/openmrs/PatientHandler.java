/*
 * Copyright © 2021, Ozone HIS <info@ozone-his.com>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.ozonehis.eip.odoo.openmrs.handlers.openmrs;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class PatientHandler {

    @Autowired
    private IGenericClient openmrsFhirClient;

    public Patient getPatientByPatientID(String patientID) {
        Patient patient = openmrsFhirClient
                .read()
                .resource(Patient.class)
                .withId(patientID)
                .execute();

        log.info("PatientHandler: Patient getPatientByPatientID {}", patient.getId());
        return patient;
    }
}
