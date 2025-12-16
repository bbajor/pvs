package de.bbajor.pvs.taskmanagement.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

@Service
public class TimeSlotReportService {

    @Autowired
    private LocationService locationService;
    
    @Autowired
    private InstitutionRepository institutionRepository;
    
    public byte[] generateTimeSlotReport(SurgicalCenterTimeSlot timeSlot, List<Treatment> treatments) {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            // Lade Standort- und Einrichtungsdaten
            Location location = locationService.getDefaultLocation();
            Institution institution = location != null ? location.getInstitution() : null;
            
            // Lade vollständige Einrichtungsdaten (inkl. Watermark und Website-URL)
            if (institution != null && institution.getId() != null) {
                institution = institutionRepository.findById(institution.getId()).orElse(institution);
            }
            
            // Erstelle erste Seite und füge Watermark hinzu
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            
            // Füge Watermark zur Seite hinzu
            addWatermark(document, page, institution);
            
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            
            int yPosition = 780;
            int lineHeight = 16;
            int margin = 50;
            int rightMargin = 545;
            int sectionSpacing = 20;
            
            PDType1Font titleFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font headerFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font normalFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            
            // Titel - prüfe ob zukünftig oder vergangen
            String titleText;
            if (timeSlot != null && timeSlot.getDate() != null) {
                LocalDate slotDate = timeSlot.getDate();
                LocalDate today = LocalDate.now();
                LocalTime slotStartTime = timeSlot.getStartTime();
                LocalTime now = LocalTime.now();
                
                if (slotDate.isAfter(today) || 
                    (slotDate.isEqual(today) && slotStartTime != null && slotStartTime.isAfter(now))) {
                    // Zukünftige Behandlung
                    String germanDate = formatGermanDate(slotDate);
                    titleText = "Geplante Behandlungen am " + germanDate;
                } else {
                    // Vergangene oder aktuell laufende Behandlung
                    String germanDate = formatGermanDate(slotDate);
                    titleText = "Behandlungen vom " + germanDate;
                }
            } else {
                titleText = "Behandlungen";
            }
            
            contentStream.setNonStrokingColor(0f/255f, 51f/255f, 153f/255f);
            contentStream.setFont(titleFont, 20);
            contentStream.beginText();
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(titleText);
            contentStream.endText();
            contentStream.setNonStrokingColor(0, 0, 0);
            yPosition -= 30;
            
            // Header-Bereich mit zwei Spalten
            contentStream.setFont(normalFont, 9);
            int leftCol = margin;
            int startY = yPosition;
            
            // Linke Spalte: Einrichtung (Institution)
            if (institution != null) {
                contentStream.setFont(headerFont, 10);
                yPosition = addTextLine(contentStream, "Einrichtung", leftCol, yPosition, lineHeight);
                contentStream.setFont(normalFont, 9);
                if (institution.getInstitutionName() != null && !institution.getInstitutionName().isBlank()) {
                    yPosition = addTextLine(contentStream, institution.getInstitutionName(), leftCol, yPosition, lineHeight);
                }
                String institutionAddress = institution.getFullAddress();
                if (institutionAddress != null && !institutionAddress.isBlank()) {
                    yPosition = addTextLine(contentStream, institutionAddress, leftCol, yPosition, lineHeight);
                }
                if (institution.getPhone() != null && !institution.getPhone().isBlank()) {
                    yPosition = addTextLine(contentStream, "Tel: " + institution.getPhone(), leftCol, yPosition, lineHeight);
                }
                if (institution.getEmail() != null && !institution.getEmail().isBlank()) {
                    yPosition = addTextLine(contentStream, institution.getEmail(), leftCol, yPosition, lineHeight);
                }
            }
            
            // Rechte Spalte: Behandlungsort und Termin
            int rightCol = 300;
            yPosition = startY;
            if (timeSlot != null && timeSlot.getSurgicalCenter() != null) {
                contentStream.setFont(headerFont, 10);
                yPosition = addTextLine(contentStream, "Behandlungsort", rightCol, yPosition, lineHeight);
                contentStream.setFont(normalFont, 9);
                String centerName = timeSlot.getSurgicalCenter().getName();
                if (centerName != null && !centerName.isBlank()) {
                    yPosition = addTextLine(contentStream, centerName, rightCol, yPosition, lineHeight);
                }
                if (timeSlot.getSurgicalCenter().getAddress() != null) {
                    String centerAddress = timeSlot.getSurgicalCenter().getAddress().toString();
                    if (centerAddress != null && !centerAddress.isBlank()) {
                        yPosition = addTextLine(contentStream, centerAddress, rightCol, yPosition, lineHeight);
                    }
                }
            }
            
            // Termin-Informationen
            yPosition -= 10;
            if (timeSlot != null) {
                contentStream.setFont(headerFont, 10);
                yPosition = addTextLine(contentStream, "Termin", rightCol, yPosition, lineHeight);
                contentStream.setFont(normalFont, 9);
                if (timeSlot.getDate() != null) {
                    DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    yPosition = addTextLine(contentStream, "Datum: " + dateFormatter.format(timeSlot.getDate()), rightCol, yPosition, lineHeight);
                }
                if (timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
                    String timeRange = String.format("%02d:%02d - %02d:%02d Uhr", 
                        timeSlot.getStartTime().getHour(), timeSlot.getStartTime().getMinute(),
                        timeSlot.getEndTime().getHour(), timeSlot.getEndTime().getMinute());
                    yPosition = addTextLine(contentStream, "Zeit: " + timeRange, rightCol, yPosition, lineHeight);
                }
                if (timeSlot.getDescription() != null && !timeSlot.getDescription().isBlank()) {
                    yPosition = addTextLine(contentStream, "Beschreibung: " + timeSlot.getDescription(), rightCol, yPosition, lineHeight);
                }
            }
            
            yPosition -= sectionSpacing;
            
            // Patientenliste
            contentStream.setFont(headerFont, 12);
            yPosition = addTextLine(contentStream, "Zu behandelnde Patienten", margin, yPosition, lineHeight);
            yPosition -= 6;
            
            // Trennlinie
            contentStream.setStrokingColor(100f/255f, 100f/255f, 100f/255f);
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
            
            // Patientenliste immer einspaltig über mehrere Seiten
            int patientNumber = 1;
            int colNr = margin;
            int colName = margin + 25;
            int colVorname = margin + 90;
            int colGeburtsdatum = margin + 165;
            int colVersicherung = margin + 245;
            int colAuge = margin + 320;
            // Medikament und Zusatzinfos in zweiter Zeile
            
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
            
            // Zweite Zeile des Headers für Medikament und Zusatzinfos
            headerY -= lineHeight;
            contentStream.setFont(headerFont, 9);
            addTextLine(contentStream, "Medikament", colName, headerY, lineHeight);
            addTextLine(contentStream, "Zusatzinfos", colName + 150, headerY, lineHeight);
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
            
            int rowIndex = 0; // Zähler für Zebra-Striping
            
            for (Treatment treatment : sortedTreatments) {
                // Prüfe ob neue Seite nötig (mit Platz für Footer)
                // Jeder Patient benötigt 2 Zeilen (Hauptzeile + Zusatzinfos)
                int patientHeight = lineHeight * 2 + 2; // 2 Zeilen + Abstand
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
                    headerY = yPosition;
                    addTextLine(contentStream, "Nr.", colNr, headerY, lineHeight);
                    addTextLine(contentStream, "Name", colName, headerY, lineHeight);
                    addTextLine(contentStream, "Vorname", colVorname, headerY, lineHeight);
                    addTextLine(contentStream, "Geburtsdatum", colGeburtsdatum, headerY, lineHeight);
                    addTextLine(contentStream, "Versicherung", colVersicherung, headerY, lineHeight);
                    addTextLine(contentStream, "Auge", colAuge, headerY, lineHeight);
                    
                    // Zweite Zeile des Headers für Medikament und Zusatzinfos
                    headerY -= lineHeight;
                    contentStream.setFont(headerFont, 9);
                    addTextLine(contentStream, "Medikament", colName, headerY, lineHeight);
                    addTextLine(contentStream, "Zusatzinfos", colName + 150, headerY, lineHeight);
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
                    
                    // Zeilennummer zurücksetzen bei neuer Seite
                    rowIndex = 0;
                }
                
                Patient patient = treatment.getTreatmentPlan() != null ? treatment.getTreatmentPlan().getPatient() : null;
                int currentRowY = yPosition;
                
                // Zebra-Striping: Jede zweite Zeile leicht grau hinterlegen
                if (rowIndex % 2 == 1) {
                    // Grauer Hintergrund für die gesamte Patientenzeile (2 Zeilen)
                    float grayValue = 0.95f; // Sehr helles Grau
                    contentStream.setNonStrokingColor(grayValue, grayValue, grayValue);
                    float rowHeight = (lineHeight * 2) + 2; // Höhe für beide Zeilen + Abstand
                    float paddingTop = 6f + 3f; // Padding oben + 3 Pixel nach oben verschieben
                    float paddingBottom = 10f; // 10 Pixel unten abschneiden
                    // Rechteck zeichnen: x, y (unten links), width, height
                    // currentRowY ist die oberste Position der ersten Zeile
                    // Rechteck 3 Pixel nach oben verschieben und unten 10 Pixel abschneiden
                    float rectTop = currentRowY + paddingTop;
                    float rectBottom = currentRowY - rowHeight + paddingBottom; // 10 Pixel weniger nach unten
                    float rectY = rectBottom;
                    float rectHeight = rectTop - rectBottom;
                    contentStream.addRect((float)margin, rectY, (float)(rightMargin - margin), rectHeight);
                    contentStream.fill();
                    contentStream.setNonStrokingColor(0, 0, 0); // Zurück zu schwarz für Text
                }
                
                // Tabellenzeile - Hauptzeile
                addTextLine(contentStream, String.valueOf(patientNumber++), colNr, currentRowY, lineHeight);
                
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
                
                // Zweite Zeile für Medikament und Zusatzinfos
                currentRowY -= lineHeight;
                String medikament = "-";
                if (treatment.getMedicationFavourite() != null && treatment.getMedicationFavourite().getMedication() != null) {
                    medikament = treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
                    if (medikament.length() > 30) medikament = medikament.substring(0, 27) + "...";
                }
                addTextLine(contentStream, "Medikament: " + medikament, colName, currentRowY, lineHeight);
                
                String zusatz = treatment.getAdditionalInfo() != null && !treatment.getAdditionalInfo().isBlank() 
                    ? treatment.getAdditionalInfo() : "-";
                if (zusatz.length() > 50) zusatz = zusatz.substring(0, 47) + "...";
                if (!zusatz.equals("-")) {
                    addTextLine(contentStream, "Zusatzinfos: " + zusatz, colName + 150, currentRowY, lineHeight);
                }
                
                yPosition = currentRowY - lineHeight - 2; // Abstand zwischen Patienten
                rowIndex++; // Zeilennummer erhöhen
            }
            
