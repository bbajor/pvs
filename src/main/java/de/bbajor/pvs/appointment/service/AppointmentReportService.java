package de.bbajor.pvs.appointment.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentStatus;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;

/**
 * Service für die Generierung von Patienten-Ausdrucken für geplante Termine.
 */
@Service
public class AppointmentReportService {

    @Autowired
    private LocationService locationService;
    
    @Autowired
    private AppointmentService appointmentService;
    
    /**
     * Generiert einen PDF-Ausdruck mit allen zukünftigen Terminen für einen Patienten.
     * 
     * @param patient Der Patient, für den der Ausdruck erstellt wird
     * @return PDF als Byte-Array
     */
    public byte[] generatePatientAppointmentReport(Patient patient) {
        // Hole alle zukünftigen Termine für den Patienten
        List<Appointment> futureAppointments = appointmentService.findFutureAppointmentsByPatient(patient);
        
        // Sortiere nach Datum
        futureAppointments = futureAppointments.stream()
            .sorted((a1, a2) -> a1.getStartTime().compareTo(a2.getStartTime()))
            .collect(Collectors.toList());
        
        return generatePdfReportForPatient(patient, futureAppointments);
    }
    
    /**
     * Generiert einen PDF-Ausdruck mit allen zukünftigen Treatments für einen Patienten.
     * 
     * @param treatments Liste der Treatments
     * @param patient Der Patient, für den der Ausdruck erstellt wird
     * @return PDF als Byte-Array
     */
    public byte[] generatePatientTreatmentReport(List<de.bbajor.pvs.intravitreal.treatment.model.Treatment> treatments, Patient patient) {
        // Konvertiere Treatments zu Appointments-ähnlichen Objekten für den Report
        // Da der Report-Service Appointments erwartet, müssen wir die Treatments konvertieren
        // oder eine alternative Implementierung verwenden
        // Für jetzt verwenden wir die Treatments direkt und generieren ein ähnliches PDF
        return generatePdfReportForTreatments(patient, treatments);
    }
    
    private byte[] generatePdfReportForTreatments(Patient patient, List<Treatment> treatments) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Lade Standort-Daten
            Location location = locationService.getDefaultLocation();
            
