package de.bbajor.pvs.taskmanagement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
            
            // Section header with separation line: Termindetails (professional styling)
            contentStream.setNonStrokingColor(0, 0, 0); // Black for headers
            contentStream.setFont(headerFont, 12); // Slightly larger for better hierarchy
            yPosition = addTextLine(contentStream, "Termindetails", margin, yPosition, lineHeight);
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
            
            // Termindetails content
            contentStream.setFont(normalFont, 9);
            if (timeSlot != null && timeSlot.getSurgicalCenter() != null) {
                yPosition = addTextLine(contentStream, "Behandlungsort: " + timeSlot.getSurgicalCenter().getName(), margin, yPosition, lineHeight);
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
            
            // Section header with separation line: Behandlungsdetails (professional styling)
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.setFont(headerFont, 12); // Slightly larger for better hierarchy
            yPosition = addTextLine(contentStream, "Behandlungsdetails", margin, yPosition, lineHeight);
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
            
            // Treatments - compact layout
            PDType1Font patientFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            contentStream.setFont(normalFont, 9);
            
            // Track which treatment plans we've already shown statistics for
            java.util.Set<Long> shownPlanStats = new java.util.HashSet<>();
            
            for (Treatment treatment : treatments) {
                // Check if we need new page (leave space for confidentiality clause)
                if (yPosition < 200) {
                    contentStream.close();
                    addPageNumber(document, document.getNumberOfPages());
                    
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    addWatermark(document, page, institution);
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 780;
                }
                
                // Patient information with full details
                Patient patient = treatment.getTreatmentPlan() != null ? treatment.getTreatmentPlan().getPatient() : null;
                if (patient != null) {
                    // Patient name in bold with high contrast color
                    String patientName = patient.getLastName() + ", " + patient.getFirstName();
                    contentStream.setNonStrokingColor(0f/255f, 51f/255f, 153f/255f); // Dark blue, high contrast
                    yPosition = addBoldTextLine(contentStream, patientName, margin, yPosition, lineHeight, patientFont);
                    contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                    
                    contentStream.setFont(normalFont, 9);
                    
                    // Geburtsdatum
                    if (patient.getBirth() != null) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        yPosition = addTextLine(contentStream, "Geburtsdatum: " + dateFormatter.format(patient.getBirth()), margin, yPosition, lineHeight);
                    }
                    
                    // Adresse
                    if (patient.getAddress() != null) {
                        String address = patient.getAddress().toString();
                        if (address != null && !address.isBlank()) {
                            yPosition = addTextLine(contentStream, "Adresse: " + address, margin, yPosition, lineHeight);
                        }
                    }
                    
                    // Krankenkasse
                    if (patient.getHealthInsurance() != null) {
                        String insuranceName = patient.getHealthInsurance().getBillingCarrierName();
                        if (insuranceName != null && !insuranceName.isBlank()) {
                            yPosition = addTextLine(contentStream, "Krankenkasse: " + insuranceName, margin, yPosition, lineHeight);
                        }
                    }
                    yPosition -= 5;
                } else {
                    // Fallback if patient is null
                    contentStream.setNonStrokingColor(0f/255f, 51f/255f, 153f/255f);
                    yPosition = addBoldTextLine(contentStream, "Patient: -", margin, yPosition, lineHeight, patientFont);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    contentStream.setFont(normalFont, 9);
                }
                
                // Treatment plan statistics (only once per treatment plan)
                TreatmentPlan treatmentPlan = treatment.getTreatmentPlan();
                if (treatmentPlan != null && treatmentPlan.getId() != null && !shownPlanStats.contains(treatmentPlan.getId())) {
                    shownPlanStats.add(treatmentPlan.getId());
                    yPosition -= 8;
                    contentStream.setFont(headerFont, 9);
                    contentStream.setNonStrokingColor(0, 0, 0);
                    yPosition = addTextLine(contentStream, "Behandlungsplan-Statistik", margin, yPosition, lineHeight);
                    contentStream.setFont(normalFont, 9);
                    
                    // Get all treatments for this plan, sorted by date
                    List<Treatment> allPlanTreatments = treatmentRepository
                            .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(treatmentPlan.getId());
                    
                    // Count treatments per eye
                    long leftEyeCount = allPlanTreatments.stream()
                            .filter(t -> t.getSideOfEye() == SideOfEye.LEFT)
                            .count();
                    long rightEyeCount = allPlanTreatments.stream()
                            .filter(t -> t.getSideOfEye() == SideOfEye.RIGHT)
                            .count();
                    
                    yPosition = addTextLine(contentStream, 
                            "Behandlungen: " + leftEyeCount + "x linkes Auge, " + rightEyeCount + "x rechtes Auge", 
                            margin + 10, yPosition, lineHeight);
                    
                    // Start date of treatment plan
                    LocalDate planStartDate = treatmentPlan.getCreationDate();
                    if (planStartDate == null && !allPlanTreatments.isEmpty()) {
                        // Fallback: use first treatment date
                        planStartDate = allPlanTreatments.stream()
                                .filter(t -> t.getDate() != null)
                                .map(Treatment::getDate)
                                .min(LocalDate::compareTo)
                                .orElse(null);
                    }
                    if (planStartDate != null) {
                        String germanStartDate = formatGermanDate(planStartDate);
                        yPosition = addTextLine(contentStream, 
                                "Behandlungsplan gestartet: " + germanStartDate, 
                                margin + 10, yPosition, lineHeight);
                    }
                    
                    // Current treatment date
                    LocalDate currentTreatmentDate = treatment.getDate();
                    if (currentTreatmentDate != null) {
                        // Find previous treatment (before current date)
                        Treatment previousTreatment = allPlanTreatments.stream()
                                .filter(t -> t.getDate() != null && t.getDate().isBefore(currentTreatmentDate))
                                .max((t1, t2) -> t1.getDate().compareTo(t2.getDate()))
                                .orElse(null);
                        
                        if (previousTreatment != null && previousTreatment.getDate() != null) {
                            long weeksBetween = java.time.temporal.ChronoUnit.WEEKS.between(previousTreatment.getDate(), currentTreatmentDate);
                            String previousDate = formatGermanDate(previousTreatment.getDate());
                            yPosition = addTextLine(contentStream, 
                                    "Letztes Behandlungsintervall: " + weeksBetween + " Wochen (von " + previousDate + " bis " + formatGermanDate(currentTreatmentDate) + ")", 
                                    margin + 10, yPosition, lineHeight);
                        } else {
                            yPosition = addTextLine(contentStream, 
                                    "Letztes Behandlungsintervall: Erste Behandlung im Behandlungsplan", 
                                    margin + 10, yPosition, lineHeight);
                        }
                        
                        // Find next treatment (after current date)
                        Treatment nextTreatment = allPlanTreatments.stream()
                                .filter(t -> t.getDate() != null && t.getDate().isAfter(currentTreatmentDate))
                                .min((t1, t2) -> t1.getDate().compareTo(t2.getDate()))
                                .orElse(null);
                        
                        if (nextTreatment != null && nextTreatment.getDate() != null) {
                            long weeksUntilNext = java.time.temporal.ChronoUnit.WEEKS.between(currentTreatmentDate, nextTreatment.getDate());
                            String nextDate = formatGermanDate(nextTreatment.getDate());
                            if (nextTreatment.getDate().isAfter(LocalDate.now())) {
                                yPosition = addTextLine(contentStream, 
                                        "Geplantes Behandlungsintervall: " + weeksUntilNext + " Wochen (bis " + nextDate + ")", 
                                        margin + 10, yPosition, lineHeight);
                            } else {
                                yPosition = addTextLine(contentStream, 
                                        "Nachfolgendes Behandlungsintervall: " + weeksUntilNext + " Wochen (bis " + nextDate + ")", 
                                        margin + 10, yPosition, lineHeight);
                            }
                        }
                    }
                    
                    yPosition -= 5;
                }
                
                // Compact two-column layout for treatment details
                int detailLeft = margin + 10;
                int detailRight = 280;
                int detailY = yPosition;
                
                String eye = treatment.getSideOfEye() != null ? treatment.getSideOfEye().toString() : "-";
                String medication = treatment.getMedication() != null 
                    ? treatment.getMedication().getArzneimittelbezeichnung() : "-";
                String dosage = treatment.getDosage() != null ? treatment.getDosage() : "-";
                String frequency = treatment.getFrequency() != null ? treatment.getFrequency() : "-";
                
                // Left column
                yPosition = addTextLine(contentStream, "Auge: " + eye, detailLeft, detailY, lineHeight);
                yPosition = addTextLine(contentStream, "Medikament: " + medication, detailLeft, yPosition, lineHeight);
                
                // Right column
                detailY = yPosition - (lineHeight * 2);
                yPosition = addTextLine(contentStream, "Dosierung: " + dosage, detailRight, detailY, lineHeight);
                yPosition = addTextLine(contentStream, "Frequenz: " + frequency, detailRight, yPosition, lineHeight);
                
                // Approval info with high contrast colors (WCAG AA compliant)
                String status;
                if (treatment.getApprovalDate() != null) {
                    if (treatment.getApprovalDateTime() != null) {
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                        status = "Geprüft am: " + dateFormatter.format(treatment.getApprovalDateTime());
                    } else {
                        // Fallback: use approvalDate if approvalDateTime is null
                        DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                        status = "Geprüft am: " + dateFormatter.format(treatment.getApprovalDate());
                    }
                    if (treatment.getApprovedByUserName() != null) {
                        status += " von " + treatment.getApprovedByUserName();
                    }
                    // Dark green for approved status (high contrast, avoids red-green confusion)
                    contentStream.setNonStrokingColor(0f/255f, 102f/255f, 51f/255f); // Dark green, high contrast
                } else {
                    // Dark orange for pending status (high contrast)
                    status = "Nicht genehmigt - Vorläufiger Bericht";
                    contentStream.setNonStrokingColor(204f/255f, 85f/255f, 0f/255f); // Dark orange, high contrast
                }
                yPosition = Math.max(yPosition, detailY) + lineHeight;
                yPosition = addTextLine(contentStream, status, detailLeft, yPosition, lineHeight);
                contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                
                // Additional info with high contrast text (no background box)
                String additionalInfo = treatment.getAdditionalInfo() != null && !treatment.getAdditionalInfo().isBlank() 
                    ? treatment.getAdditionalInfo() : null;
                if (additionalInfo != null) {
                    // Text in bold with high contrast color
                    contentStream.setFont(headerFont, 9);
                    contentStream.setNonStrokingColor(0, 0, 0); // Black for maximum contrast
                    yPosition = addTextLine(contentStream, "Bemerkungen: " + additionalInfo, detailLeft, yPosition, lineHeight);
                    contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                    contentStream.setFont(normalFont, 9);
                }
                
                yPosition -= 10; // Space between patients
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
            
            // Prüfungsinformationen
            contentStream.setFont(normalFont, 9);
            Treatment firstTreatment = treatments.stream().findFirst().orElse(null);
            if (firstTreatment != null && firstTreatment.getApprovalDate() != null) {
                // Find approving doctor from treatments
                String approvingDoctor = treatments.stream()
                        .filter(t -> t.getApprovedByUserName() != null)
                        .map(Treatment::getApprovedByUserName)
                        .findFirst()
                        .orElse("-");
                
                // Get full name of approving doctor
                if (!"-".equals(approvingDoctor)) {
                    try {
                        UserAccount approvingDoctorAccount = userAccountRepository.findByUsername(approvingDoctor).orElse(null);
                        if (approvingDoctorAccount != null && approvingDoctorAccount.getFullName() != null && !approvingDoctorAccount.getFullName().isBlank()) {
                            approvingDoctor = approvingDoctorAccount.getFullName();
                        }
                    } catch (Exception e) {
                        // Fallback to username if lookup fails
                    }
                }
                
                if (firstTreatment.getApprovalDateTime() != null) {
                    DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
                    yPosition = addTextLine(contentStream, "Geprüft am: " + dateFormatter.format(firstTreatment.getApprovalDateTime()), margin, yPosition, lineHeight);
                } else {
                    DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    yPosition = addTextLine(contentStream, "Geprüft am: " + dateFormatter.format(firstTreatment.getApprovalDate()), margin, yPosition, lineHeight);
                }
                if (!"-".equals(approvingDoctor)) {
                    yPosition = addTextLine(contentStream, "von: " + approvingDoctor, margin, yPosition, lineHeight);
                }
            } else {
                // Signal color for "Noch nicht geprüft" (red for attention)
                contentStream.setNonStrokingColor(220f/255f, 50f/255f, 47f/255f); // Signal red
                contentStream.setFont(headerFont, 10); // Slightly larger and bold for emphasis
                yPosition = addTextLine(contentStream, "Noch nicht geprüft", margin, yPosition, lineHeight);
                contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                contentStream.setFont(normalFont, 9);
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
