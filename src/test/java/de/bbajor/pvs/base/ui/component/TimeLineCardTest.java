package de.bbajor.pvs.base.ui.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.dom.Element;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;

class TimeLineCardTest {

    private TimeLineCardConfig configMock;
    private Consumer<TimeLineCardConfig> onDeleteMock;
    private Consumer<TimeLineCardConfig> onClickMock;

    @BeforeEach
    void setup() {
        configMock = mock(TimeLineCardConfig.class);
        onDeleteMock = mock(Consumer.class);
        onClickMock = mock(Consumer.class);
        
        // Setup default mocks to avoid NullPointerExceptions
        when(configMock.getTreatment()).thenReturn(null);
        when(configMock.isFirst()).thenReturn(true);
        when(configMock.getTreatmentDate()).thenReturn(LocalDate.now());
        when(configMock.getAdditionalInfo()).thenReturn("");
    }

    @Test
    void testFirstTreatmentCardDisplaysCorrectly() {
        LocalDate date = LocalDate.of(2023, 6, 1);
        when(configMock.getTreatmentDate()).thenReturn(date);
        when(configMock.isFirst()).thenReturn(true);
        when(configMock.getAdditionalInfo()).thenReturn("Info");
        when(configMock.getFirstDate()).thenReturn(date);

        // Verify mock configuration
        assertEquals("Info", configMock.getAdditionalInfo(), "Mock should return 'Info' for additionalInfo");
        assertTrue(configMock.isFirst(), "Mock should return true for isFirst");
        assertEquals(date, configMock.getTreatmentDate(), "Mock should return correct date");

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock, onClickMock);

