package de.bbajor.pvs.patientsearch.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



class PatientSearchServiceTest {

    @Test
    void testFindPatientsThrowsUnsupportedOperationException() {
        PatientSearchService service = new PatientSearchService();
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> service.findPatients("anyFilter")
        );
        assertEquals("Unimplemented method 'findPatients'", exception.getMessage());
    }
}