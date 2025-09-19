package de.bbajor.pvs.base.ui.component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

public class CountrySelectionComboBox extends ComboBox<Locale> {

    public CountrySelectionComboBox() {
        setLabel("Land");
        List<Locale> countries = Arrays.stream(Locale.getISOCountries())
                .map(code -> Locale.of("", code))
                .toList();

        setItems(countries);
        setRenderer(new ComponentRenderer<>(locale -> {
            String flag = countryCodeToFlag(locale.getCountry());
            String name = locale.getDisplayCountry(Locale.GERMAN);
            return new Span(flag + " " + name);
        }));
        setItemLabelGenerator(locale -> countryCodeToFlag(locale.getCountry()) + " " +
                locale.getDisplayCountry(Locale.GERMAN));
    }

    private String countryCodeToFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) {
            return "";
        }
        int firstChar = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6;
        int secondChar = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6;
        return new String(Character.toChars(firstChar)) + new String(Character.toChars(secondChar));
    }
}