            // Vertraulichkeitsklausel - prüfe ob genug Platz vorhanden ist
            // Die Klausel benötigt etwa 80-100 Pixel Platz
            int confidentialityClauseHeight = 100;
            if (yPosition < confidentialityClauseHeight + 50) { // 50px Sicherheitsabstand
                contentStream.close();
                addPageNumber(document, document.getNumberOfPages());
                page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                addWatermark(document, page, institution);
                contentStream = new PDPageContentStream(document, page);
                yPosition = 750;
            } else {
                yPosition -= 10;
                contentStream.setStrokingColor(128f/255f, 128f/255f, 128f/255f);
                contentStream.setLineWidth(1f);
                contentStream.moveTo(margin, yPosition);
                contentStream.lineTo(rightMargin, yPosition);
                contentStream.stroke();
                contentStream.setStrokingColor(0, 0, 0);
                yPosition -= 10;
            }
            
            yPosition = addConfidentialityClause(contentStream, institution, margin, yPosition, lineHeight, headerFont, normalFont);
            
            contentStream.close();
            
            // Seitenzahlen hinzufügen
            int totalPages = document.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                addPageNumber(document, i);
            }
            
            document.save(baos);
            
            // PDF schreibgeschützt machen
            byte[] pdfBytes = baos.toByteArray();
            document.close();
            
            // Geschütztes Dokument erstellen
            try (PDDocument protectedDocument = Loader.loadPDF(pdfBytes);
                 ByteArrayOutputStream protectedBaos = new ByteArrayOutputStream()) {
                
                AccessPermission permission = new AccessPermission();
                permission.setCanModify(false);
                permission.setCanExtractContent(false);
                permission.setCanExtractForAccessibility(false);
                permission.setCanModifyAnnotations(false);
                permission.setCanFillInForm(false);
                permission.setCanAssembleDocument(false);
                permission.setCanPrint(true);
                
                StandardProtectionPolicy policy = new StandardProtectionPolicy("", "", permission);
                protectedDocument.protect(policy);
                
                protectedDocument.save(protectedBaos);
                protectedDocument.close();
                
                return protectedBaos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("Fehler beim Generieren des Zeitslot-Berichts", e);
        }
    }
    
    private int addTextLine(PDPageContentStream contentStream, String text, int x, int y, int lineHeight) throws IOException {
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
    
    private void addWatermark(PDDocument document, PDPage page, Institution institution) throws IOException {
        try (PDPageContentStream watermarkStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            PDRectangle pageSize = page.getMediaBox();
            float width = pageSize.getWidth();
            float height = pageSize.getHeight();
            
            if (institution != null && institution.getWatermarkImage() != null && institution.getWatermarkImage().length > 0) {
                try {
                    BufferedImage watermarkImage = ImageIO.read(new ByteArrayInputStream(institution.getWatermarkImage()));
                    if (watermarkImage != null) {
                        ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
                        ImageIO.write(watermarkImage, "PNG", imageBaos);
                        PDImageXObject watermarkPDImage = PDImageXObject.createFromByteArray(document, imageBaos.toByteArray(), "watermark");
                        
                        float imageWidth = watermarkImage.getWidth();
                        float imageHeight = watermarkImage.getHeight();
                        float scale = Math.min(width / imageWidth, height / imageHeight) * 0.6f;
                        float scaledWidth = imageWidth * scale;
                        float scaledHeight = imageHeight * scale;
                        
                        float x = (width - scaledWidth) / 2;
                        float y = (height - scaledHeight) / 2;
                        
                        watermarkStream.setNonStrokingColor(220f/255f, 220f/255f, 220f/255f);
                        watermarkStream.drawImage(watermarkPDImage, x, y, scaledWidth, scaledHeight);
                        watermarkStream.close();
                        return;
                    }
                } catch (Exception e) {
                    // Fallback to text watermark
                }
            }
            
            // Fallback: Text watermark
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            String watermarkText = "";
            if (institution != null && institution.getInstitutionName() != null && !institution.getInstitutionName().isBlank()) {
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
    
    private int addConfidentialityClause(PDPageContentStream contentStream, Institution institution, 
            int margin, int yPosition, int lineHeight, PDType1Font headerFont, PDType1Font normalFont) throws IOException {
        
        contentStream.setStrokingColor(128f/255f, 128f/255f, 128f/255f);
        contentStream.setLineWidth(1f);
        contentStream.moveTo(margin, yPosition);
        contentStream.lineTo(margin + 495, yPosition);
        contentStream.stroke();
        contentStream.setStrokingColor(0, 0, 0);
        yPosition -= 10;
        
        contentStream.setNonStrokingColor(0, 0, 0);
        contentStream.setFont(headerFont, 10);
        yPosition = addTextLine(contentStream, "Vertraulichkeitserklärung", margin, yPosition, lineHeight);
        
        contentStream.setFont(normalFont, 9);
        String clause = "Dieses Dokument enthält streng vertrauliche Patientendaten. " +
            "Unbefugte Weitergabe ist gesetzlich verboten (DSGVO).";
        yPosition = addTextLine(contentStream, clause, margin, yPosition, lineHeight);
        
        if (institution != null && institution.getPhone() != null && !institution.getPhone().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust: " + institution.getPhone(), 
                margin, yPosition, lineHeight);
        } else if (institution != null && institution.getEmail() != null && !institution.getEmail().isBlank()) {
            yPosition = addTextLine(contentStream, 
                "Bei Verlust: " + institution.getEmail(), 
                margin, yPosition, lineHeight);
        }
        
        contentStream.setNonStrokingColor(0, 0, 0);
        
        return yPosition;
    }
    
    private String formatGermanDate(LocalDate date) {
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
            footerStream.setNonStrokingColor(0, 0, 0);
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

