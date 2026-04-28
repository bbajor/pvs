package de.bbajor.pvs.taskmanagement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

@Service
public class TreatmentReportService {

    @Autowired
    private Clock clock;
    
    @Autowired
    private LocationService locationService;
    
    @Autowired
    private UserAccountRepository userAccountRepository;
    
    @Autowired
    private InstitutionRepository institutionRepository;
    
    @Autowired
    private TreatmentRepository treatmentRepository;
    
    public byte[] generatePatientPdfReport(Treatment treatment, SurgicalCenterTimeSlot timeSlot, 
            String treatingDoctor, boolean isApproved) {
        return generatePdfReportForPatient(java.util.List.of(treatment), timeSlot, treatingDoctor, isApproved);
    }
    
    public byte[] generatePdfReport(List<Treatment> treatments, SurgicalCenterTimeSlot timeSlot, 
            String treatingDoctor, boolean allApproved) {
        return generatePdfReportForPatient(treatments, timeSlot, treatingDoctor, allApproved);
    }
    
    private byte[] generatePdfReportForPatient(List<Treatment> treatments, SurgicalCenterTimeSlot timeSlot, 
            String treatingDoctor, boolean allApproved) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Load location and institution data
            Location location = locationService.getDefaultLocation();
            Institution institution = location != null ? location.getInstitution() : null;
            
            // Load full institution data (including watermark and website URL)
            if (institution != null && institution.getId() != null) {
                institution = institutionRepository.findById(institution.getId()).orElse(institution);
            }
            
            // Create first page and add watermark
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Add watermark to the page (from institution if available)
            addWatermark(document, page, institution);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            int yPosition = 780;
            int lineHeight = 16;
            int margin = 50;
            int rightMargin = 545;
            int sectionSpacing = 20; // Professional spacing between sections
            
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            // Professional title with German date format
            String titleText = "Behandlungsprotokoll";
            if (timeSlot != null) {
                String germanDate = formatGermanDate(timeSlot.getDate());
                titleText += " vom " + germanDate;
            }
            
            // Title with professional styling (dark blue, high contrast)
            contentStream.setNonStrokingColor(0f/255f, 51f/255f, 153f/255f); // Dark blue, high contrast
            contentStream.setFont(titleFont, 20); // Slightly larger for more professional look
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(titleText);
            contentStream.endText();
            contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
            yPosition -= 30; // More space after title
            
            // Compact header section with two columns
            contentStream.setFont(normalFont, 9);
            int leftCol = margin;
            int startY = yPosition;
            
            // Left column: Behandelnde Einrichtung
            if (location != null) {
                contentStream.setNonStrokingColor(0, 0, 0); // Black for headers (high contrast)
                contentStream.setFont(headerFont, 10);
                yPosition = addTextLine(contentStream, "Behandelnde Einrichtung", leftCol, yPosition, lineHeight);
                contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                contentStream.setFont(normalFont, 9);
                if (location.getLocationName() != null && !location.getLocationName().isBlank()) {
                    yPosition = addTextLine(contentStream, location.getLocationName(), leftCol, yPosition, lineHeight);
                }
                String fullAddress = location.getFullAddress();
                if (fullAddress != null && !fullAddress.isBlank()) {
                    yPosition = addTextLine(contentStream, fullAddress, leftCol, yPosition, lineHeight);
                }
                if (location.getPhone() != null && !location.getPhone().isBlank()) {
                    yPosition = addTextLine(contentStream, "Tel: " + location.getPhone(), leftCol, yPosition, lineHeight);
                }
                if (location.getEmail() != null && !location.getEmail().isBlank()) {
                    yPosition = addTextLine(contentStream, location.getEmail(), leftCol, yPosition, lineHeight);
                }
            }
            
