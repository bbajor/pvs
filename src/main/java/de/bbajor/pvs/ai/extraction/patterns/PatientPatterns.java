package de.bbajor.pvs.ai.extraction.patterns;

import java.util.regex.Pattern;

import lombok.Getter;

public class PatientPatterns {
    
    // Name patterns: "Patient Hans Müller" or "Patient Hans, Müller"
    public static final Pattern PATIENT_NAME = Pattern.compile(
            "(?:Patient|Pateint)?\\s*([A-ZÄÖÜ][a-zäöüß]+)\\s+(?:und\\s+)?([A-ZÄÖÜ][a-zäöüß]+(?:\\-[A-Z][a-zäöüß]+)?)",
            Pattern.CASE_INSENSITIVE);
    
    // Birth date patterns
    public static final Pattern BIRTH_DATE_DD_MM_YYYY = Pattern.compile(
            "(?:geboren|geb\\.?|geburtstag)\\s*(?:am\\s+)?(\\d{1,2})\\.(\\d{1,2})\\.(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    
    public static final Pattern BIRTH_DATE_DD_MMM_YYYY = Pattern.compile(
            "(?:geboren|geb\\.?)\\s*(?:am\\s+)?(\\d{1,2})\\.\\s*(Januar|Februar|März|April|Mai|Juni|Juli|August|September|Oktober|November|Dezember|Jan|Feb|Mär|Apr|Jun|Jul|Aug|Sep|Okt|Nov|Dez)\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    
    // Address patterns
    public static final Pattern ADDRESS_STREET_HOUSE = Pattern.compile(
            "(?:anschrift|adresse|wohnt\\s+in|wohnt|straße)\\s*([A-ZÄÖÜa-zäöüß\\s]+straße|Straße|Gasse|Weg|Platz|Allee|Park)\\s+(\\d+[a-z]?)",
            Pattern.CASE_INSENSITIVE);
    
    public static final Pattern ADDRESS_POSTAL_CITY = Pattern.compile(
            "(?:in\\s+)?(\\d{5})\\s+([A-ZÄÖÜ][a-zäöüß\\s\\-]+?)(?:,|$)",
            Pattern.CASE_INSENSITIVE);
    
    public static final Pattern ADDRESS_COUNTRY = Pattern.compile(
            "(?:Deutschland|Deutsch|DE|BRD)",
            Pattern.CASE_INSENSITIVE);
    
    // Insurance patterns
    public static final Pattern INSURANCE_NAME = Pattern.compile(
            "(?:versicherung|krankenkasse|kk|versichert\\s+bei)\\s+([A-ZÄÖÜ][a-zäöüß\\s\\-]+?)(?:,|versichertennummer|vers-nr|$)",
            Pattern.CASE_INSENSITIVE);
    
    public static final Pattern INSURANCE_NUMBER = Pattern.compile(
            "(?:versichertennummer|vers-nr|versnr|versicherungsnummer)\\s*([A-Z]?\\d{10,11})",
            Pattern.CASE_INSENSITIVE);
    
    @Getter
    private static final String[] MONTHS_GERMAN = {
            "januar", "februar", "märz", "april", "mai", "juni",
            "juli", "august", "september", "oktober", "november", "dezember"
    };
    
    @Getter
    private static final String[] MONTHS_SHORT = {
            "jan", "feb", "mär", "apr", "mai", "jun",
            "jul", "aug", "sep", "okt", "nov", "dez"
    };
    
    public static int monthToNumber(String month) {
        month = month.toLowerCase().trim();
        for (int i = 0; i < MONTHS_GERMAN.length; i++) {
            if (MONTHS_GERMAN[i].equals(month) || MONTHS_SHORT[i].equals(month)) {
                return i + 1;
            }
        }
        return -1;
    }
    
}