        // Get all paragraphs from the card to check content
        boolean foundInfo = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.contains("Info"));

        assertTrue(foundInfo, "Card should contain a paragraph with 'Info' text");

        // Check that card has the expected class name for first treatment
        assertTrue(card.getClassNames().contains("start"),
                "First treatment card should have 'start' class name");
        
        // For Vaadin Card, the title is set via setTitle() and may not be accessible via getElement().getText()
        // Instead, we verify the card structure is correct by checking class names and children
        // The title component access would require Vaadin TestBench or a more complex setup
        assertFalse(card.getClassNames().contains("past") || card.getClassNames().contains("future"),
                "First treatment should not have past/future class");

        // Verify no time information is added for first treatment
        boolean hasTime = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.contains("Uhrzeit:"));
        assertFalse(hasTime, "Card should not contain time for first treatment");
    }

    @Test
    void testNonFirstTreatmentCardDisplaysDetails() {
        LocalDate date = LocalDate.of(2023, 6, 2);
        LocalTime time = LocalTime.of(10, 30);
        
        // Create proper mocks for Treatment chain
        Treatment treatmentMock = mock(Treatment.class);
        SurgicalCenterTimeSlot timeSlotMock = mock(SurgicalCenterTimeSlot.class);
        SurgicalCenter centerMock = mock(SurgicalCenter.class);
        
        when(timeSlotMock.getStartTime()).thenReturn(time);
        when(timeSlotMock.getDate()).thenReturn(date);
        when(timeSlotMock.getSurgicalCenter()).thenReturn(centerMock);
        when(centerMock.getName()).thenReturn("Raum 1");
        when(centerMock.toString()).thenReturn("Raum 1");
        when(treatmentMock.getSurgicalCenterTimeSlot()).thenReturn(timeSlotMock);
        when(treatmentMock.getSideOfEye()).thenReturn(SideOfEye.LEFT);
        
        when(configMock.getTreatmentDate()).thenReturn(date);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Zusatzinfo");
        when(configMock.getTreatment()).thenReturn(treatmentMock);

        // Verify mock configuration
        assertEquals("Zusatzinfo", configMock.getAdditionalInfo(), "Mock should return correct additional info");
        assertFalse(configMock.isFirst(), "Mock should return false for isFirst");

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock, onClickMock);

        // Check each component individually
        boolean foundInfo = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.equals("Zusatzinfo"));
        assertTrue(foundInfo, "Card should contain additional info");

        boolean foundTime = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.equals("Uhrzeit: 10:30") || text.contains("Uhrzeit: 10:30"));
        assertTrue(foundTime, "Card should contain time");

        boolean foundLocation = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.equals("Behandlungsort: Raum 1"));
        assertTrue(foundLocation, "Card should contain location");

        boolean foundEyeSide = card.getChildren()
                .filter(component -> component instanceof Paragraph)
                .map(component -> component.getElement().getText())
                .anyMatch(text -> text.contains("Auge:") && text.contains(SideOfEye.LEFT.toString()));
        assertTrue(foundEyeSide, "Card should contain eye side");
    }

    @Test
    void testDeleteButtonAppearsForFutureDateAndIsClickable() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        LocalTime time = LocalTime.of(9, 0);
        
        // Create proper mocks for Treatment chain
        Treatment treatmentMock = mock(Treatment.class);
        SurgicalCenterTimeSlot timeSlotMock = mock(SurgicalCenterTimeSlot.class);
        SurgicalCenter centerMock = mock(SurgicalCenter.class);
        
        when(timeSlotMock.getStartTime()).thenReturn(time);
        when(timeSlotMock.getDate()).thenReturn(futureDate);
        when(timeSlotMock.getSurgicalCenter()).thenReturn(centerMock);
        when(centerMock.getName()).thenReturn("Ort");
        when(centerMock.toString()).thenReturn("Ort");
        when(treatmentMock.getSurgicalCenterTimeSlot()).thenReturn(timeSlotMock);
        when(treatmentMock.getSideOfEye()).thenReturn(SideOfEye.RIGHT);
        
        when(configMock.getTreatmentDate()).thenReturn(futureDate);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Info");
        when(configMock.getTreatment()).thenReturn(treatmentMock);

        AtomicBoolean deleted = new AtomicBoolean(false);
        Consumer<TimeLineCardConfig> onDelete = cfg -> deleted.set(true);

        TimeLineCard card = new TimeLineCard(configMock, onDelete, onClickMock);

        boolean foundDeleteButton = card.getChildren()
                .anyMatch(c -> c instanceof Button && ((Button) c).getText().equals("löschen"));
        assertTrue(foundDeleteButton);

        // Simulate button click
        card.getChildren()
                .filter(c -> c instanceof Button && ((Button) c).getText().equals("löschen"))
                .findFirst()
                .ifPresent(c -> ((Button) c).click());

        assertTrue(deleted.get());
    }

    @Test
    void testNoDeleteButtonForPastDate() {
        LocalDate pastDate = LocalDate.now().minusDays(1);
        LocalTime time = LocalTime.of(8, 0);
        
        // Create proper mocks for Treatment chain
        Treatment treatmentMock = mock(Treatment.class);
        SurgicalCenterTimeSlot timeSlotMock = mock(SurgicalCenterTimeSlot.class);
        SurgicalCenter centerMock = mock(SurgicalCenter.class);
        
        when(timeSlotMock.getStartTime()).thenReturn(time);
        when(timeSlotMock.getDate()).thenReturn(pastDate);
        when(timeSlotMock.getSurgicalCenter()).thenReturn(centerMock);
        when(centerMock.getName()).thenReturn("Ort");
        when(centerMock.toString()).thenReturn("Ort");
        when(treatmentMock.getSurgicalCenterTimeSlot()).thenReturn(timeSlotMock);
        when(treatmentMock.getSideOfEye()).thenReturn(SideOfEye.RIGHT);
        
        when(configMock.getTreatmentDate()).thenReturn(pastDate);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Vergangenheit");
        when(configMock.getTreatment()).thenReturn(treatmentMock);

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock, onClickMock);

        boolean foundDeleteButton = card.getChildren()
                .anyMatch(c -> c instanceof Button && ((Button) c).getText().equals("löschen"));
        assertFalse(foundDeleteButton);
    }

    @Test
    void testNullConfigDoesNotThrow() {
        assertDoesNotThrow(() -> new TimeLineCard(null, onDeleteMock, onClickMock));
    }
}