            // Right column: QR-Code if website URL available
            yPosition = startY;
            if (institution != null && institution.getWebsiteUrl() != null && !institution.getWebsiteUrl().isBlank()) {
                try {
                    BufferedImage qrImage = generateQRCode(institution.getWebsiteUrl(), 100, 100);
                    ByteArrayOutputStream qrBaos = new ByteArrayOutputStream();
                    ImageIO.write(qrImage, "PNG", qrBaos);
                    PDImageXObject qrCodeImage = PDImageXObject.createFromByteArray(document, qrBaos.toByteArray(), "qr-code");
                    contentStream.drawImage(qrCodeImage, rightMargin - 100, yPosition - 100, 100, 100);
                } catch (Exception e) {
                    // QR code generation failed, continue without it
                }
            }
            
            // Use minimum Y position from both columns
            yPosition = Math.min(yPosition, startY - (location != null ? 80 : 40));
            yPosition -= sectionSpacing;
            
            // Section header with separation line: Termin (professional styling)
            contentStream.setNonStrokingColor(0, 0, 0); // Black for headers
            contentStream.setFont(headerFont, 12); // Slightly larger for better hierarchy
            yPosition = addTextLine(contentStream, "Termin", margin, yPosition, lineHeight);
            contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
            yPosition -= 6;
            
