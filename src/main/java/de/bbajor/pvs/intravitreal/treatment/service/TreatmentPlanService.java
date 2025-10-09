package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentPlanRepository;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import de.bbajor.pvs.medication.service.MedicationMapper;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterMapper;
import jakarta.persistence.criteria.Predicate;

@Service
public class TreatmentPlanService {

    @Autowired
    private IvomDiagnosisService diagnosisService;
    @Autowired
    private PatientService patientService;

    @Autowired
    private SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository;
    @Autowired
    private TreatmentPlanRepository treatmentPlanRepository;
    @Autowired
    private TreatmentRepository treatmentRepository;
    @Autowired
    private MedicationRepository medicationRepository;

    @Autowired
    private TreatmentPlanMapper treatmentPlanMapper;
    @Autowired
    private TreatmentMapper treatmentMapper;
    @Autowired
    private SurgicalCenterMapper surgicalCenterMapper;
    @Autowired
    private MedicationMapper medicationMapper;

    @Transactional(readOnly = true)
    private TreatmentPlan findByIdWithDetails(Long id) {
        // Fetch the treatment plan with patient and diagnosis in a single query
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findTreatmentPlanByIdWithPatientDiagnosis(id)
                .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + id));

        // Fetch treatments separately to avoid lazy loading issues
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(id);

        // Set treatments without modifying the collection directly
        treatmentPlan.setTreatments(new ArrayList<>(treatments));

        return treatmentPlan;
    }

    private List<TreatmentPlan> findAll() {
        return treatmentPlanRepository.findAll();
    }

    public List<TreatmentPlanDto> getTreatmentPlans() {
        return treatmentPlanMapper.toTreatmentPlanDtoList(findAll());
    }

    @Transactional
    private Collection<TreatmentPlan> findByPatient(Integer patientId) {
        return treatmentPlanRepository.findByPatientId(patientId);
    }

    public List<TreatmentPlanDto> getTreatmentPlans(String filter) {
        List<TreatmentPlan> treatmentPlans = findTreatmentPlans(filter);
        return treatmentPlanMapper.toTreatmentPlanDtoList(treatmentPlans);
    }

    private List<TreatmentPlan> findTreatmentPlans(String filter) {
        Specification<TreatmentPlan> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("description")), likeFilter),
                    cb.like(cb.lower(root.get("additionalInformation")), likeFilter),
                    cb.like(cb.lower(root.get("patient").get("firstName")), likeFilter),
                    cb.like(cb.lower(root.get("patient").get("lastName")), likeFilter)));
            // TODO add more fields like name of health insurance, ivom type etc.
            try {
                Integer birthFilter = Integer.parseInt(filter);
                predicates.add(cb.equal(root.get("birth"), birthFilter));
            } catch (NumberFormatException ignored) {
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
        return treatmentPlanRepository.findAll(spec);
    }

    @Transactional(readOnly = true)
    public List<TreatmentDto> generateWeeklyList(LocalDate startDate) {

        List<TreatmentDto> resultList = new ArrayList<>();
        LocalDate monday = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate endOfWeek = monday.plusDays(6);

        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByDateRangeWithSurgicalCenterAndTreatmentPlan(monday, endOfWeek);
        for (Treatment treatment : treatments) {

            SurgicalCenterTimeSlot timeSlot = treatment.getSurgicalCenterTimeSlot();
            SurgicalCenter surgicalCenter = timeSlot.getSurgicalCenter();

            TreatmentDto treatmentDto = treatmentMapper.toTreatmentDto(treatment);
            MedicationDto medicationDto = medicationMapper.toMedicationDto(treatment.getMedication());
            TreatmentPlanDto treatmentPlanDto = treatmentPlanMapper.toTreatmentPlanDto(treatment.getTreatmentPlan());
            SurgicalCenterTimeSlotDto timeSlotDto = surgicalCenterMapper.toDto(timeSlot);
            SurgicalCenterDto surgicalCenterDto = surgicalCenterMapper.toDto(surgicalCenter);
            timeSlotDto.setSurgicalCenter(surgicalCenterDto);
            treatmentDto.setSurgicalCenterTimeSlot(timeSlotDto);
            treatmentDto.setTreatmentPlan(treatmentPlanDto);
            treatmentDto.setMedication(medicationDto);
            resultList.add(treatmentDto);
        }

        return resultList;
    }

    @Transactional
    private List<Treatment> getTreatmentSlots(Long treatmentPlanId) {
        List<Treatment> treatments = treatmentRepository
                .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlanId);
        return treatments;
    }

    public List<TreatmentDto> getTreatmentSlotsByTreatmentPlanId(Long treatmentPlanId) {
        List<Treatment> treatments = getTreatmentSlots(treatmentPlanId);
        return toTreatmentDtoList(treatments);
    }

    public TreatmentPlanDto loadTreatmentPlanWithFullDetails(Long id) {
        TreatmentPlan treatmentPlan = findByIdWithDetails(id);
        TreatmentPlanDto treatmentPlanDto = treatmentPlanMapper.toTreatmentPlanDto(treatmentPlan);
        List<TreatmentDto> treatments = toTreatmentDtoList(treatmentPlan.getTreatments());
        treatmentPlanDto.setTreatments(treatments);
        return treatmentPlanDto;
    }

    @Transactional
    public TreatmentPlanDto saveTreatmentPlan(TreatmentPlanDto update) throws NoSuchElementException {
        // 1. save treatmentplan without treatments
        TreatmentPlan current;
        if (update.getId() != null) {
            // Load the current treatment plan with all its treatments
            current = treatmentPlanRepository.findById(update.getId())
                    .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found: " + update.getId()));

            // Clear existing treatments from the plan to avoid orphans
            // First, detach existing treatments from the plan
            if (current.getTreatments() != null) {
                List<Treatment> existingTreatments = new ArrayList<>(current.getTreatments());
                for (Treatment existingTreatment : existingTreatments) {
                    // Don't remove from database yet, just detach from the plan
                    existingTreatment.setTreatmentPlan(null);
                }
                current.getTreatments().clear();
                // Explicitly save to ensure the detachment is persisted
                treatmentRepository.saveAll(existingTreatments);
            }
        } else {
            current = new TreatmentPlan();
        }

        if (update.getDiagnosis() != null) {
            if (update.getDiagnosis().getId() == null || update.getDiagnosis().getId() <= 0) {
                update.getDiagnosis().setId(null);
                // New diagnosis, save it first
                update.setDiagnosis(diagnosisService.save(update.getDiagnosis()));
                current.setDiagnosis(diagnosisService.getByDiagnoseId(update.getDiagnosis().getId()));
            } else {
                // Existing diagnosis, ensure it exists
                current.setDiagnosis(diagnosisService.getByDiagnoseId(update.getDiagnosis().getId()));
            }
        }

        if (update.getPatient() != null) {
            if (update.getPatient().getId() == null || update.getPatient().getId() <= 0) {
                throw new IllegalArgumentException("Patient ID is required");
            } else {
                Patient patient = patientService.findEntityById(update.getPatient().getId());
                current.setPatient(patient);
            }
        } else {
            throw new IllegalArgumentException("Patient information is required");
        }

        // Update the treatment plan from the DTO
        treatmentPlanMapper.updateEntityFromDto(update, current);

        // Save the treatment plan first
        TreatmentPlan savedTreatmentPlan = treatmentPlanRepository.save(current);

        // 2. Create and save new treatments linked to the treatment plan
        List<Treatment> treatmentEntityList = new ArrayList<>();
        for (TreatmentDto treatmentDto : update.getTreatments()) {
            Treatment treatmentToSave = new Treatment();
            treatmentMapper.updateTreatmentEntityFromDto(treatmentDto, treatmentToSave);
            SurgicalCenterTimeSlot surgicalCenterTimeSlot = surgicalCenterTimeSlotRepository
                    .getReferenceById(treatmentDto.getSurgicalCenterTimeSlot().getId());
            Medication medication = medicationRepository.getReferenceById(treatmentDto.getMedication().getId());
            treatmentToSave.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot);
            treatmentToSave.setMedication(medication);
            treatmentToSave.setTreatmentPlan(savedTreatmentPlan);
            treatmentEntityList.add(treatmentToSave);
        }

        // Save all new treatments
        treatmentRepository.saveAll(treatmentEntityList);

        // Refresh the treatment plan to get the updated state with treatments
        TreatmentPlanDto savedTreatmentPlanDto = getTreatmentPlanByIdWithFullDetails(savedTreatmentPlan.getId());
        return savedTreatmentPlanDto;
    }

    public TreatmentPlanDto getTreatmentPlanByIdWithFullDetails(Long id) throws NoSuchElementException {
        TreatmentPlan result = findByIdWithDetails(id);
        List<TreatmentDto> treatmentDtos = toTreatmentDtoList(result.getTreatments());
        TreatmentPlanDto treatmentPlanDto = treatmentPlanMapper.toTreatmentPlanDto(result);
        treatmentPlanDto.setTreatments(treatmentDtos);
        return treatmentPlanDto;
    }

    @Transactional
    public List<TreatmentDto> saveNewTreatmentsForExistingPlan(List<TreatmentDto> treatmentsToCreate,
            Long treatmentPlanId) {

        // Get the treatment plan by ID, ensuring it exists
        TreatmentPlan treatmentPlan = treatmentPlanRepository.findById(treatmentPlanId)
                .orElseThrow(() -> new NoSuchElementException("TreatmentPlan not found with id: " + treatmentPlanId));

        // Create new treatments linked to the treatment plan
        List<Treatment> treatments = new ArrayList<>();
        for (TreatmentDto treatmentDto : treatmentsToCreate) {
            // Get references to related entities
            SurgicalCenterTimeSlot timeSlot = surgicalCenterTimeSlotRepository
                    .getReferenceById(treatmentDto.getSurgicalCenterTimeSlot().getId());
            Medication medication = medicationRepository.getReferenceById(treatmentDto.getMedication().getId());

            // Create and configure the treatment
            Treatment treatment = new Treatment();
            treatmentMapper.updateTreatmentEntityFromDto(treatmentDto, treatment);
            treatment.setSurgicalCenterTimeSlot(timeSlot);
            treatment.setMedication(medication);
            treatment.setTreatmentPlan(treatmentPlan);
            treatments.add(treatment);
        }

        // Save all treatments in a single batch operation
        List<Treatment> saved = treatmentRepository.saveAll(treatments);

        // Update the treatments collection in the treatment plan
        if (treatmentPlan.getTreatments() == null) {
            treatmentPlan.setTreatments(new ArrayList<>(saved));
        } else {
            treatmentPlan.getTreatments().addAll(saved);
        }
        treatmentPlanRepository.save(treatmentPlan);

        // Convert and return the saved treatments
        return toTreatmentDtoList(saved);

    }

    private List<TreatmentDto> toTreatmentDtoList(List<Treatment> treatments) {
        List<TreatmentDto> resultList = new ArrayList<>();
        for (Treatment treatment : treatments) {
            TreatmentDto treatmentDto = treatmentMapper.toTreatmentDto(treatment);
            treatmentDto.setMedication(medicationMapper.toMedicationDto(treatment.getMedication()));
            treatmentDto.setTreatmentPlan(treatmentPlanMapper.toTreatmentPlanDto(treatment.getTreatmentPlan()));
            treatmentDto.setSurgicalCenterTimeSlot(surgicalCenterMapper.toDto(treatment.getSurgicalCenterTimeSlot()));
            resultList.add(treatmentDto);
        }
        return resultList;
    }
}
