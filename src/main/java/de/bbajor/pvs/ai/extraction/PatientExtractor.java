package de.bbajor.pvs.ai.extraction;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ai.extraction.patterns.PatientPatterns;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.repository.HealthInsuranceRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PatientExtractor implements EntityExtractor<Patient> {

    private final HealthInsuranceRepository healthInsuranceRepository;

    @Override
    public ExtractionResult<Patient> extract(String text) {
        Patient patient = new Patient();
        Map<String, Double> fieldConfidences = new HashMap<>();
        double totalConfidence = 0.0;
        int matchedFields = 0;

        // Extract name
        Matcher nameMatcher = PatientPatterns.PATIENT_NAME.matcher(text);
        if (nameMatcher.find()) {
            patient.setFirstName(nameMatcher.group(1));
            patient.setLastName(nameMatcher.group(2));
            fieldConfidences.put("name", 0.9);
            totalConfidence += 0.9;
            matchedFields++;
        }

        // Extract birth date
        LocalDate birthDate = extractBirthDate(text);
        if (birthDate != null) {
            patient.setBirth(birthDate);
            fieldConfidences.put("birth", 0.85);
            totalConfidence += 0.85;
            matchedFields++;
        }

        // Extract address
        Address address = extractAddress(text);
        if (address != null) {
            patient.setAddress(address);
            fieldConfidences.put("address", 0.8);
            totalConfidence += 0.8;
            matchedFields++;
        }

        // Extract insurance
        HealthInsurance insurance = extractInsurance(text);
        if (insurance != null) {
            patient.setHealthInsurance(insurance);
            fieldConfidences.put("insurance", 0.75);
            totalConfidence += 0.75;
            matchedFields++;
        }

        // Extract insurance number
        Matcher insuranceNumberMatcher = PatientPatterns.INSURANCE_NUMBER.matcher(text);
        if (insuranceNumberMatcher.find()) {
            patient.setInsuranceNumber(insuranceNumberMatcher.group(1));
            fieldConfidences.put("insuranceNumber", 0.9);
            totalConfidence += 0.9;
            matchedFields++;
        }

        // Calculate average confidence
        double avgConfidence = matchedFields > 0 ? totalConfidence / matchedFields : 0.0;

        return new ExtractionResult<>(patient, avgConfidence, fieldConfidences, text);
    }

    private LocalDate extractBirthDate(String text) {
        // Try DD.MM.YYYY format first
        Matcher matcher = PatientPatterns.BIRTH_DATE_DD_MM_YYYY.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                int month = Integer.parseInt(matcher.group(2));
                int year = Integer.parseInt(matcher.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Invalid date, try next format
            }
        }

        // Try DD. Month YYYY format
        matcher = PatientPatterns.BIRTH_DATE_DD_MMM_YYYY.matcher(text);
        if (matcher.find()) {
            try {
                int day = Integer.parseInt(matcher.group(1));
                String monthStr = matcher.group(2);
                int month = PatientPatterns.monthToNumber(monthStr);
                int year = Integer.parseInt(matcher.group(3));
                if (month > 0) {
                    return LocalDate.of(year, month, day);
                }
            } catch (Exception e) {
                // Invalid date
            }
        }

        return null;
    }

    private Address extractAddress(String text) {
        Address address = new Address();

        // Extract street and house number
        Matcher streetMatcher = PatientPatterns.ADDRESS_STREET_HOUSE.matcher(text);
        if (streetMatcher.find()) {
            address.setStreet(streetMatcher.group(1).trim());
            address.setHouseNo(streetMatcher.group(2));
        }

        // Extract postal code and city
        Matcher postalMatcher = PatientPatterns.ADDRESS_POSTAL_CITY.matcher(text);
        if (postalMatcher.find()) {
            address.setPostalCode(Integer.parseInt(postalMatcher.group(1)));
            address.setCity(postalMatcher.group(2).trim());
        }

        // Extract country
        if (PatientPatterns.ADDRESS_COUNTRY.matcher(text).find()) {
            address.setCountry(Locale.GERMANY);
        }

        // Return address only if we found at least street or city
        if (address.getStreet() != null || address.getCity() != null) {
            return address;
        }

        return null;
    }

    private HealthInsurance extractInsurance(String text) {
        Matcher insuranceMatcher = PatientPatterns.INSURANCE_NAME.matcher(text);
        if (insuranceMatcher.find()) {
            String insuranceName = insuranceMatcher.group(1).trim();

            // Try to find matching insurance in database
            List<HealthInsurance> allInsurances = healthInsuranceRepository.findAll();
            for (HealthInsurance insurance : allInsurances) {
                String costCarrierName = insurance.getCostCarrierName();
                String billingCarrierName = insurance.getBillingCarrierName();
                if (costCarrierName != null && costCarrierName.toLowerCase().contains(insuranceName.toLowerCase())) {
                    return insurance;
                }
                if (billingCarrierName != null
                        && billingCarrierName.toLowerCase().contains(insuranceName.toLowerCase())) {
                    return insurance;
                }
            }

            // Create new insurance if not found
            HealthInsurance newInsurance = new HealthInsurance();
            newInsurance.setCostCarrierName(insuranceName);
            return newInsurance;
        }

        return null;
    }

    @Override
    public Class<Patient> getEntityType() {
        return Patient.class;
    }

    @Override
    public String getEntityName() {
        return "Patient";
    }

}

