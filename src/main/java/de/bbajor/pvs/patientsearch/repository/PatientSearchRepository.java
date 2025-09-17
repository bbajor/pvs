package de.bbajor.pvs.patientsearch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.base.domain.Patient;

public interface PatientSearchRepository extends JpaRepository<Patient, Integer>, JpaSpecificationExecutor<Patient> {

}
