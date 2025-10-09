package de.bbajor.pvs.base.ui.component;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;




public class CountrySelectionComboBoxTest {

    @Test
    void testLabelIsSet() {
        CountrySelectionComboBox comboBox = new CountrySelectionComboBox();
        assertEquals("Land", comboBox.getLabel());
    }

    @Test
    void testItemsContainAllISOCountries() {
        CountrySelectionComboBox comboBox = new CountrySelectionComboBox();
        List<Locale> items = comboBox.getListDataView().getItems().toList();
        String[] isoCountries = Locale.getISOCountries();
        assertEquals(isoCountries.length, items.size());
        for (String code : isoCountries) {
            assertTrue(items.contains(Locale.of("", code)));
        }
    }

    @Test
    void testCountryCodeToFlagValid() throws Exception {
        CountrySelectionComboBox comboBox = new CountrySelectionComboBox();
        Method method = CountrySelectionComboBox.class.getDeclaredMethod("countryCodeToFlag", String.class);
        method.setAccessible(true);
        String flag = (String) method.invoke(comboBox, "DE");
        // Unicode flag for Germany 🇩🇪
        assertEquals("\uD83C\uDDE9\uD83C\uDDEA", flag);
    }

    @Test
    void testCountryCodeToFlagInvalid() throws Exception {
        CountrySelectionComboBox comboBox = new CountrySelectionComboBox();
        Method method = CountrySelectionComboBox.class.getDeclaredMethod("countryCodeToFlag", String.class);
        method.setAccessible(true);
        assertEquals("", method.invoke(comboBox, (String) null));
        assertEquals("", method.invoke(comboBox, ""));
        assertEquals("", method.invoke(comboBox, "D"));
        assertEquals("", method.invoke(comboBox, "DEU"));
    }

    @Test
    void testItemLabelGenerator() {
        CountrySelectionComboBox comboBox = new CountrySelectionComboBox();
        Locale locale = Locale.of("", "FR");
        String label = comboBox.getItemLabelGenerator().apply(locale);
        assertTrue(label.contains("Frankreich")); // German for France
        assertTrue(label.contains("\uD83C\uDDEB\uD83C\uDDF7")); // Unicode flag for France 🇫🇷
    }
}