            // Erstelle erste Seite und füge Wasserzeichen hinzu
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Füge Wasserzeichen hinzu (Institution aus Location)
            Institution institution = location != null ? location.getInstitution() : null;
            addWatermark(document, page, institution);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            int yPosition = 780;
            int lineHeight = 14;
            int margin = 50;
            int rightMargin = 545;
            
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            // Titel
            String titleText = "Terminübersicht";
            contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
            contentStream.setFont(titleFont, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(titleText);
            contentStream.endText();
            contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
            yPosition -= 30;
            
            // Praxis-Daten
            if (location != null) {
                contentStream.setFont(headerFont, 12);
                yPosition = addTextLine(contentStream, "Praxis", margin, yPosition, lineHeight);
                contentStream.setFont(normalFont, 10);
                
                if (location.getLocationName() != null && !location.getLocationName().isBlank()) {
                    yPosition = addTextLine(contentStream, location.getLocationName(), margin + 10, yPosition, lineHeight);
                }
                String fullAddress = location.getFullAddress();
                if (fullAddress != null && !fullAddress.isBlank()) {
                    yPosition = addTextLine(contentStream, fullAddress, margin + 10, yPosition, lineHeight);
                }
                if (location.getPhone() != null && !location.getPhone().isBlank()) {
                    yPosition = addTextLine(contentStream, "Tel: " + location.getPhone(), margin + 10, yPosition, lineHeight);
                }
                if (location.getEmail() != null && !location.getEmail().isBlank()) {
                    yPosition = addTextLine(contentStream, "E-Mail: " + location.getEmail(), margin + 10, yPosition, lineHeight);
                }
                
                yPosition -= 10;
                
                // Trennlinie
                contentStream.setStrokingColor(200f/255f, 200f/255f, 200f/255f);
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                contentStream.setStrokingColor(0, 0, 0);
                yPosition -= 20;
            }
            
            // Patient-Informationen
            contentStream.setFont(headerFont, 12);
            yPosition = addTextLine(contentStream, "Patient", margin, yPosition, lineHeight);
            contentStream.setFont(normalFont, 10);
            
            String patientName = patient.getLastName() + ", " + patient.getFirstName();
            yPosition = addTextLine(contentStream, patientName, margin + 10, yPosition, lineHeight);
            
            if (patient.getAddress() != null) {
                String address = patient.getAddress().toString();
                if (address != null && !address.isBlank()) {
                    yPosition = addTextLine(contentStream, address, margin + 10, yPosition, lineHeight);
                }
            }
            
            yPosition -= 10;
            
            // Trennlinie
            contentStream.setStrokingColor(200f/255f, 200f/255f, 200f/255f);
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            contentStream.setStrokingColor(0, 0, 0);
            yPosition -= 20;
            
            // Termine - kompakt und übersichtlich
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            if (treatments.isEmpty()) {
                contentStream.setFont(normalFont, 10);
                yPosition = addTextLine(contentStream, "Keine geplanten Termine", margin, yPosition, lineHeight);
            } else {
                // Sortiere alle Termine chronologisch
                List<Treatment> sortedTreatments = treatments.stream()
                    .filter(t -> t.getDate() != null)
                    .sorted((a1, a2) -> a1.getDate().compareTo(a2.getDate()))
                    .collect(Collectors.toList());
                
                // Trenne nächsten Termin vom Rest (falls vorhanden)
                LocalDate now = LocalDate.now();
                Treatment nextTreatment = sortedTreatments.stream()
                    .filter(t -> t.getDate().isAfter(now))
                    .findFirst()
                    .orElse(null);
                
                // Alle Termine außer dem nächsten (falls vorhanden)
                List<Treatment> otherTreatments = sortedTreatments.stream()
                    .filter(t -> nextTreatment == null || !t.equals(nextTreatment))
                    .collect(Collectors.toList());
                
                // Zeige zuerst alle anderen Termine
                for (Treatment treatment : otherTreatments) {
                    // Prüfe ob neue Seite benötigt wird
                    if (yPosition < 150) {
                        contentStream.close();
                        addPageNumber(document, document.getNumberOfPages());
                        
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        addWatermark(document, page, institution);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 780;
                    }
                    
                    // Termin-Datum und Uhrzeit (hervorgehoben)
                    LocalDate treatmentDate = treatment.getDate();
                    LocalTime startTime = treatment.getSurgicalCenterTimeSlot() != null 
                        ? treatment.getSurgicalCenterTimeSlot().getStartTime() : null;
                    LocalTime endTime = treatment.getSurgicalCenterTimeSlot() != null 
                        ? treatment.getSurgicalCenterTimeSlot().getEndTime() : null;
                    
                    String dateTimeStr = dateFormatter.format(treatmentDate);
                    if (startTime != null) {
                        dateTimeStr += " um " + timeFormatter.format(startTime) + " Uhr";
                        if (endTime != null && !endTime.equals(startTime)) {
                            dateTimeStr += " - " + timeFormatter.format(endTime) + " Uhr";
                        }
                    }
                    
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 11);
                    yPosition = addTextLine(contentStream, dateTimeStr, margin, yPosition, lineHeight + 2);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(normalFont, 10);
                    
                    // Ort
                    if (treatment.getSurgicalCenterTimeSlot() != null && 
                        treatment.getSurgicalCenterTimeSlot().getSurgicalCenter() != null) {
                        String centerName = treatment.getSurgicalCenterTimeSlot().getSurgicalCenter().getName();
                        if (centerName != null && !centerName.isBlank()) {
                            yPosition = addTextLine(contentStream, "Ort: " + centerName, 
                                                   margin + 10, yPosition, lineHeight);
                        }
                    }
                    
                    // Arzt (aus Treatment.treatingDoctors)
                    String doctorNames = "";
                    if (treatment.getTreatingDoctors() != null && !treatment.getTreatingDoctors().isEmpty()) {
                        doctorNames = treatment.getTreatingDoctors().stream()
                            .map(doctor -> doctor.getFullName() != null ? doctor.getFullName() : doctor.getUsername())
                            .collect(Collectors.joining(", "));
                    }
                    if (!doctorNames.isBlank()) {
                        yPosition = addTextLine(contentStream, "Arzt: " + doctorNames, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Auge
                    if (treatment.getSideOfEye() != null) {
                        String eye = treatment.getSideOfEye().toString();
                        yPosition = addTextLine(contentStream, "Auge: " + eye, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Medikament
                    if (treatment.getMedicationFavourite() != null &&
                        treatment.getMedicationFavourite().getMedication() != null) {
                        String medication = treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                        yPosition = addTextLine(contentStream, "Medikament: " + medication, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Bemerkungen
                    String remarks = "";
                    if (treatment.getAdditionalInfo() != null && !treatment.getAdditionalInfo().isBlank()) {
                        remarks = treatment.getAdditionalInfo();
                    }
                    if (!remarks.isBlank()) {
                        yPosition = addTextLine(contentStream, "Bemerkungen: " + remarks, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    yPosition -= 10;
                    
                    // Trennlinie zwischen Terminen
                    contentStream.setStrokingColor(230f/255f, 230f/255f, 230f/255f);
                    contentStream.setLineWidth(0.5f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(rightMargin, yPosition);
                    contentStream.stroke();
                    contentStream.setStrokingColor(0, 0, 0);
                    yPosition -= 15;
                }
                
                // Nächster Termin am Ende separiert und optisch hervorgehoben
                if (nextTreatment != null) {
                    // Prüfe ob neue Seite benötigt wird
                    if (yPosition < 250) {
                        contentStream.close();
                        addPageNumber(document, document.getNumberOfPages());
                        
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        addWatermark(document, page, institution);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 780;
                    }
                    
                    yPosition -= 20;
                    
                    // Trennlinie vor nächstem Termin (dicker)
                    contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f);
                    contentStream.setLineWidth(2f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(rightMargin, yPosition);
                    contentStream.stroke();
                    contentStream.setStrokingColor(0, 0, 0);
                    yPosition -= 20;
                    
                    // Überschrift "Nächster Termin" (hervorgehoben)
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 14);
                    yPosition = addTextLine(contentStream, "Nächster Termin", margin, yPosition, lineHeight + 4);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    yPosition -= 10;
                    
                    // Termin-Datum und Uhrzeit (hervorgehoben)
                    LocalDate treatmentDate = nextTreatment.getDate();
                    LocalTime startTime = nextTreatment.getSurgicalCenterTimeSlot() != null 
                        ? nextTreatment.getSurgicalCenterTimeSlot().getStartTime() : null;
                    LocalTime endTime = nextTreatment.getSurgicalCenterTimeSlot() != null 
                        ? nextTreatment.getSurgicalCenterTimeSlot().getEndTime() : null;
                    
                    String dateTimeStr = dateFormatter.format(treatmentDate);
                    if (startTime != null) {
                        dateTimeStr += " um " + timeFormatter.format(startTime) + " Uhr";
                        if (endTime != null && !endTime.equals(startTime)) {
                            dateTimeStr += " - " + timeFormatter.format(endTime) + " Uhr";
                        }
                    }
                    
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 12);
                    yPosition = addTextLine(contentStream, dateTimeStr, margin, yPosition, lineHeight + 2);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(normalFont, 10);
                    
                    // Ort
                    if (nextTreatment.getSurgicalCenterTimeSlot() != null && 
                        nextTreatment.getSurgicalCenterTimeSlot().getSurgicalCenter() != null) {
                        String centerName = nextTreatment.getSurgicalCenterTimeSlot().getSurgicalCenter().getName();
                        if (centerName != null && !centerName.isBlank()) {
                            yPosition = addTextLine(contentStream, "Ort: " + centerName, 
                                                   margin + 10, yPosition, lineHeight);
                        }
                    }
                    
                    // Arzt
                    String doctorNames = "";
                    if (nextTreatment.getTreatingDoctors() != null && !nextTreatment.getTreatingDoctors().isEmpty()) {
                        doctorNames = nextTreatment.getTreatingDoctors().stream()
                            .map(doctor -> doctor.getFullName() != null ? doctor.getFullName() : doctor.getUsername())
                            .collect(Collectors.joining(", "));
                    }
                    if (!doctorNames.isBlank()) {
                        yPosition = addTextLine(contentStream, "Arzt: " + doctorNames, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Auge
                    if (nextTreatment.getSideOfEye() != null) {
                        String eye = nextTreatment.getSideOfEye().toString();
                        yPosition = addTextLine(contentStream, "Auge: " + eye, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Medikament
                    if (nextTreatment.getMedicationFavourite() != null &&
                        nextTreatment.getMedicationFavourite().getMedication() != null) {
                        String medication = nextTreatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                        yPosition = addTextLine(contentStream, "Medikament: " + medication, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Bemerkungen
                    String remarks = "";
                    if (nextTreatment.getAdditionalInfo() != null && !nextTreatment.getAdditionalInfo().isBlank()) {
                        remarks = nextTreatment.getAdditionalInfo();
                    }
                    if (!remarks.isBlank()) {
                        yPosition = addTextLine(contentStream, "Bemerkungen: " + remarks, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Zusätzliche Details
                    if (nextTreatment.getDosage() != null && !nextTreatment.getDosage().isBlank()) {
                        yPosition = addTextLine(contentStream, "Dosierung: " + nextTreatment.getDosage(), 
                                               margin + 10, yPosition, lineHeight);
                    }
                    if (nextTreatment.getFrequency() != null && !nextTreatment.getFrequency().isBlank()) {
                        yPosition = addTextLine(contentStream, "Frequenz: " + nextTreatment.getFrequency(), 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    yPosition -= 10;
                }
            }
            
            // Vertraulichkeitsklausel am Ende der Seite
            // Positioniere am unteren Seitenrand (ca. 60px vom unteren Rand)
            int bottomMargin = 60;
            yPosition = bottomMargin + (lineHeight * 3) + 10;
            yPosition = addConfidentialityClause(contentStream, location, margin, yPosition, 
                                                 lineHeight, headerFont, normalFont);
            
            // Seitenzahl
            addPageNumber(document, document.getNumberOfPages());
            
            contentStream.close();
            
            // Speichere Dokument
            document.save(baos);
            document.close();
            
            byte[] pdfBytes = baos.toByteArray();
            
            // Erstelle geschütztes Dokument
            try (PDDocument protectedDocument = Loader.loadPDF(pdfBytes);
                 ByteArrayOutputStream protectedBaos = new ByteArrayOutputStream()) {
                
                // Setze Berechtigungen - nur Lesen und Drucken, kein Bearbeiten
                AccessPermission permission = new AccessPermission();
                permission.setCanModify(false);
                permission.setCanExtractContent(false);
                permission.setCanExtractForAccessibility(false);
                permission.setCanModifyAnnotations(false);
                permission.setCanFillInForm(false);
                permission.setCanAssembleDocument(false);
                permission.setCanPrint(true);
                
                // Wende Schutz an
                StandardProtectionPolicy policy = new StandardProtectionPolicy("", "", permission);
                protectedDocument.protect(policy);
                
                protectedDocument.save(protectedBaos);
                protectedDocument.close();
                
                return protectedBaos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Generieren des Termin-Ausdrucks", e);
        }
    }
    
    private byte[] generatePdfReportForPatient(Patient patient, List<Appointment> appointments) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Lade Standort-Daten
            Location location = locationService.getDefaultLocation();
            
            // Erstelle erste Seite und füge Wasserzeichen hinzu
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Füge Wasserzeichen hinzu (Institution aus Location)
            Institution institution = location != null ? location.getInstitution() : null;
            addWatermark(document, page, institution);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            int yPosition = 780;
            int lineHeight = 14;
            int margin = 50;
            int rightMargin = 545;
            
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            // Titel
            String titleText = "Terminübersicht";
            contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
            contentStream.setFont(titleFont, 18);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(titleText);
            contentStream.endText();
            contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
            yPosition -= 30;
            
            // Praxis-Daten
            if (location != null) {
                contentStream.setFont(headerFont, 12);
                yPosition = addTextLine(contentStream, "Praxis", margin, yPosition, lineHeight);
                contentStream.setFont(normalFont, 10);
                
                if (location.getLocationName() != null && !location.getLocationName().isBlank()) {
                    yPosition = addTextLine(contentStream, location.getLocationName(), margin + 10, yPosition, lineHeight);
                }
                String fullAddress = location.getFullAddress();
                if (fullAddress != null && !fullAddress.isBlank()) {
                    yPosition = addTextLine(contentStream, fullAddress, margin + 10, yPosition, lineHeight);
                }
                if (location.getPhone() != null && !location.getPhone().isBlank()) {
                    yPosition = addTextLine(contentStream, "Tel: " + location.getPhone(), margin + 10, yPosition, lineHeight);
                }
                if (location.getEmail() != null && !location.getEmail().isBlank()) {
                    yPosition = addTextLine(contentStream, "E-Mail: " + location.getEmail(), margin + 10, yPosition, lineHeight);
                }
                
                yPosition -= 10;
                
                // Trennlinie
                contentStream.setStrokingColor(200f/255f, 200f/255f, 200f/255f);
                contentStream.setLineWidth(1.5f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                contentStream.setStrokingColor(0, 0, 0);
                yPosition -= 20;
            }
            
            // Patient-Informationen
            contentStream.setFont(headerFont, 12);
            yPosition = addTextLine(contentStream, "Patient", margin, yPosition, lineHeight);
            contentStream.setFont(normalFont, 10);
            
            String patientName = patient.getLastName() + ", " + patient.getFirstName();
            yPosition = addTextLine(contentStream, patientName, margin + 10, yPosition, lineHeight);
            
            if (patient.getAddress() != null) {
                String address = patient.getAddress().toString();
                if (address != null && !address.isBlank()) {
                    yPosition = addTextLine(contentStream, address, margin + 10, yPosition, lineHeight);
                }
            }
            
            yPosition -= 10;
            
            // Trennlinie
            contentStream.setStrokingColor(200f/255f, 200f/255f, 200f/255f);
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            contentStream.setStrokingColor(0, 0, 0);
            yPosition -= 20;
            
            // Termine - kompakt und übersichtlich
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            
            if (appointments.isEmpty()) {
                contentStream.setFont(normalFont, 10);
                yPosition = addTextLine(contentStream, "Keine geplanten Termine", margin, yPosition, lineHeight);
            } else {
                // Sortiere alle Termine chronologisch
                List<Appointment> sortedAppointments = appointments.stream()
                    .filter(apt -> apt.getStartTime() != null)
                    .sorted((a1, a2) -> a1.getStartTime().compareTo(a2.getStartTime()))
                    .collect(Collectors.toList());
                
                // Trenne nächsten Termin vom Rest (falls vorhanden)
                LocalDateTime now = LocalDateTime.now();
                Appointment nextAppointment = sortedAppointments.stream()
                    .filter(apt -> apt.getStartTime().isAfter(now))
                    .findFirst()
                    .orElse(null);
                
                // Alle Termine außer dem nächsten (falls vorhanden)
                List<Appointment> otherAppointments = sortedAppointments.stream()
                    .filter(apt -> nextAppointment == null || !apt.equals(nextAppointment))
                    .collect(Collectors.toList());
                
                // Zeige zuerst alle anderen Termine
                for (Appointment appointment : otherAppointments) {
                    // Prüfe ob neue Seite benötigt wird
                    if (yPosition < 150) {
                        contentStream.close();
                        addPageNumber(document, document.getNumberOfPages());
                        
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        addWatermark(document, page, institution);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 780;
                    }
                    
                    // Termin-Datum und Uhrzeit (hervorgehoben)
                    LocalDateTime startTime = appointment.getStartTime();
                    LocalDateTime endTime = appointment.getEndTime();
                    
                    String dateTimeStr = dateFormatter.format(startTime) + " um " + 
                                        timeFormatter.format(startTime) + " Uhr";
                    if (endTime != null && !endTime.equals(startTime)) {
                        dateTimeStr += " - " + timeFormatter.format(endTime) + " Uhr";
                    }
                    
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 11);
                    yPosition = addTextLine(contentStream, dateTimeStr, margin, yPosition, lineHeight + 2);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(normalFont, 10);
                    
                    // Ort
                    if (appointment.getScheduler() != null && 
                        appointment.getScheduler().getLocation() != null) {
                        Location appointmentLocation = appointment.getScheduler().getLocation();
                        String locationName = appointmentLocation.getLocationName();
                        if (locationName != null && !locationName.isBlank()) {
                            yPosition = addTextLine(contentStream, "Ort: " + locationName, 
                                                   margin + 10, yPosition, lineHeight);
                        }
                    }
                    
                    // Arzt (aus Treatment.treatingDoctors)
                    String doctorNames = "";
                    if (appointment.getTreatment() != null && 
                        appointment.getTreatment().getTreatingDoctors() != null &&
                        !appointment.getTreatment().getTreatingDoctors().isEmpty()) {
                        doctorNames = appointment.getTreatment().getTreatingDoctors().stream()
                            .map(doctor -> doctor.getFullName() != null ? doctor.getFullName() : doctor.getUsername())
                            .collect(Collectors.joining(", "));
                    }
                    if (!doctorNames.isBlank()) {
                        yPosition = addTextLine(contentStream, "Arzt: " + doctorNames, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Auge (aus Treatment)
                    if (appointment.getTreatment() != null && 
                        appointment.getTreatment().getSideOfEye() != null) {
                        String eye = appointment.getTreatment().getSideOfEye().toString();
                        yPosition = addTextLine(contentStream, "Auge: " + eye, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Medikament (aus Treatment)
                    if (appointment.getTreatment() != null && 
                        appointment.getTreatment().getMedicationFavourite() != null &&
                        appointment.getTreatment().getMedicationFavourite().getMedication() != null) {
                        String medication = appointment.getTreatment().getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                        yPosition = addTextLine(contentStream, "Medikament: " + medication, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Bemerkungen (aus Appointment.notes oder Treatment.additionalInfo)
                    String remarks = "";
                    if (appointment.getNotes() != null && !appointment.getNotes().isBlank()) {
                        remarks = appointment.getNotes();
                    } else if (appointment.getTreatment() != null && 
                               appointment.getTreatment().getAdditionalInfo() != null &&
                               !appointment.getTreatment().getAdditionalInfo().isBlank()) {
                        remarks = appointment.getTreatment().getAdditionalInfo();
                    }
                    if (!remarks.isBlank()) {
                        yPosition = addTextLine(contentStream, "Bemerkungen: " + remarks, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    yPosition -= 10;
                    
                    // Trennlinie zwischen Terminen
                    contentStream.setStrokingColor(230f/255f, 230f/255f, 230f/255f);
                    contentStream.setLineWidth(0.5f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(rightMargin, yPosition);
                    contentStream.stroke();
                    contentStream.setStrokingColor(0, 0, 0);
                    yPosition -= 15;
                }
                
                // Nächster Termin am Ende separiert und optisch hervorgehoben
                if (nextAppointment != null) {
                    // Prüfe ob neue Seite benötigt wird
                    if (yPosition < 250) {
                        contentStream.close();
                        addPageNumber(document, document.getNumberOfPages());
                        
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        addWatermark(document, page, institution);
                        contentStream = new PDPageContentStream(document, page);
                        yPosition = 780;
                    }
                    
                    yPosition -= 20;
                    
                    // Trennlinie vor nächstem Termin (dicker)
                    contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f);
                    contentStream.setLineWidth(2f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(rightMargin, yPosition);
                    contentStream.stroke();
                    contentStream.setStrokingColor(0, 0, 0);
                    yPosition -= 20;
                    
                    // Überschrift "Nächster Termin" (hervorgehoben)
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 14);
                    yPosition = addTextLine(contentStream, "Nächster Termin", margin, yPosition, lineHeight + 4);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    yPosition -= 10;
                    
                    // Termin-Datum und Uhrzeit (hervorgehoben)
                    LocalDateTime startTime = nextAppointment.getStartTime();
                    LocalDateTime endTime = nextAppointment.getEndTime();
                    
                    String dateTimeStr = dateFormatter.format(startTime) + " um " + 
                                        timeFormatter.format(startTime) + " Uhr";
                    if (endTime != null && !endTime.equals(startTime)) {
                        dateTimeStr += " - " + timeFormatter.format(endTime) + " Uhr";
                    }
                    
                    contentStream.setNonStrokingColor(33f/255f, 150f/255f, 243f/255f); // Primary blue
                    contentStream.setFont(headerFont, 12);
                    yPosition = addTextLine(contentStream, dateTimeStr, margin, yPosition, lineHeight + 2);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(normalFont, 10);
                    
                    // Ort
                    if (nextAppointment.getScheduler() != null && 
                        nextAppointment.getScheduler().getLocation() != null) {
                        Location appointmentLocation = nextAppointment.getScheduler().getLocation();
                        String locationName = appointmentLocation.getLocationName();
                        if (locationName != null && !locationName.isBlank()) {
                            yPosition = addTextLine(contentStream, "Ort: " + locationName, 
                                                   margin + 10, yPosition, lineHeight);
                        }
                    }
                    
                    // Arzt (aus Treatment.treatingDoctors)
                    String doctorNames = "";
                    if (nextAppointment.getTreatment() != null && 
                        nextAppointment.getTreatment().getTreatingDoctors() != null &&
                        !nextAppointment.getTreatment().getTreatingDoctors().isEmpty()) {
                        doctorNames = nextAppointment.getTreatment().getTreatingDoctors().stream()
                            .map(doctor -> doctor.getFullName() != null ? doctor.getFullName() : doctor.getUsername())
                            .collect(Collectors.joining(", "));
                    }
                    if (!doctorNames.isBlank()) {
                        yPosition = addTextLine(contentStream, "Arzt: " + doctorNames, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Auge (aus Treatment)
                    if (nextAppointment.getTreatment() != null && 
                        nextAppointment.getTreatment().getSideOfEye() != null) {
                        String eye = nextAppointment.getTreatment().getSideOfEye().toString();
                        yPosition = addTextLine(contentStream, "Auge: " + eye, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Medikament (aus Treatment)
                    if (nextAppointment.getTreatment() != null && 
                        nextAppointment.getTreatment().getMedicationFavourite() != null &&
                        nextAppointment.getTreatment().getMedicationFavourite().getMedication() != null) {
                        String medication = nextAppointment.getTreatment().getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                        yPosition = addTextLine(contentStream, "Medikament: " + medication, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Bemerkungen (aus Appointment.notes oder Treatment.additionalInfo)
                    String remarks = "";
                    if (nextAppointment.getNotes() != null && !nextAppointment.getNotes().isBlank()) {
                        remarks = nextAppointment.getNotes();
                    } else if (nextAppointment.getTreatment() != null && 
                               nextAppointment.getTreatment().getAdditionalInfo() != null &&
                               !nextAppointment.getTreatment().getAdditionalInfo().isBlank()) {
                        remarks = nextAppointment.getTreatment().getAdditionalInfo();
                    }
                    if (!remarks.isBlank()) {
                        yPosition = addTextLine(contentStream, "Bemerkungen: " + remarks, 
                                               margin + 10, yPosition, lineHeight);
                    }
                    
                    // Zusätzliche Details zur Behandlung (falls vorhanden)
                    if (nextAppointment.getTreatment() != null) {
                        if (nextAppointment.getTreatment().getDosage() != null && !nextAppointment.getTreatment().getDosage().isBlank()) {
                            yPosition = addTextLine(contentStream, "Dosierung: " + nextAppointment.getTreatment().getDosage(), 
                                                   margin + 10, yPosition, lineHeight);
                        }
                        if (nextAppointment.getTreatment().getFrequency() != null && !nextAppointment.getTreatment().getFrequency().isBlank()) {
                            yPosition = addTextLine(contentStream, "Frequenz: " + nextAppointment.getTreatment().getFrequency(), 
                                                   margin + 10, yPosition, lineHeight);
                        }
                    }
                    
                    yPosition -= 10;
                }
            }
            
            // Vertraulichkeitsklausel am Ende der Seite
            // Positioniere am unteren Seitenrand (ca. 60px vom unteren Rand)
            int bottomMargin = 60;
            yPosition = bottomMargin + (lineHeight * 3) + 10;
            yPosition = addConfidentialityClause(contentStream, location, margin, yPosition, 
                                                 lineHeight, headerFont, normalFont);
            
            // Seitenzahl
            addPageNumber(document, document.getNumberOfPages());
            
            contentStream.close();
            
            // Speichere Dokument
            document.save(baos);
            document.close();
            
            byte[] pdfBytes = baos.toByteArray();
            
            // Erstelle geschütztes Dokument
            try (PDDocument protectedDocument = Loader.loadPDF(pdfBytes);
                 ByteArrayOutputStream protectedBaos = new ByteArrayOutputStream()) {
                
                // Setze Berechtigungen - nur Lesen und Drucken, kein Bearbeiten
                AccessPermission permission = new AccessPermission();
                permission.setCanModify(false);
                permission.setCanExtractContent(false);
                permission.setCanExtractForAccessibility(false);
                permission.setCanModifyAnnotations(false);
                permission.setCanFillInForm(false);
                permission.setCanAssembleDocument(false);
                permission.setCanPrint(true);
                
                // Wende Schutz an
                StandardProtectionPolicy policy = new StandardProtectionPolicy("", "", permission);
                protectedDocument.protect(policy);
                
                protectedDocument.save(protectedBaos);
                protectedDocument.close();
                
                return protectedBaos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Generieren des Termin-Ausdrucks", e);
        }
    }
    
    private int addTextLine(PDPageContentStream contentStream, String text, int x, int y, int lineHeight) 
            throws IOException {
        // Text umbrechen falls zu lang
        int maxWidth = 90;
        if (text.length() > maxWidth) {
            String[] wrapped = wrapText(text, maxWidth);
            for (int i = 0; i < wrapped.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(x, y - (i * lineHeight));
                contentStream.showText(wrapped[i]);
                contentStream.endText();
            }
            return y - (wrapped.length * lineHeight);
        } else {
            contentStream.beginText();
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(text);
            contentStream.endText();
            return y - lineHeight;
        }
    }
    
    private String[] wrapText(String text, int maxWidth) {
        // Einfache Textumbrechung
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxWidth) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    // Wort ist zu lang, teile es
                    lines.add(word.substring(0, Math.min(maxWidth, word.length())));
                    if (word.length() > maxWidth) {
                        currentLine = new StringBuilder(word.substring(maxWidth));
                    }
                }
            }
        }
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    private void addWatermark(PDDocument document, PDPage page, Institution institution) throws IOException {
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        try (PDPageContentStream watermarkStream = new PDPageContentStream(document, page, 
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDRectangle pageSize = page.getMediaBox();
            float width = pageSize.getWidth();
            float height = pageSize.getHeight();
            
            String watermarkText = "";
            if (institution != null && institution.getInstitutionName() != null && 
                !institution.getInstitutionName().isBlank()) {
                watermarkText = institution.getInstitutionName();
            }
            
            if (!watermarkText.isBlank()) {
                float fontSize = 20f;
                watermarkStream.setFont(font, fontSize);
                watermarkStream.setNonStrokingColor(220f/255f, 220f/255f, 220f/255f);
                
                float textWidth = font.getStringWidth(watermarkText) / 1000 * fontSize;
                float textHeight = fontSize;
                
                watermarkStream.beginText();
                watermarkStream.setFont(font, fontSize);
                watermarkStream.newLineAtOffset(width/2 - textWidth/2, height/2 - textHeight/2);
                watermarkStream.showText(watermarkText);
                watermarkStream.endText();
            }
            
            watermarkStream.close();
        }
    }
    
    private int addConfidentialityClause(PDPageContentStream contentStream, Location location, 
            int margin, int yPosition, int lineHeight, PDType1Font headerFont, PDType1Font normalFont) 
            throws IOException {
        
        // Kompakte Vertraulichkeitsklausel ohne roten Hintergrund
        // Einfache Trennlinie oben
        contentStream.setStrokingColor(200f/255f, 200f/255f, 200f/255f);
        contentStream.setLineWidth(0.5f);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(margin + 495, yPosition);
        contentStream.stroke();
        contentStream.setStrokingColor(0, 0, 0);
        yPosition -= 10;
        
        // Überschrift in normaler Farbe
        contentStream.setFont(headerFont, 9);
        contentStream.setNonStrokingColor(0, 0, 0);
        yPosition = addTextLine(contentStream, "Vertraulichkeitserklärung", margin, yPosition, lineHeight);
        
        // Text in normaler Farbe
        contentStream.setFont(normalFont, 8);
        String clauseText = "Dieses Dokument enthält vertrauliche Informationen. " +
                           "Die Weitergabe an Dritte ist untersagt.";
        yPosition = addTextLine(contentStream, clauseText, margin, yPosition, lineHeight);
        
        return yPosition - 10;
    }
    
    private void addPageNumber(PDDocument document, int pageNumber) throws IOException {
        if (document.getNumberOfPages() > 0) {
            PDPage page = document.getPage(pageNumber - 1);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            try (PDPageContentStream contentStream = new PDPageContentStream(document, page, 
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                contentStream.setFont(font, 9);
                contentStream.setNonStrokingColor(128f/255f, 128f/255f, 128f/255f);
                contentStream.beginText();
                contentStream.newLineAtOffset(270, 30);
                contentStream.showText("Seite " + pageNumber);
                contentStream.endText();
                contentStream.close();
            }
        }
    }
}

