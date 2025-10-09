package de.bbajor.pvs.base.ui.component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

class TimeLineCardTest {

    private TimeLineCardConfig configMock;
    private Consumer<TimeLineCardConfig> onDeleteMock;

    @BeforeEach
    void setup() {
        configMock = mock(TimeLineCardConfig.class);
        onDeleteMock = mock(Consumer.class);
    }

    @Test
    void testFirstTreatmentCardDisplaysCorrectly() {
        LocalDate date = LocalDate.of(2023, 6, 1);
        when(configMock.getTreatmentDate()).thenReturn(date);
        when(configMock.isFirst()).thenReturn(true);
        when(configMock.getAdditionalInfo()).thenReturn("Info");

        // Verify mock configuration
        assertEquals("Info", configMock.getAdditionalInfo(), "Mock should return 'Info' for additionalInfo");
        assertTrue(configMock.isFirst(), "Mock should return true for isFirst");
        assertEquals(date, configMock.getTreatmentDate(), "Mock should return correct date");

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock);
        
        // Get all paragraphs from the card to check content
        boolean foundInfo = card.getChildren()
            .filter(component -> component instanceof Paragraph)
            .map(component -> component.getElement().getText())
            .anyMatch(text -> text.contains("Info"));
            
        assertTrue(foundInfo, "Card should contain a paragraph with 'Info' text");
        
        Element title = card.getElement().getChild(0);
        assertTrue(title.getText().contains("Start der Behandlung: ") || title.getText().contains("Behandlung am: "),
            "Title should contain correct treatment text");
            
        assertFalse(card.getElement().getText().contains("Uhrzeit:"),
            "Card should not contain time for first treatment");
    }

    @Test
    void testNonFirstTreatmentCardDisplaysDetails() {
        LocalDate date = LocalDate.of(2023, 6, 2);
        LocalTime time = LocalTime.of(10, 30);
        when(configMock.getTreatmentDate()).thenReturn(date);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Zusatzinfo");
        when(configMock.getStartTime()).thenReturn(time);
        when(configMock.getLocationInfo()).thenReturn("Raum 1");
        when(configMock.getSideOfEye()).thenReturn(SideOfEye.LEFT);

        // Verify mock configuration
        assertEquals("Zusatzinfo", configMock.getAdditionalInfo(), "Mock should return correct additional info");
        assertFalse(configMock.isFirst(), "Mock should return false for isFirst");
        assertEquals(time, configMock.getStartTime(), "Mock should return correct time");
        assertEquals("Raum 1", configMock.getLocationInfo(), "Mock should return correct location");
        assertEquals(SideOfEye.LEFT, configMock.getSideOfEye(), "Mock should return correct eye side");

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock);
       
        // Check each component individually
        boolean foundInfo = card.getChildren()
            .filter(component -> component instanceof Paragraph)
            .map(component -> component.getElement().getText())
            .anyMatch(text -> text.equals("Zusatzinfo"));
        assertTrue(foundInfo, "Card should contain additional info");

        boolean foundTime = card.getChildren()
            .filter(component -> component instanceof Paragraph)
            .map(component -> component.getElement().getText())
            .anyMatch(text -> text.equals("Uhrzeit: 10:30"));
        assertTrue(foundTime, "Card should contain time");

        boolean foundLocation = card.getChildren()
            .filter(component -> component instanceof Paragraph)
            .map(component -> component.getElement().getText())
            .anyMatch(text -> text.equals("Ort: Raum 1"));
        assertTrue(foundLocation, "Card should contain location");

        boolean foundEyeSide = card.getChildren()
            .filter(component -> component instanceof Paragraph)
            .map(component -> component.getElement().getText())
            .anyMatch(text -> text.equals(SideOfEye.LEFT.toString()));
        assertTrue(foundEyeSide, "Card should contain eye side");
    }

    @Test
    void testDeleteButtonAppearsForFutureDateAndIsClickable() {
        LocalDate futureDate = LocalDate.now().plusDays(2);
        when(configMock.getTreatmentDate()).thenReturn(futureDate);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Info");
        when(configMock.getStartTime()).thenReturn(LocalTime.of(9, 0));
        when(configMock.getLocationInfo()).thenReturn("Ort");
        when(configMock.getSideOfEye()).thenReturn(SideOfEye.RIGHT);

        AtomicBoolean deleted = new AtomicBoolean(false);
        Consumer<TimeLineCardConfig> onDelete = cfg -> deleted.set(true);

        TimeLineCard card = new TimeLineCard(configMock, onDelete);

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
        when(configMock.getTreatmentDate()).thenReturn(pastDate);
        when(configMock.isFirst()).thenReturn(false);
        when(configMock.getAdditionalInfo()).thenReturn("Vergangenheit");
        when(configMock.getStartTime()).thenReturn(LocalTime.of(8, 0));
        when(configMock.getLocationInfo()).thenReturn("Ort");
        when(configMock.getSideOfEye()).thenReturn(SideOfEye.RIGHT);

        TimeLineCard card = new TimeLineCard(configMock, onDeleteMock);

        boolean foundDeleteButton = card.getChildren()
                .anyMatch(c -> c instanceof Button && ((Button) c).getText().equals("löschen"));
        assertFalse(foundDeleteButton);
    }

    @Test
    void testNullConfigDoesNotThrow() {
        assertDoesNotThrow(() -> new TimeLineCard(null, onDeleteMock));
    }
}