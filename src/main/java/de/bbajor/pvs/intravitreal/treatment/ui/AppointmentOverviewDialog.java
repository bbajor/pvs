package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

public class AppointmentOverviewDialog extends Dialog {

    private static final Logger log = LoggerFactory.getLogger(AppointmentOverviewDialog.class);

    private final TreatmentRepository treatmentRepository;
    private final SurgicalCenterTimeSlot timeSlot;

    public AppointmentOverviewDialog(SurgicalCenterTimeSlot timeSlot, TreatmentRepository treatmentRepository) {
        this.timeSlot = timeSlot;
        this.treatmentRepository = treatmentRepository;

        setWidth("1000px");
        setHeight("700px");
        setModal(true);
        setCloseOnOutsideClick(true);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);

        // Header mit Titel und Schließen-Button
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.getStyle().set("padding", "var(--lumo-space-m)");
        header.getStyle().set("border-bottom", "1px solid var(--lumo-contrast-20pct)");

        String dateTimeStr = (timeSlot.getDate() != null
                ? DateAndTimeUtils.getGermanDateTimeFormatter().format(timeSlot.getDate())
                : "-")
                + (timeSlot.getStartTime() != null ? " " + timeSlot.getStartTime().toString() : "");
        H4 title = new H4("Terminübersicht - " + dateTimeStr);
        title.getStyle().set("margin", "0");
        header.add(title);

        Button closeButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        header.add(closeButton);
        header.setFlexGrow(1, title);

        content.add(header);

