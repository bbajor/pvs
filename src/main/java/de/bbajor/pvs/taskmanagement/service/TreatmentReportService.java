package de.bbajor.pvs.taskmanagement.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Clock;
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

import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.institution.service.InstitutionService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.institution.context.InstitutionContext;

@Service
public class TreatmentReportService {

    @Autowired
    private Clock clock;
    
    @Autowired
    private LocationService locationService;
    
    public byte[] generatePdfReport(List<Treatment> treatments, SurgicalCenterTimeSlot timeSlot, 
            String treatingDoctor) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Load location data first
            Location location = locationService.getDefaultLocation();
            
            // Create first page and add watermark
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Add watermark to the page (use institution from location)
            Institution institution = location != null ? location.getInstitution() : null;
            addWatermark(document, page, institution);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            int yPosition = 750;
            int lineHeight = 20;
            int margin = 50;
            
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font smallFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            // Title with time slot information
            String titleText = "Behandlungsbericht";
            if (timeSlot != null) {
                DateTimeFormatter dateFormatter = DateAndTimeUtils.getGermanDateTimeFormatter();
                titleText += " für die Behandlungen vom " + dateFormatter.format(timeSlot.getDate());
                
                if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                    titleText += " von " + timeSlot.getStartTime() + " - " + timeSlot.getEndTime() + " Uhr";
                }
                
