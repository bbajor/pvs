package de.bbajor.pvs.ivom.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ivom.model.IvomPlan;
import de.bbajor.pvs.ivom.repository.IvomRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class IvomService {

    private IvomRepository ivomRepository;

    public IvomService(IvomRepository ivomRepository) {
        this.ivomRepository = ivomRepository;
    }

    public Optional<IvomPlan> findById(Long id) {
        return ivomRepository.findById(id);
    }

    public Collection<IvomPlan> findByPatient(Integer patientId) {
        // TODO implement
        return Collections.emptyList();
    }

    public List<IvomPlan> findIvoms(String filter) {
        Specification<IvomPlan> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), likeFilter)));
                    // TODO add more fields like name of health insurance, ivom type etc.

            try {
                Integer birthFilter = Integer.parseInt(filter);
                predicates.add(cb.equal(root.get("birth"), birthFilter));
            } catch (NumberFormatException ignored) {
            }

            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return ivomRepository.findAll(spec);
    }

    public void save(IvomPlan newEntity) {
        // TODO validate and check relating entities
        ivomRepository.save(newEntity);
    }

    public Collection<IvomPlan> generateDailyList(LocalDate date) {
        // TODO implement
        return Collections.emptyList();
    }

}
