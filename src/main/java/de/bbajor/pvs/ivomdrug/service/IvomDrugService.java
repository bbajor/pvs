package de.bbajor.pvs.ivomdrug.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.ivom.model.IvomDrug;
import de.bbajor.pvs.ivomdrug.repository.IvomDrugRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class IvomDrugService {

    private IvomDrugRepository ivomDrugRepository;

    public IvomDrugService(IvomDrugRepository ivomDrugRepository) {
        this.ivomDrugRepository = ivomDrugRepository;
    }

    public Optional<IvomDrug> findById(Long id) {
        return ivomDrugRepository.findById(id);
    }

    public List<IvomDrug> findIvomDrugs(String filter) {
        Specification<IvomDrug> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return ivomDrugRepository.findAll(spec);
    }

    public List<IvomDrug> findAll() {
        return ivomDrugRepository.findAll();
    }

    public void save(IvomDrug newEntity) {
        ivomDrugRepository.save(newEntity);
    }

}