        // Grid für Patientenliste
        Grid<PatientTreatmentInfo> grid = new Grid<>();
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.NONE);

        // Lade alle Treatments für diesen Zeitslot
        List<Treatment> treatments = treatmentRepository.findByTimeSlotId(timeSlot.getId());

        // Sammle alle TreatmentPlan-IDs für Batch-Loading
        List<Long> planIds = treatments.stream()
                .filter(t -> t.getTreatmentPlan() != null)
                .map(t -> t.getTreatmentPlan().getId())
                .distinct()
                .collect(Collectors.toList());

        // Lade alle Treatments für alle Pläne in einem Batch (vermeidet N+1 Problem)
        java.util.Map<Long, List<Treatment>> treatmentsByPlanId = new java.util.HashMap<>();
        for (Long planId : planIds) {
            List<Treatment> planTreatments = treatmentRepository
                    .findTreatmentsByPlanIdWithTreatmentPlanAndTimeSlotOrderByDateDesc(planId);
            treatmentsByPlanId.put(planId, planTreatments);
        }

        // Konvertiere zu PatientTreatmentInfo-Objekten
        List<PatientTreatmentInfo> patientInfos = treatments.stream()
                .filter(t -> t.getTreatmentPlan() != null && t.getTreatmentPlan().getPatient() != null)
                .map(t -> {
                    PatientTreatmentInfo info = new PatientTreatmentInfo();
                    info.treatment = t;
                    info.patient = t.getTreatmentPlan().getPatient();
                    info.treatmentPlan = t.getTreatmentPlan();
                    // Verwende gecachte Treatments für Intervall-Berechnung
                    info.allPlanTreatments = treatmentsByPlanId.get(t.getTreatmentPlan().getId());
                    info.previousInterval = calculatePreviousInterval(t, info.allPlanTreatments);
                    return info;
                })
                .sorted(Comparator
                        .comparing((PatientTreatmentInfo pti) -> pti.patient.getLastName(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(pti -> pti.patient.getFirstName(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // Spalten konfigurieren
        // Patientendaten-Renderer (eine Spalte) - wie im TaskReviewDialog
        grid.addColumn(new ComponentRenderer<>(info -> {
            if (info.patient != null) {
                VerticalLayout patientLayout = new VerticalLayout();
                patientLayout.setSpacing(false);
                patientLayout.setPadding(false);
                
                String name = (info.patient.getLastName() != null ? info.patient.getLastName() : "") + 
                              (info.patient.getFirstName() != null ? ", " + info.patient.getFirstName() : "");
                if (name.startsWith(", ")) name = name.substring(2);
                if (name.isEmpty()) name = "-";
                
                Span nameSpan = new Span(name);
                nameSpan.getStyle().set("font-weight", "600");
                patientLayout.add(nameSpan);
                
                if (info.patient.getBirth() != null) {
                    Span birthSpan = new Span("geb. " + 
                        info.patient.getBirth().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")));
                    birthSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");
                    birthSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
                    patientLayout.add(birthSpan);
                }
                
                return patientLayout;
            }
            return new Span("-");
        })).setHeader("Patient").setResizable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(info -> {
            String insurance = info.treatmentPlan.getHealthInsurance() != null
                    ? info.treatmentPlan.getHealthInsurance()
                    : "-";
            Span span = new Span(insurance);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Krankenkasse").setResizable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(info -> {
            if (info.treatment.getSideOfEye() != null) {
                return new Span(info.treatment.getSideOfEye().toString());
            }
            return new Span("-");
        })).setHeader("Augenseite").setResizable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(info -> {
            if (info.treatment.getMedicationFavourite() != null
                    && info.treatment.getMedicationFavourite().getMedication() != null) {
                String medication = info.treatment.getMedicationFavourite().getMedication()
                        .getArzneimittelbezeichnung();
                Span span = new Span(medication);
                span.getStyle().set("white-space", "normal");
                span.getStyle().set("word-wrap", "break-word");
                return span;
            }
            return new Span("-");
        })).setHeader("Medikament").setResizable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(info -> {
            String additionalInfo = info.treatmentPlan.getAdditionalInformation();
            if (additionalInfo != null && !additionalInfo.trim().isEmpty()) {
                Span span = new Span(additionalInfo);
                span.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                span.getStyle().set("white-space", "normal");
                span.getStyle().set("word-wrap", "break-word");
                return span;
            }
            return new Span("-");
        })).setHeader("Zusätzliche Informationen").setResizable(true).setAutoWidth(true);

        grid.addColumn(new ComponentRenderer<>(info -> {
            if (info.previousInterval != null) {
                String intervalText = info.previousInterval + (info.previousInterval == 1 ? " Woche" : " Wochen");
                Span span = new Span(intervalText);
                span.addClassNames(LumoUtility.TextColor.SECONDARY);
                return span;
            }
            return new Span("-");
        })).setHeader("Vorheriges Intervall").setResizable(true).setAutoWidth(true);

        // Verwende ListDataProvider für in-memory Liste
        // setItems(List) erstellt automatisch einen ListDataProvider
        grid.setItems(patientInfos);
        // Keine setItemCountUnknown() nötig, da ListDataProvider die exakte Anzahl kennt
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);

        content.add(grid);
        content.setFlexGrow(1, grid);

        add(content);
    }

    /**
     * Berechnet das vorherige Intervall in Wochen für das Treatment.
     * Das Intervall wird zwischen dem vorletzten und letzten Treatment für das gleiche Auge berechnet.
     * 
     * @param currentTreatment Das aktuelle Treatment
     * @param allPlanTreatments Alle Treatments des Plans (bereits geladen, um N+1 zu vermeiden)
     */
    private Integer calculatePreviousInterval(Treatment currentTreatment, List<Treatment> allPlanTreatments) {
        if (currentTreatment == null || allPlanTreatments == null || allPlanTreatments.isEmpty()) {
            return null;
        }

        try {
            SideOfEye sideOfEye = currentTreatment.getSideOfEye();
            LocalDate currentDate = currentTreatment.getDate();

            if (sideOfEye == null || currentDate == null) {
                return null;
            }

            // Filtere nach gleichem Auge und sortiere nach Datum (absteigend)
            List<Treatment> sameEyeTreatments = allPlanTreatments.stream()
                    .filter(t -> t.getSideOfEye() == sideOfEye)
                    .filter(t -> t.getDate() != null)
                    .sorted(Comparator.comparing(Treatment::getDate).reversed())
                    .collect(Collectors.toList());

            // Finde das aktuelle Treatment und das vorherige
            int currentIndex = -1;
            for (int i = 0; i < sameEyeTreatments.size(); i++) {
                if (sameEyeTreatments.get(i).getId().equals(currentTreatment.getId())) {
                    currentIndex = i;
                    break;
                }
            }

            // Wenn wir das aktuelle Treatment gefunden haben und es ein vorheriges gibt
            if (currentIndex >= 0 && currentIndex + 1 < sameEyeTreatments.size()) {
                Treatment previous = sameEyeTreatments.get(currentIndex + 1);
                if (previous.getDate() != null) {
                    long weeks = ChronoUnit.WEEKS.between(previous.getDate(), currentDate);
                    if (weeks > 0) {
                        return (int) weeks;
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Fehler beim Berechnen des vorherigen Intervalls", ex);
        }

        return null;
    }

    /**
     * Hilfsklasse für die Anzeige der Patientendaten im Grid.
     */
    private static class PatientTreatmentInfo {
        Treatment treatment;
        de.bbajor.pvs.patient.model.Patient patient;
        de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan treatmentPlan;
        List<Treatment> allPlanTreatments; // Gecachte Treatments für Intervall-Berechnung
        Integer previousInterval;
    }
}

