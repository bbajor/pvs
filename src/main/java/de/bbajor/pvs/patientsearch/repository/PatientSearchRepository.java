package de.bbajor.pvs.patientsearch.repository;

import org.springframework.data.repository.CrudRepository;

import de.bbajor.pvs.base.domain.Patient;

public interface PatientSearchRepository extends CrudRepository<Patient, Long> {

}
