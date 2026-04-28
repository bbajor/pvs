package de.bbajor.pvs.surgicalcenter.presenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class SurgicalCenterListPresenter {

    private static final Logger LOG = LogManager.getLogger();

    @Autowired
    private SurgicalCenterService surgicalCenterService;
    
    @Autowired
    private UserAccountRepository userAccountRepository;

    public List<SurgicalCenter> getAll() {
        return surgicalCenterService.findAll();
    }
    
    public org.springframework.data.domain.Slice<SurgicalCenter> getAll(org.springframework.data.domain.Pageable pageable) {
        return surgicalCenterService.findAll(pageable);
    }
    
    public org.springframework.data.domain.Slice<SurgicalCenter> findAllBy(String searchTerm, org.springframework.data.domain.Pageable pageable) {
        return surgicalCenterService.findAllBy(searchTerm, pageable);
    }

    public SurgicalCenter getById(Integer id) {
        // TODO only use one query here
        SurgicalCenter dto = surgicalCenterService.findByIdWithDetails(id);
        List<SurgicalCenterTimeSlot> timeSlotDtos = surgicalCenterService
                .getTimeSlotsBySurgicalCenterIdWithTreatmentCount(id);
        dto.setAvailableTimeSlots(timeSlotDtos);
        return dto;
    }

    public void save(SurgicalCenter surgicalCenterDto, List<TimeSlotConfig> timeSlotConfigList) {
        LOG.debug("Entering save-method for SurgicalCenter....");
        
        // Ensure InstitutionContext is set before service calls.
        // This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
        // so the context might not be set, especially for Institutionsadmins.
        ensureInstitutionContext();
        
        List<SurgicalCenterTimeSlot> newTimeSlots = new ArrayList<>();
        for (TimeSlotConfig config : timeSlotConfigList) {
            List<SurgicalCenterTimeSlot> timeSlotDtos = TimeSlotCreator.createTimeSlots(config);
            newTimeSlots.addAll(timeSlotDtos);
        }
        LOG.debug("Found " + newTimeSlots.size() + " new TimeSlots for SurgicalCenter....");

        if (surgicalCenterDto.getAvailableTimeSlots() != null) {
            Collection<SurgicalCenterTimeSlot> invalidSlots = TimeSlotCreator
                    .getNewInvalidTimeSlots(surgicalCenterDto.getAvailableTimeSlots(), newTimeSlots);
            newTimeSlots.removeAll(invalidSlots);
            LOG.debug("Found " + invalidSlots.size() + " invalid TimeSlots, that had to be removed before saving...");
        }
        LOG.debug("Saving SurgicalCenter with " + newTimeSlots.size() + " TimeSlots...");
        surgicalCenterService.saveTimeSlotsAndSurgicalCenter(newTimeSlots, surgicalCenterDto);
    }
    
    /**
     * Speichert einen SurgicalCenter mit direkt übergebenen TimeSlots.
     * Diese Methode wird verwendet, wenn die Slots bereits erstellt wurden.
     */
    public void saveWithTimeSlots(SurgicalCenter surgicalCenterDto, List<SurgicalCenterTimeSlot> newTimeSlots) {
        LOG.debug("Entering save-method for SurgicalCenter with pre-created TimeSlots....");
        
        // Ensure InstitutionContext is set before service calls.
        ensureInstitutionContext();
        
        LOG.debug("Found " + newTimeSlots.size() + " new TimeSlots for SurgicalCenter....");

        if (surgicalCenterDto.getAvailableTimeSlots() != null) {
            Collection<SurgicalCenterTimeSlot> invalidSlots = TimeSlotCreator
                    .getNewInvalidTimeSlots(surgicalCenterDto.getAvailableTimeSlots(), newTimeSlots);
            newTimeSlots.removeAll(invalidSlots);
            LOG.debug("Found " + invalidSlots.size() + " invalid TimeSlots, that had to be removed before saving...");
        }
        LOG.debug("Saving SurgicalCenter with " + newTimeSlots.size() + " TimeSlots...");
        surgicalCenterService.saveTimeSlotsAndSurgicalCenter(newTimeSlots, surgicalCenterDto);
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set, especially for Institutionsadmins.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                LOG.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    LOG.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, userAccount.getInstitution().getInstitutionCode());
                } else {
                    LOG.warn("UserAccount has no institution - cannot set InstitutionContext");
                }
            } catch (Exception e) {
                LOG.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
            }
        } else {
            LOG.debug("Authentication type: {}, Principal type: {} - cannot set InstitutionContext",
                    authentication != null ? authentication.getClass().getSimpleName() : "null",
                    authentication != null && authentication.getPrincipal() != null 
                        ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        }
    }

}