                if (timeSlot.getSurgicalCenter() != null && timeSlot.getSurgicalCenter().getName() != null) {
                    titleText += " am Behandlungsort " + timeSlot.getSurgicalCenter().getName();
                }
            }
            
            contentStream.setFont(titleFont, 16); // Slightly smaller to fit
            
            // Wrap title text if too long
            String[] titleLines = wrapText(titleText, 90);
            for (int i = 0; i < titleLines.length; i++) {
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(titleLines[i]);
                contentStream.endText();
                yPosition -= 22; // Line height for title
            }
            yPosition -= 5; // Extra space after title
            
            // Location Information (if available)
            if (location != null && location.getLocationName() != null && !location.getLocationName().isBlank()) {
                contentStream.setFont(headerFont, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText("Standortinformationen");
                contentStream.endText();
                yPosition -= 30;
                
                contentStream.setFont(normalFont, 10);
                yPosition = addTextLine(contentStream, "Standort: " + location.getLocationName(), margin, yPosition, lineHeight);
                
                String fullAddress = location.getFullAddress();
                if (fullAddress != null && !fullAddress.isBlank()) {
                    yPosition = addTextLine(contentStream, "Adresse: " + fullAddress, margin, yPosition, lineHeight);
                }
                
                String ownerWithTitle = location.getOwnerWithTitle();
                if (ownerWithTitle != null && !ownerWithTitle.isBlank()) {
                    yPosition = addTextLine(contentStream, "Praxisinhaber: " + ownerWithTitle, margin, yPosition, lineHeight);
                }
                
                if (location.getLanr() != null && !location.getLanr().isBlank()) {
                    yPosition = addTextLine(contentStream, "LANR: " + location.getLanr(), margin, yPosition, lineHeight);
                }
                
                if (location.getBsnr() != null && !location.getBsnr().isBlank()) {
                    yPosition = addTextLine(contentStream, "BSNR: " + location.getBsnr(), margin, yPosition, lineHeight);
                }
                
                if (location.getPhone() != null && !location.getPhone().isBlank()) {
                    yPosition = addTextLine(contentStream, "Telefon: " + location.getPhone(), margin, yPosition, lineHeight);
                }
                
                if (location.getEmail() != null && !location.getEmail().isBlank()) {
                    yPosition = addTextLine(contentStream, "E-Mail: " + location.getEmail(), margin, yPosition, lineHeight);
                }
                
                yPosition -= 10;
            }
            
            // Visual separation line
            yPosition -= 5;
            contentStream.setLineWidth(1.5f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(545, yPosition);
            contentStream.stroke();
            yPosition -= 15;
            
            // Treatment Location Information
            contentStream.setFont(headerFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Behandlungsort");
            contentStream.endText();
            yPosition -= 30;
            
            contentStream.setFont(normalFont, 10);
            
            if (timeSlot != null && timeSlot.getSurgicalCenter() != null) {
                yPosition = addTextLine(contentStream, "Einrichtung: " + timeSlot.getSurgicalCenter().getName(), 
                        margin, yPosition, lineHeight);
                if (timeSlot.getSurgicalCenter().getAddress() != null) {
                    yPosition = addTextLine(contentStream, "Adresse: " + timeSlot.getSurgicalCenter().getAddress(), 
                            margin, yPosition, lineHeight);
                }
            }
            
            if (timeSlot != null) {
                DateTimeFormatter dateFormatter = DateAndTimeUtils.getGermanDateTimeFormatter();
                yPosition = addTextLine(contentStream, "Datum: " + dateFormatter.format(timeSlot.getDate()), 
                        margin, yPosition, lineHeight);
                if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                    yPosition = addTextLine(contentStream, "Zeitraum: " + timeSlot.getStartTime() + " - " + timeSlot.getEndTime(), 
                            margin, yPosition, lineHeight);
                }
            }
            
            yPosition = addTextLine(contentStream, "Behandelnder Arzt: " + treatingDoctor, margin, yPosition, lineHeight);
            yPosition = addTextLine(contentStream, "Anzahl Behandlungen: " + treatments.size(), margin, yPosition, lineHeight);
            
            // Report creation info
            java.time.format.DateTimeFormatter formatter = DateAndTimeUtils.getGermanDateTimeFormatter();
            String creationInfo = "Bericht erstellt: " + 
                java.time.LocalDateTime.now(clock.getZone()).format(DateAndTimeUtils.getGermanDateTimeFormatter()) + 
                " von " + treatingDoctor;
            yPosition = addTextLine(contentStream, creationInfo, margin, yPosition, lineHeight);
            
            yPosition -= 20;
            
            // Treatments
            contentStream.setFont(headerFont, 12);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Behandlungsdetails");
            contentStream.endText();
            yPosition -= 30;
            
            PDType1Font patientFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            contentStream.setFont(normalFont, 10);
            
            for (Treatment treatment : treatments) {
                if (yPosition < 150) {
                    contentStream.close();
                    // Add page number to previous page
                    addPageNumber(document, document.getNumberOfPages());
                    
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    
                    // Add watermark to new page (use institution from location)
                    addWatermark(document, page, institution);
                    
                    contentStream = new PDPageContentStream(document, page);
                    yPosition = 750;
                }
                
                // Visual separation line before each patient
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(545, yPosition);
                contentStream.stroke();
                yPosition -= 15; // Increased spacing
                
                String patientInfo = treatment.getTreatmentPlan() != null && treatment.getTreatmentPlan().getPatient() != null 
                    ? treatment.getTreatmentPlan().getPatient().toString() : "-";
                
                // Patient name in bold
                yPosition = addBoldTextLine(contentStream, "Patient: " + patientInfo, margin, yPosition, lineHeight, patientFont);
                
                contentStream.setFont(normalFont, 10);
                String eye = treatment.getSideOfEye() != null ? treatment.getSideOfEye().toString() : "-";
                String eyeText = "Auge: " + eye;
                
                // Save the current Y position before drawing text
                int eyeYPosition = yPosition;
                
                // Color code based on eye - draw background first at the text position
                if (treatment.getSideOfEye() != null) {
                    if (treatment.getSideOfEye().name().equals("RIGHT")) {
                        // Right eye: light blue background (#E3F2FD)
                        contentStream.setNonStrokingColor(227f/255f, 242f/255f, 253f/255f);
                        contentStream.addRect(margin, eyeYPosition - 3, 495, lineHeight);
                        contentStream.fill();
                        contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                    } else if (treatment.getSideOfEye().name().equals("LEFT")) {
                        // Left eye: light orange background (#FFF3E0)
                        contentStream.setNonStrokingColor(255f/255f, 243f/255f, 224f/255f);
                        contentStream.addRect(margin, eyeYPosition - 3, 495, lineHeight);
                        contentStream.fill();
                        contentStream.setNonStrokingColor(0, 0, 0); // Reset to black
                    }
                }
                
                // Now draw the text on top of the colored background
                yPosition = addTextLine(contentStream, eyeText, margin, eyeYPosition, lineHeight);
                
                String medication = treatment.getMedication() != null 
                    ? treatment.getMedication().getArzneimittelbezeichnung() : "-";
                yPosition = addTextLine(contentStream, "Medikament: " + medication, margin, yPosition, lineHeight);
                
                String dosage = treatment.getDosage() != null ? treatment.getDosage() : "-";
                yPosition = addTextLine(contentStream, "Dosierung: " + dosage, margin, yPosition, lineHeight);
                
                String frequency = treatment.getFrequency() != null ? treatment.getFrequency() : "-";
                yPosition = addTextLine(contentStream, "Frequenz: " + frequency, margin, yPosition, lineHeight);
                
                // Approval status with details
                DateTimeFormatter dateFormatter = DateAndTimeUtils.getGermanDateTimeFormatter();
                String status = "Offen";
                if (treatment.getApprovalDate() != null) {
                    status = "Geprüft am " + dateFormatter.format(treatment.getApprovalDate());
                    if (treatment.getApprovedByUserName() != null) {
                        status += " von " + treatment.getApprovedByUserName();
                    }
                    if (treatment.getSecondApprovalDateTime() != null && treatment.getSecondApprovedByUserName() != null) {
                        status += " | Zweitprüfung: " + treatment.getSecondApprovedByUserName();
                    }
                }
                yPosition = addTextLine(contentStream, "Status: " + status, margin, yPosition, lineHeight);
                
                String additionalInfo = treatment.getAdditionalInfo() != null && !treatment.getAdditionalInfo().isBlank() 
                    ? treatment.getAdditionalInfo() : "-";
                yPosition = addTextLine(contentStream, "Zusätzliche Informationen: " + additionalInfo, margin, yPosition, lineHeight);
                
                yPosition -= 15; // Extra space between patients
            }
            
            // Add confidentiality clause on a new page at the end
            contentStream.close();
            // Add page number to previous page
            addPageNumber(document, document.getNumberOfPages());
            
            // Always create a new page for confidentiality clause
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            addWatermark(document, page, institution);
            contentStream = new PDPageContentStream(document, page);
            yPosition = 750;
            
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
        // Handle text that might be too long
        String wrappedText = text.length() > 90 ? text.substring(0, 87) + "..." : text;
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(wrappedText);
        contentStream.endText();
        return y - lineHeight;
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
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        
        try (PDPageContentStream watermarkStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            // Get page dimensions
            PDRectangle pageSize = page.getMediaBox();
            float width = pageSize.getWidth();
            float height = pageSize.getHeight();
            
            // Watermark text
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
    
    private int addConfidentialityClause(PDPageContentStream contentStream, Location location, 
            int margin, int yPosition, int lineHeight, PDType1Font headerFont, PDType1Font normalFont) throws IOException {
        
        // Separation line
        yPosition -= 10;
        contentStream.setLineWidth(1.5f);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(545, yPosition);
        contentStream.stroke();
        yPosition -= 20;
        
        // German Clause
        contentStream.setFont(headerFont, 11);
        yPosition = addTextLine(contentStream, "Vertraulichkeitserklärung", margin, yPosition, lineHeight);
        
        contentStream.setFont(normalFont, 9);
        yPosition = addTextLine(contentStream, 
            "Dieses Dokument enthält streng vertrauliche und personenbezogene Patientendaten.", 
            margin, yPosition, lineHeight);
        
        if (location != null && location.getPhone() != null && !location.getPhone().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust oder Fund dieses Dokuments wenden Sie sich bitte umgehend an die Praxis unter: " + location.getPhone(), 
                margin, yPosition, lineHeight);
        } else if (location != null && location.getEmail() != null && !location.getEmail().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust oder Fund dieses Dokuments wenden Sie sich bitte umgehend an: " + location.getEmail(), 
                margin, yPosition, lineHeight);
        }
        
        yPosition = addTextLine(contentStream, 
            "Die unbefugte Weitergabe, Veröffentlichung oder Vervielfältigung dieses Dokuments ist gesetzlich", 
            margin, yPosition, lineHeight);
        yPosition = addTextLine(contentStream, 
            "verboten und wird strafrechtlich verfolgt. Dieses Dokument ist gemäß Datenschutz-Grundverordnung", 
            margin, yPosition, lineHeight);
        yPosition = addTextLine(contentStream, 
            "(DSGVO) sowie dem Bundesdatenschutzgesetz zu schützen.", 
            margin, yPosition, lineHeight);
        
        yPosition -= 10;
        
        // English Clause
        contentStream.setFont(headerFont, 11);
        yPosition = addTextLine(contentStream, "Confidentiality Declaration", margin, yPosition, lineHeight);
        
        contentStream.setFont(normalFont, 9);
        yPosition = addTextLine(contentStream, 
            "This document contains strictly confidential and personally identifiable patient data.", 
            margin, yPosition, lineHeight);
        
        if (location != null && location.getPhone() != null && !location.getPhone().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "In case of loss or finding of this document, please contact the practice immediately at: " + location.getPhone(), 
                margin, yPosition, lineHeight);
        } else if (location != null && location.getEmail() != null && !location.getEmail().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "In case of loss or finding of this document, please contact immediately: " + location.getEmail(), 
                margin, yPosition, lineHeight);
        }
        
        yPosition = addTextLine(contentStream, 
            "Unauthorized disclosure, publication or reproduction of this document is prohibited by law", 
            margin, yPosition, lineHeight);
        yPosition = addTextLine(contentStream, 
            "and will be prosecuted. This document is protected in accordance with the General Data Protection", 
            margin, yPosition, lineHeight);
        yPosition = addTextLine(contentStream, 
            "Regulation (GDPR) and the Federal Data Protection Act.", 
            margin, yPosition, lineHeight);
        
        return yPosition;
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