            // Professional separation line (slightly thicker)
            contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f); // Darker gray for better visibility
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            contentStream.setStrokingColor(0, 0, 0);
            yPosition -= 12;
            
            // Termin content
            contentStream.setFont(normalFont, 9);
            if (timeSlot != null && timeSlot.getSurgicalCenter() != null) {
                // Behandlungsort fett hervorheben
                contentStream.setFont(headerFont, 9);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Behandlungsort: ");
                contentStream.endText();
                contentStream.setFont(normalFont, 9);
                contentStream.beginText();
                float textWidth = headerFont.getStringWidth("Behandlungsort: ") / 1000 * 9;
                contentStream.newLineAtOffset(margin + textWidth, yPosition);
                contentStream.showText(timeSlot.getSurgicalCenter().getName());
                contentStream.endText();
                yPosition -= lineHeight;
                if (timeSlot.getSurgicalCenter().getAddress() != null) {
                    yPosition = addTextLine(contentStream, "Adresse: " + timeSlot.getSurgicalCenter().getAddress().toString(), margin, yPosition, lineHeight);
                }
                if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                    DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    String dateTime = dateFormatter.format(timeSlot.getDate()) + " " + timeSlot.getStartTime() + "-" + timeSlot.getEndTime() + " Uhr";
                    yPosition = addTextLine(contentStream, "Datum/Zeit: " + dateTime, margin, yPosition, lineHeight);
                }
            }
            
            // Get full name of treating doctor
            String treatingDoctorFullName = treatingDoctor;
            try {
                UserAccount treatingDoctorAccount = userAccountRepository.findByUsername(treatingDoctor).orElse(null);
                if (treatingDoctorAccount != null && treatingDoctorAccount.getFullName() != null && !treatingDoctorAccount.getFullName().isBlank()) {
                    treatingDoctorFullName = treatingDoctorAccount.getFullName();
                }
            } catch (Exception e) {
                // Fallback to username if lookup fails
            }
            yPosition = addTextLine(contentStream, "Behandelnder Arzt: " + treatingDoctorFullName, margin, yPosition, lineHeight);
            yPosition -= sectionSpacing;
            
            // Section header with separation line: Details (professional styling)
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.setFont(headerFont, 12); // Slightly larger for better hierarchy
            yPosition = addTextLine(contentStream, "Details", margin, yPosition, lineHeight);
            contentStream.setNonStrokingColor(0, 0, 0);
            yPosition -= 6;
            
            // Professional separation line (slightly thicker)
            contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f); // Darker gray for better visibility
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            contentStream.setStrokingColor(0, 0, 0);
            yPosition -= 12;
            
            // Sortiere Treatments: Zuerst nach Auge (RIGHT, dann LEFT), dann nach Nachname
            List<Treatment> sortedTreatments = treatments.stream()
                .sorted(Comparator
                    .comparing((Treatment t) -> {
                        SideOfEye eye = t.getSideOfEye();
                        // RIGHT = 0, LEFT = 1, null = 2 (kommt ganz ans Ende)
                        if (eye == SideOfEye.RIGHT) return 0;
                        if (eye == SideOfEye.LEFT) return 1;
                        return 2;
                    })
                    .thenComparing((Treatment t) -> {
                        Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                        String lastName = patient != null && patient.getLastName() != null 
                            ? patient.getLastName() 
                            : "";
                        return lastName.toLowerCase(); // Case-insensitive Sortierung
                    }))
                .collect(Collectors.toList());
            
            // Tabellenkopf
            int colNr = margin;
            int colName = margin + 25;
            int colVorname = margin + 90;
            int colGeburtsdatum = margin + 165;
            int colVersicherung = margin + 245;
            int colAuge = margin + 320;
            // Medikament, Status und Bemerkungen in zweiter Zeile
            
            // Mindestabstand zum Footer (für Vertraulichkeitsklausel)
            int minYForFooter = 180;
            
            // Tabellenkopf - erste Zeile
            contentStream.setFont(headerFont, 9);
            contentStream.setNonStrokingColor(0, 0, 0);
            int headerY = yPosition;
            addTextLine(contentStream, "Nr.", colNr, headerY, lineHeight);
            addTextLine(contentStream, "Name", colName, headerY, lineHeight);
            addTextLine(contentStream, "Vorname", colVorname, headerY, lineHeight);
            addTextLine(contentStream, "Geburtsdatum", colGeburtsdatum, headerY, lineHeight);
            addTextLine(contentStream, "Versicherung", colVersicherung, headerY, lineHeight);
            addTextLine(contentStream, "Auge", colAuge, headerY, lineHeight);
            
            // Zweite Zeile des Headers für Medikament und Status
            headerY -= lineHeight;
            contentStream.setFont(headerFont, 9);
            addTextLine(contentStream, "Medikament", colName, headerY, lineHeight);
            addTextLine(contentStream, "Status", colName + 150, headerY, lineHeight);
            
            // Dritte Zeile des Headers für Bemerkungen
            headerY -= lineHeight;
            contentStream.setFont(headerFont, 9);
            addTextLine(contentStream, "Bemerkungen", colName, headerY, lineHeight);
            yPosition = headerY;
            
            // Trennlinie unter Tabellenkopf
            yPosition -= 5;
            contentStream.setStrokingColor(0, 0, 0);
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            yPosition -= 10;
            
            contentStream.setFont(normalFont, 8);
            
            int rowNumber = 1;
            
            for (Treatment treatment : sortedTreatments) {
                // Prüfe ob neue Seite nötig (mit Platz für Footer)
                // Jeder Patient benötigt 3 Zeilen (Hauptzeile + Medikament/Status + Bemerkungen)
                int patientHeight = lineHeight * 3 + 2; // 3 Zeilen + Abstand
                if (yPosition - patientHeight < minYForFooter) {
                    contentStream.close();
                    addPageNumber(document, document.getNumberOfPages());
                    
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    addWatermark(document, page, institution);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 780;
                    
                    // Tabellenkopf erneut zeichnen - erste Zeile
                    contentStream.setFont(headerFont, 9);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    headerY = yPosition;
                    addTextLine(contentStream, "Nr.", colNr, headerY, lineHeight);
                    addTextLine(contentStream, "Name", colName, headerY, lineHeight);
                    addTextLine(contentStream, "Vorname", colVorname, headerY, lineHeight);
                    addTextLine(contentStream, "Geburtsdatum", colGeburtsdatum, headerY, lineHeight);
                    addTextLine(contentStream, "Versicherung", colVersicherung, headerY, lineHeight);
                    addTextLine(contentStream, "Auge", colAuge, headerY, lineHeight);
                    
                    // Zweite Zeile des Headers für Medikament und Status
                    headerY -= lineHeight;
                    contentStream.setFont(headerFont, 9);
                    addTextLine(contentStream, "Medikament", colName, headerY, lineHeight);
                    addTextLine(contentStream, "Status", colName + 150, headerY, lineHeight);
                    
                    // Dritte Zeile des Headers für Bemerkungen
                    headerY -= lineHeight;
                    contentStream.setFont(headerFont, 9);
                    addTextLine(contentStream, "Bemerkungen", colName, headerY, lineHeight);
                    yPosition = headerY;
                    
                    // Trennlinie unter Tabellenkopf
                    yPosition -= 5;
                    contentStream.setStrokingColor(0, 0, 0);
                    contentStream.setLineWidth(1f);
                    contentStream.moveTo(margin, yPosition);
                    contentStream.lineTo(rightMargin, yPosition);
                    contentStream.stroke();
                    yPosition -= 10;
                    contentStream.setFont(normalFont, 8);
                }
                
                Patient patient = treatment.getTreatmentPlan() != null ? treatment.getTreatmentPlan().getPatient() : null;
                int currentRowY = yPosition;
                
                // Tabellenzeile - Hauptzeile
                addTextLine(contentStream, String.valueOf(rowNumber++), colNr, currentRowY, lineHeight);
                
                String name = patient != null && patient.getLastName() != null ? patient.getLastName() : "-";
                if (name.length() > 18) name = name.substring(0, 15) + "...";
                addTextLine(contentStream, name, colName, currentRowY, lineHeight);
                
                String vorname = patient != null && patient.getFirstName() != null ? patient.getFirstName() : "-";
                if (vorname.length() > 18) vorname = vorname.substring(0, 15) + "...";
                addTextLine(contentStream, vorname, colVorname, currentRowY, lineHeight);
                
                String geburtsdatum = "-";
                if (patient != null && patient.getBirth() != null) {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    geburtsdatum = dateFormatter.format(patient.getBirth());
                }
                addTextLine(contentStream, geburtsdatum, colGeburtsdatum, currentRowY, lineHeight);
                
                String versicherung = "-";
                if (patient != null && patient.getHealthInsurance() != null && 
                    patient.getHealthInsurance().getCostCarrierName() != null) {
                    versicherung = patient.getHealthInsurance().getCostCarrierName();
                    if (versicherung.length() > 12) versicherung = versicherung.substring(0, 9) + "...";
                }
                addTextLine(contentStream, versicherung, colVersicherung, currentRowY, lineHeight);
                
                String auge = treatment.getSideOfEye() != null ? treatment.getSideOfEye().toString() : "-";
                addTextLine(contentStream, auge, colAuge, currentRowY, lineHeight);
                
                // Zweite Zeile für Medikament und Status
                currentRowY -= lineHeight;
                String medikament = "-";
                if (treatment.getMedicationFavourite() != null && treatment.getMedicationFavourite().getMedication() != null) {
                    medikament = treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                    if (medikament.length() > 30) medikament = medikament.substring(0, 27) + "...";
                }
                addTextLine(contentStream, "Medikament: " + medikament, colName, currentRowY, lineHeight);
                
                // Status mit farbiger Schrift
                boolean isApproved = treatment.getApprovalDate() != null;
                String status = isApproved ? "Geprüft" : "Nicht geprüft";
                String statusText = "Status: " + status;
                int statusX = colName + 150;
                
                // Setze Schriftfarbe basierend auf Prüfstatus
                if (isApproved) {
                    contentStream.setNonStrokingColor(0f, 0.6f, 0f); // Grün
                } else {
                    contentStream.setNonStrokingColor(0.8f, 0.6f, 0f); // Gelb/Orange
                }
                
                // Zeichne Status-Text mit farbiger Schrift
                addTextLine(contentStream, statusText, statusX, currentRowY, lineHeight);
                
                // Zurück zu schwarz für weiteren Text
                contentStream.setNonStrokingColor(0, 0, 0);
                
                // Dritte Zeile für Bemerkungen
                currentRowY -= lineHeight;
                String bemerkungen = treatment.getAdditionalInfo() != null && !treatment.getAdditionalInfo().isBlank() 
                    ? treatment.getAdditionalInfo() : "-";
                if (bemerkungen.length() > 60) bemerkungen = bemerkungen.substring(0, 57) + "...";
                if (!bemerkungen.equals("-")) {
                    addTextLine(contentStream, "Bemerkungen: " + bemerkungen, colName, currentRowY, lineHeight);
                }
                
                yPosition = currentRowY - lineHeight - 2; // Abstand zwischen Patienten
            }
            
            // Section header with separation line: Prüfung (moved to end, before confidentiality clause)
            yPosition -= sectionSpacing;
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.setFont(headerFont, 12); // Slightly larger for better hierarchy
            yPosition = addTextLine(contentStream, "Prüfung", margin, yPosition, lineHeight);
            contentStream.setNonStrokingColor(0, 0, 0);
            yPosition -= 6;
            
            // Professional separation line (slightly thicker)
            contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f); // Darker gray for better visibility
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(rightMargin, yPosition);
            contentStream.stroke();
            contentStream.setStrokingColor(0, 0, 0);
            yPosition -= 12;
            
            // Prüfungsstatus: Prüfe ob alle Behandlungen geprüft wurden
            boolean allTreatmentsApproved = sortedTreatments.stream().allMatch(t -> t.getApprovalDate() != null);
            
            // Prüfungsstatus mit farbiger Schrift anzeigen
            yPosition -= 5;
            String statusText = allTreatmentsApproved ? "Prüfung abgeschlossen" : "Prüfung ausstehend";
            int statusX = margin;
            
            // Setze Schriftfarbe basierend auf Prüfstatus
            contentStream.setFont(headerFont, 10);
            if (allTreatmentsApproved) {
                contentStream.setNonStrokingColor(0f, 0.6f, 0f); // Grün
            } else {
                contentStream.setNonStrokingColor(0.8f, 0.6f, 0f); // Gelb/Orange
            }
            
            // Zeichne Status-Text mit farbiger Schrift
            yPosition = addTextLine(contentStream, statusText, statusX, yPosition, lineHeight);
            
            // Zurück zu schwarz für weiteren Text
            contentStream.setNonStrokingColor(0, 0, 0);
            yPosition -= 10;
            
            // Prüfungsinformationen für alle Behandlungen
            contentStream.setFont(normalFont, 9);
            
            // Sammle alle dokumentierten Behandlungen
            List<Treatment> documentedTreatments = sortedTreatments.stream()
                    .filter(t -> t.getApprovalDate() != null)
                    .collect(Collectors.toList());
            
            if (!documentedTreatments.isEmpty()) {
                // Wenn nur eine Behandlung dokumentiert wurde, zeige Details
                if (documentedTreatments.size() == 1) {
                    Treatment documentedTreatment = documentedTreatments.get(0);
                    
                    // Dokumentationsdatum
                    if (documentedTreatment.getApprovalDateTime() != null) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        yPosition = addTextLine(contentStream, "Dokumentiert am: " + dateFormatter.format(documentedTreatment.getApprovalDateTime()), margin, yPosition, lineHeight);
                    } else if (documentedTreatment.getApprovalDate() != null) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        yPosition = addTextLine(contentStream, "Dokumentiert am: " + dateFormatter.format(documentedTreatment.getApprovalDate()), margin, yPosition, lineHeight);
                    }
                    
                    // Dokumentierer
                    String documentedBy = documentedTreatment.getApprovedByUserName();
                    if (documentedBy != null && !documentedBy.isBlank()) {
                        // Get full name of documenting doctor
                        try {
                            UserAccount documentedByAccount = userAccountRepository.findByUsername(documentedBy).orElse(null);
                            if (documentedByAccount != null && documentedByAccount.getFullName() != null && !documentedByAccount.getFullName().isBlank()) {
                                documentedBy = documentedByAccount.getFullName();
                            }
                        } catch (Exception e) {
                            // Fallback to username if lookup fails
                        }
                        yPosition = addTextLine(contentStream, "Dokumentiert von: " + documentedBy, margin, yPosition, lineHeight);
                    }
                    
                    // Zweitprüfung
                    if (documentedTreatment.getSecondApprovalDateTime() != null) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        yPosition = addTextLine(contentStream, "Zweitprüfung am: " + dateFormatter.format(documentedTreatment.getSecondApprovalDateTime()), margin, yPosition, lineHeight);
                        
                        String secondApprovedBy = documentedTreatment.getSecondApprovedByUserName();
                        if (secondApprovedBy != null && !secondApprovedBy.isBlank()) {
                            // Get full name of second approving doctor
                            try {
                                UserAccount secondApprovedByAccount = userAccountRepository.findByUsername(secondApprovedBy).orElse(null);
                                if (secondApprovedByAccount != null && secondApprovedByAccount.getFullName() != null && !secondApprovedByAccount.getFullName().isBlank()) {
                                    secondApprovedBy = secondApprovedByAccount.getFullName();
                                }
                            } catch (Exception e) {
                                // Fallback to username if lookup fails
                            }
                            yPosition = addTextLine(contentStream, "Zweitprüfung von: " + secondApprovedBy, margin, yPosition, lineHeight);
                        }
                    } else {
                        yPosition = addTextLine(contentStream, "Zweitprüfung: Nicht durchgeführt", margin, yPosition, lineHeight);
                    }
                } else {
                    // Mehrere Behandlungen dokumentiert - zeige zusammenfassende Informationen
                    // Finde das früheste und späteste Dokumentationsdatum
                    java.util.Optional<LocalDateTime> earliestDoc = documentedTreatments.stream()
                            .map(Treatment::getApprovalDateTime)
                            .filter(dt -> dt != null)
                            .min(java.util.Comparator.naturalOrder());
                    
                    java.util.Optional<LocalDateTime> latestDoc = documentedTreatments.stream()
                            .map(Treatment::getApprovalDateTime)
                            .filter(dt -> dt != null)
                            .max(java.util.Comparator.naturalOrder());
                    
                    if (earliestDoc.isPresent()) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        yPosition = addTextLine(contentStream, "Erste Dokumentation am: " + dateFormatter.format(earliestDoc.get()), margin, yPosition, lineHeight);
                    }
                    
                    if (latestDoc.isPresent() && !latestDoc.equals(earliestDoc)) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        yPosition = addTextLine(contentStream, "Letzte Dokumentation am: " + dateFormatter.format(latestDoc.get()), margin, yPosition, lineHeight);
                    }
                    
                    // Sammle alle Dokumentierer
                    java.util.Set<String> documenters = documentedTreatments.stream()
                            .map(Treatment::getApprovedByUserName)
                            .filter(name -> name != null && !name.isBlank())
                            .collect(Collectors.toSet());
                    
                    if (!documenters.isEmpty()) {
                        // Get full names
                        java.util.List<String> documenterNames = documenters.stream()
                                .map(username -> {
                                    try {
                                        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);
                                        if (account != null && account.getFullName() != null && !account.getFullName().isBlank()) {
                                            return account.getFullName();
                                        }
                                        return username;
                                    } catch (Exception e) {
                                        return username;
                                    }
                                })
                                .collect(Collectors.toList());
                        yPosition = addTextLine(contentStream, "Dokumentiert von: " + String.join(", ", documenterNames), margin, yPosition, lineHeight);
                    }
                    
                    // Prüfe Zweitprüfung
                    long secondApprovedCount = documentedTreatments.stream()
                            .filter(t -> t.getSecondApprovalDateTime() != null)
                            .count();
                    
                    if (secondApprovedCount == documentedTreatments.size()) {
                        yPosition = addTextLine(contentStream, "Zweitprüfung: Alle Behandlungen zweitgeprüft", margin, yPosition, lineHeight);
                    } else if (secondApprovedCount > 0) {
                        yPosition = addTextLine(contentStream, "Zweitprüfung: " + secondApprovedCount + " von " + documentedTreatments.size() + " Behandlungen zweitgeprüft", margin, yPosition, lineHeight);
                    } else {
                        yPosition = addTextLine(contentStream, "Zweitprüfung: Nicht durchgeführt", margin, yPosition, lineHeight);
                    }
                }
            } else {
                yPosition = addTextLine(contentStream, "Dokumentation: Ausstehend", margin, yPosition, lineHeight);
            }
            yPosition -= 15;
            
            // Add confidentiality clause on same page if space available, otherwise new page
            if (yPosition < 150) {
                contentStream.close();
                addPageNumber(document, document.getNumberOfPages());
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                addWatermark(document, page, institution);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            } else {
                yPosition -= 10;
                // Separation line with high contrast
                contentStream.setStrokingColor(128f/255f, 128f/255f, 128f/255f); // Medium gray for subtle but visible separation
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                contentStream.setStrokingColor(0, 0, 0); // Reset to black
                yPosition -= 10;
            }
            
            yPosition = addConfidentialityClause(contentStream, location, margin, yPosition, lineHeight, headerFont, normalFont);
            
            contentStream.close();
            
            // Add page numbers to all pages
            int totalPages = document.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                addPageNumber(document, i);
            }
            
            document.save(baos);
            
            // Make PDF read-only
            byte[] pdfBytes = baos.toByteArray();
            document.close();
            
            // Create a new document with protection
            try (PDDocument protectedDocument = Loader.loadPDF(pdfBytes);
                 ByteArrayOutputStream protectedBaos = new ByteArrayOutputStream()) {
                
                // Set permissions - only allow reading and printing, no editing
                AccessPermission permission = new AccessPermission();
                permission.setCanModify(false);
                permission.setCanExtractContent(false);
                permission.setCanExtractForAccessibility(false);
                permission.setCanModifyAnnotations(false);
                permission.setCanFillInForm(false);
                permission.setCanAssembleDocument(false);
                permission.setCanPrint(true);
                
                // Apply protection
                StandardProtectionPolicy policy = new StandardProtectionPolicy("", "", permission);
                protectedDocument.protect(policy);
                
                protectedDocument.save(protectedBaos);
                protectedDocument.close();
                
                return protectedBaos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }
    
    private int addTextLine(PDPageContentStream contentStream, String text, int x, int y, int lineHeight) throws IOException {
        // Handle text that might be too long - wrap if needed
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
    
    private int addBoldTextLine(PDPageContentStream contentStream, String text, int x, int y, int lineHeight, PDType1Font font) throws IOException {
        // Handle text that might be too long
        String wrappedText = text.length() > 90 ? text.substring(0, 87) + "..." : text;
        contentStream.setFont(font, 10);
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(wrappedText);
        contentStream.endText();
        return y - lineHeight;
    }
    
    private void addWatermark(PDDocument document, PDPage page, Institution institution) throws IOException {
        try (PDPageContentStream watermarkStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            // Get page dimensions
            PDRectangle pageSize = page.getMediaBox();
            float width = pageSize.getWidth();
            float height = pageSize.getHeight();
            
            // Try to load watermark image from institution
            if (institution != null && institution.getWatermarkImage() != null && institution.getWatermarkImage().length > 0) {
                try {
                    // Load image from byte array
                    BufferedImage watermarkImage = ImageIO.read(new ByteArrayInputStream(institution.getWatermarkImage()));
                    if (watermarkImage != null) {
                        // Convert to PDF image
                        ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
                        ImageIO.write(watermarkImage, "PNG", imageBaos);
                        PDImageXObject watermarkPDImage = PDImageXObject.createFromByteArray(document, imageBaos.toByteArray(), "watermark");
                        
                        // Calculate size to fit page (maintain aspect ratio)
                        float imageWidth = watermarkImage.getWidth();
                        float imageHeight = watermarkImage.getHeight();
                        float scale = Math.min(width / imageWidth, height / imageHeight) * 0.6f; // 60% of page size
                        float scaledWidth = imageWidth * scale;
                        float scaledHeight = imageHeight * scale;
                        
                        // Center the watermark
                        float x = (width - scaledWidth) / 2;
                        float y = (height - scaledHeight) / 2;
                        
                        // Draw with transparency
                        watermarkStream.setNonStrokingColor(220f/255f, 220f/255f, 220f/255f);
                        watermarkStream.drawImage(watermarkPDImage, x, y, scaledWidth, scaledHeight);
                        watermarkStream.close();
                        return;
                    }
                } catch (Exception e) {
                    // Fallback to text watermark if image loading fails
                }
            }
            
            // Fallback: Text watermark
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String watermarkText = "";
            if (institution != null && institution.getInstitutionName() != null && !institution.getInstitutionName().isBlank()) {
                watermarkText = institution.getInstitutionName();
            }
            
            if (!watermarkText.isBlank()) {
                // Set watermark properties
                float fontSize = 20f;
                watermarkStream.setFont(font, fontSize);
                watermarkStream.setNonStrokingColor(220f/255f, 220f/255f, 220f/255f); // Light gray
                
                // Calculate text dimensions
                float textWidth = font.getStringWidth(watermarkText) / 1000 * fontSize;
                float textHeight = fontSize;
                
                // Draw watermark text without rotation (simplified)
                watermarkStream.beginText();
                watermarkStream.setFont(font, fontSize);
                watermarkStream.newLineAtOffset(width/2 - textWidth/2, height/2 - textHeight/2);
                watermarkStream.showText(watermarkText);
                watermarkStream.endText();
            }
            
            watermarkStream.close();
        }
    }
    
    private BufferedImage generateQRCode(String url, int width, int height) throws Exception {
        java.util.Map<EncodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(url, BarcodeFormat.QR_CODE, width, height, hints);
        
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }
    
    private int addConfidentialityClause(PDPageContentStream contentStream, Location location, 
            int margin, int yPosition, int lineHeight, PDType1Font headerFont, PDType1Font normalFont) throws IOException {
        
        // Compact confidentiality clause with high contrast (no background box for better readability)
        // Separation line
        contentStream.setStrokingColor(128f/255f, 128f/255f, 128f/255f); // Medium gray for subtle separation
        contentStream.setLineWidth(1f);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(margin + 495, yPosition);
        contentStream.stroke();
        contentStream.setStrokingColor(0, 0, 0); // Reset to black
        yPosition -= 10;
        
        // Header in bold with high contrast (black for maximum readability)
        contentStream.setNonStrokingColor(0, 0, 0); // Black for maximum contrast
        contentStream.setFont(headerFont, 10);
        yPosition = addTextLine(contentStream, "Vertraulichkeitserklärung", margin, yPosition, lineHeight);
        
        contentStream.setFont(normalFont, 9);
        String clause = "Dieses Dokument enthält streng vertrauliche Patientendaten. " +
            "Unbefugte Weitergabe ist gesetzlich verboten (DSGVO).";
        yPosition = addTextLine(contentStream, clause, margin, yPosition, lineHeight);
        
        if (location != null && location.getPhone() != null && !location.getPhone().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust: " + location.getPhone(), 
                margin, yPosition, lineHeight);
        } else if (location != null && location.getEmail() != null && !location.getEmail().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust: " + location.getEmail(), 
                margin, yPosition, lineHeight);
        }
        
        contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
        
        return yPosition;
    }
    
    private String formatGermanDate(java.time.LocalDate date) {
        if (date == null) {
            return "";
        }
        String[] months = {"Januar", "Februar", "März", "April", "Mai", "Juni", 
                          "Juli", "August", "September", "Oktober", "November", "Dezember"};
        int day = date.getDayOfMonth();
        int month = date.getMonthValue();
        int year = date.getYear();
        return day + ". " + months[month - 1] + " " + year;
    }
    
    private String[] wrapText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return new String[] { text };
        }
        
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() + word.length() + 1 <= maxLength) {
                if (currentLine.length() > 0) {
                    currentLine.append(" ");
                }
                currentLine.append(word);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }
    
    private void addPageNumber(PDDocument document, int pageNumber) throws IOException {
        if (pageNumber > document.getNumberOfPages()) {
            return;
        }
        
        PDPage page = document.getPage(pageNumber - 1);
        PDRectangle pageSize = page.getMediaBox();
        
        try (PDPageContentStream footerStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            footerStream.setNonStrokingColor(0, 0, 0); // Black
            footerStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
            
            String pageText = "Seite " + pageNumber;
            float textWidth = new PDType1Font(Standard14Fonts.FontName.HELVETICA).getStringWidth(pageText) / 1000 * 9;
            
            footerStream.beginText();
            footerStream.newLineAtOffset((pageSize.getWidth() - textWidth) / 2, 30);
            footerStream.showText(pageText);
            footerStream.endText();
            
            footerStream.close();
        }
    }
}
