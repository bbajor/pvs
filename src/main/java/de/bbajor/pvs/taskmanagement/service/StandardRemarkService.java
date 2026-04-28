package de.bbajor.pvs.taskmanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.InstitutionAccessValidator;
import de.bbajor.pvs.taskmanagement.domain.StandardRemark;
import de.bbajor.pvs.taskmanagement.domain.StandardRemarkRepository;

/**
 * Service für Standardbemerkungen.
 */
@Service
public class StandardRemarkService {

    @Autowired
    private StandardRemarkRepository standardRemarkRepository;
    
    @Autowired
    private InstitutionAccessValidator institutionAccessValidator;

    @Transactional(readOnly = true)
    public List<StandardRemark> findAllForCurrentInstitution() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return List.of();
        }
        return standardRemarkRepository.findByInstitutionIdOrderBySortOrderAscTextAsc(institutionId);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public StandardRemark save(StandardRemark remark) {
        Objects.requireNonNull(remark);
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set");
        }
        
        if (remark.getInstitution() == null || !remark.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Standard remark must belong to current institution");
        }
        
        institutionAccessValidator.validateInstitutionAccess(institutionId, "StandardRemark", remark.getId());
        
        return standardRemarkRepository.save(remark);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Objects.requireNonNull(id);
        
        StandardRemark remark = standardRemarkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Standard remark not found: " + id));
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set");
        }
        
        institutionAccessValidator.validateInstitutionAccess(institutionId, "StandardRemark", id);
        
        standardRemarkRepository.delete(remark);
    }
}

