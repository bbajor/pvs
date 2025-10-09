package de.bbajor.pvs.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import de.bbajor.pvs.patient.model.Address;

public interface PatientAddressRepository extends JpaRepository<Address, Long>, JpaSpecificationExecutor<Address> {

}
