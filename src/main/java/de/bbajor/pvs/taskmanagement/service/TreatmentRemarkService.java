package de.bbajor.pvs.taskmanagement.service;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.InstitutionAccessValidator;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.taskmanagement.domain.StandardRemark;
import de.bbajor.pvs.taskmanagement.domain.TreatmentRemark;
import de.bbajor.pvs.taskmanagement.domain.TreatmentRemarkRepository;

/**
 * Service für Behandlungsbemerkungen.
 */
@Service
public class TreatmentRemarkService {

    @Autowired
    private TreatmentRemarkRepository treatmentRemarkRepository;
    
    @Autowired
    private TreatmentRepository treatmentRepository;
    
    @Autowired
    private InstitutionAccessValidator institutionAccessValidator;
    
    @Autowired
    private de.bbajor.pvs.taskmanagement.domain.StandardRemarkRepository standardRemarkRepository;

    @Transactional(readOnly = true)
    public List<TreatmentRemark> findByTreatmentId(Long treatmentId) {
        Objects.requireNonNull(treatmentId);
        return treatmentRemarkRepository.findByTreatmentIdOrderBySortOrderAscTextAsc(treatmentId);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER', 'ADMIN')")
    public TreatmentRemark addStandardRemark(Long treatmentId, Long standardRemarkId) {
        Objects.requireNonNull(treatmentId);
        Objects.requireNonNull(standardRemarkId);
        
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Prüfe, ob Behandlung bereits dokumentiert ist
        if (treatment.getApprovalDate() != null) {
            throw new IllegalStateException("Cannot modify remarks for documented treatment");
        }
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set");
        }
        
        institutionAccessValidator.validateInstitutionAccess(institutionId, "Treatment", treatmentId);
        
        // Prüfe, ob Bemerkung bereits vorhanden ist
        boolean alreadyExists = treatment.getRemarks().stream()
                .anyMatch(tr -> tr.getStandardRemark() != null && tr.getStandardRemark().getId().equals(standardRemarkId));
        
        if (alreadyExists) {
            throw new IllegalStateException("Standard remark already added to treatment");
        }
        
        // Lade Standardbemerkung
        StandardRemark standardRemark = standardRemarkRepository.findById(standardRemarkId)
                .orElseThrow(() -> new IllegalArgumentException("Standard remark not found: " + standardRemarkId));
        
        TreatmentRemark remark = new TreatmentRemark();
        remark.setTreatment(treatment);
        remark.setStandardRemark(standardRemark);
        // Text wird aus Standardbemerkung übernommen
        remark.setText(standardRemark.getText());
        
        treatment.getRemarks().add(remark);
        treatmentRepository.save(treatment);
        
        return remark;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER', 'ADMIN')")
    public TreatmentRemark addCustomRemark(Long treatmentId, String text) {
        Objects.requireNonNull(treatmentId);
        Objects.requireNonNull(text);
        
        if (text.trim().isEmpty()) {
            throw new IllegalArgumentException("Remark text cannot be empty");
        }
        
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Prüfe, ob Behandlung bereits dokumentiert ist
        boolean isDocumented = treatment.getApprovalDate() != null;
        boolean isSecondApproved = treatment.getSecondApprovalDateTime() != null;
        
        // Wenn bereits zweitgeprüft, können keine Bemerkungen mehr hinzugefügt werden
        if (isSecondApproved) {
            throw new IllegalStateException("Behandlung wurde bereits zweitgeprüft. Keine Änderungen mehr möglich.");
        }
        
        // Wenn dokumentiert, aber nicht zweitgeprüft: Nur Zweitprüfer können Bemerkungen hinzufügen
        if (isDocumented && !isSecondApproved) {
            // Prüfe, ob der aktuelle Benutzer ein Zweitprüfer ist (nicht der erste Prüfer)
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth == null) {
                throw new org.springframework.security.access.AccessDeniedException("Keine Authentifizierung gefunden.");
            }
            
            String currentUserId = auth.getName();
            if (treatment.getApprovedByUserId() != null && treatment.getApprovedByUserId().equals(currentUserId)) {
                throw new IllegalStateException("Nach der ersten Dokumentation können nur Zweitprüfer Bemerkungen hinzufügen.");
            }
            
            // Prüfe, ob Text mit Präfix beginnt
            String prefix = "Bemerkung Zweitprüfer: ";
            if (!text.trim().startsWith(prefix)) {
                throw new IllegalArgumentException("Bemerkungen von Zweitprüfern müssen mit '" + prefix + "' beginnen.");
            }
        } else if (!isDocumented) {
            // Wenn noch nicht dokumentiert, können normale Bemerkungen hinzugefügt werden
            // (keine Prüfung auf Präfix)
        }
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set");
        }
        
        institutionAccessValidator.validateInstitutionAccess(institutionId, "Treatment", treatmentId);
        
        TreatmentRemark remark = new TreatmentRemark();
        remark.setTreatment(treatment);
        remark.setText(text);
        
        // Persistiere zuerst die Bemerkung direkt, um detached entity Probleme zu vermeiden
        remark = treatmentRemarkRepository.save(remark);
        
        // Füge die Bemerkung zur Treatment-Liste hinzu und speichere das Treatment
        treatment.getRemarks().add(remark);
        treatmentRepository.save(treatment);
        
        // Lade die Bemerkung neu, um sicherzustellen, dass alle Beziehungen korrekt geladen sind
        final Long remarkId = remark.getId();
        TreatmentRemark refreshedRemark = treatmentRemarkRepository.findById(remarkId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment remark not found after save: " + remarkId));
        
        return refreshedRemark;
    }

    @Transactional
    @PreAuthorize("hasAnyRole('MEDICAL_STAFF', 'DOCTOR', 'OWNER', 'ADMIN')")
    public void removeRemark(Long treatmentId, Long remarkId) {
        Objects.requireNonNull(treatmentId);
        Objects.requireNonNull(remarkId);
        
        Treatment treatment = treatmentRepository.findById(treatmentId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment not found: " + treatmentId));
        
        // Prüfe, ob Behandlung bereits dokumentiert ist
        if (treatment.getApprovalDate() != null) {
            throw new IllegalStateException("Cannot modify remarks for documented treatment");
        }
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("No institution context set");
        }
        
        institutionAccessValidator.validateInstitutionAccess(institutionId, "Treatment", treatmentId);
        
        TreatmentRemark remark = treatmentRemarkRepository.findById(remarkId)
                .orElseThrow(() -> new IllegalArgumentException("Treatment remark not found: " + remarkId));
        
        if (!remark.getTreatment().getId().equals(treatmentId)) {
            throw new IllegalArgumentException("Remark does not belong to treatment");
        }
        
        treatment.getRemarks().remove(remark);
        treatmentRemarkRepository.delete(remark);
        treatmentRepository.save(treatment);
    }
}

