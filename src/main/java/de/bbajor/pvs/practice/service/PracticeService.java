package de.bbajor.pvs.practice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.practice.repository.PracticeRepository;

@Service
public class PracticeService {

    @Autowired
    private PracticeRepository practiceRepository;

    /**
     * Gets the practice data. Returns null if no practice is configured yet.
     */
    @Transactional(readOnly = true)
    public Practice getPractice() {
        return practiceRepository.findFirstByOrderByIdAsc().orElse(null);
    }

    /**
     * Saves or updates the practice data.
     * Since only one practice should exist, this either creates a new one
     * or updates the existing one.
     */
    @Transactional
    public Practice savePractice(Practice practice) {
        // Check if a practice already exists
        Optional<Practice> existingPractice = practiceRepository.findFirstByOrderByIdAsc();
        
        if (existingPractice.isPresent()) {
            // Update existing practice
            Practice practiceToUpdate = existingPractice.get();
            updatePracticeFields(practiceToUpdate, practice);
            return practiceRepository.save(practiceToUpdate);
        } else {
            // Create new practice
            return practiceRepository.save(practice);
        }
    }

    private void updatePracticeFields(Practice target, Practice source) {
        target.setPracticeName(source.getPracticeName());
        target.setStreet(source.getStreet());
        target.setHouseNumber(source.getHouseNumber());
        target.setPostalCode(source.getPostalCode());
        target.setCity(source.getCity());
        target.setCountry(source.getCountry());
        target.setOwnerName(source.getOwnerName());
        target.setOwnerTitle(source.getOwnerTitle());
        target.setLanr(source.getLanr());
        target.setBsnr(source.getBsnr());
        target.setPhone(source.getPhone());
        target.setFax(source.getFax());
        target.setEmail(source.getEmail());
        target.setAdditionalInfo(source.getAdditionalInfo());
    }

    /**
     * Deletes the practice data.
     */
    @Transactional
    public void deletePractice() {
        practiceRepository.findFirstByOrderByIdAsc().ifPresent(practiceRepository::delete);
    }
}


