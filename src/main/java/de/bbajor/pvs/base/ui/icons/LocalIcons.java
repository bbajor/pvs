package de.bbajor.pvs.base.ui.icons;

import java.util.Locale;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.icon.IconFactory;

@JsModule("./icons/my-icons-icons.js")
public enum LocalIcons implements IconFactory {
    FLAT_COLOR_ICONS__ABOUT, FLAT_COLOR_ICONS__BAR_CHART, FLAT_COLOR_ICONS__CALCULATOR, FLAT_COLOR_ICONS__CALENDAR,
    FLAT_COLOR_ICONS__CANCEL, FLAT_COLOR_ICONS__CHECKMARK, FLAT_COLOR_ICONS__CLEAR_FILTERS,
    FLAT_COLOR_ICONS__EMPTY_FILTER, FLAT_COLOR_ICONS__END_CALL, FLAT_COLOR_ICONS__EXPORT,
    FLAT_COLOR_ICONS__FILLED_FILTER, FLAT_COLOR_ICONS__GRID, FLAT_COLOR_ICONS__LINE_CHART, FLAT_COLOR_ICONS__LINK,
    FLAT_COLOR_ICONS__MINUS, FLAT_COLOR_ICONS__OK, FLAT_COLOR_ICONS__ORGANIZATION, FLAT_COLOR_ICONS__PHONE,
    FLAT_COLOR_ICONS__PLUS, FLAT_COLOR_ICONS__TODO_LIST, HEALTHICONS__AMBULATORY_CLINIC,
    HEALTHICONS__CHART_CURED_INCREASING, HEALTHICONS__DIAGNOSTICS, HEALTHICONS__DOCTOR_OUTLINE, HEALTHICONS__ICD_10,
    HEALTHICONS__ICD, HEALTHICONS__INFO, HEALTHICONS__MEDICINES, HEALTHICONS__NO, HEALTHICONS__PHONE,
    HEALTHICONS__SEXUAL_REPRODUCTIVE_HEALTH_OUTLINE, HEALTHICONS__SYRINGE, ICON_PARK__SAVE_ONE, LOCAL_ICONS;

    public Icon create() {
        return new Icon(this.name().toLowerCase(Locale.ENGLISH).replace('_', '-').replaceAll("^-", ""));
    }

    public static final class Icon extends com.vaadin.flow.component.icon.Icon {
        Icon(String icon) {
            super("my-icons-icons", icon);
        }
    }
}