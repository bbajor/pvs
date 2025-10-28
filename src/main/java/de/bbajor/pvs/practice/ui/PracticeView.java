package de.bbajor.pvs.practice.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.base.util.PhoneUtils;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.practice.service.PracticeService;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("praxis")
@PageTitle("Eigene Praxisdaten")
@Menu(order = 99, icon = "vaadin:building", title = "Eigene Praxisdaten")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.DOCTOR, AppRoles.TECH_USER, AppRoles.OWNER })
public class PracticeView extends Main {

    private final PracticeService practiceService;
    private final Binder<Practice> binder = new Binder<>(Practice.class);
    
    private TextField practiceName;
    private TextField street;
    private TextField houseNumber;
    private TextField postalCode;
    private TextField city;
    private TextField countryField;
    private TextField ownerName;
    private TextField ownerTitle;
    private TextField lanr;
    private TextField bsnr;
    private TextField phone;
    private TextField fax;
    private EmailField email;
    private TextArea additionalInfo;

    public PracticeView(PracticeService practiceService) {
        this.practiceService = practiceService;
        
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, 
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        // Load existing practice or create new
        final Practice practice = practiceService.getPractice() != null 
            ? practiceService.getPractice() 
            : new Practice();

        // Create form
        VerticalLayout formLayout = createForm();
        
        // Bind data
        binder.setBean(practice);
        
        // Buttons
        Button saveButton = new Button("Speichern", e -> savePractice());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        Button resetButton = new Button("Zurücksetzen", e -> binder.readBean(practice));
        
        HorizontalLayout buttonLayout = new HorizontalLayout(saveButton, resetButton);
        
        // Add components
        VerticalLayout content = new VerticalLayout(formLayout, buttonLayout);
        content.setSpacing(true);
        
        add(new ViewToolbar("Eigene Praxisdaten"));
        add(content);
    }

    private VerticalLayout createForm() {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        
        // Practice Information Section
        FormLayout practiceLayout = new FormLayout();
        
        practiceName = new TextField("Praxisname");
        practiceName.setWidthFull();
        practiceLayout.add(practiceName, 2);
        
        street = new TextField("Straße");
        street.setWidthFull();
        houseNumber = new TextField("Hausnummer");
        houseNumber.setWidthFull();
        practiceLayout.add(street, houseNumber);
        
        postalCode = new TextField("Postleitzahl");
        postalCode.setWidthFull();
        city = new TextField("Stadt");
        city.setWidthFull();
        practiceLayout.add(postalCode, city);
        
        countryField = new TextField("Land");
        countryField.setWidthFull();
        practiceLayout.add(countryField, 2);
        
        phone = new TextField("Telefon");
        phone.setWidthFull();
        phone.setPrefixComponent(new Button(new Icon(VaadinIcon.PHONE), e -> {
            if (phone.getValue() != null && !phone.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("tel:" + phone.getValue(), "_self");
            }
        }));
        
        fax = new TextField("Fax");
        fax.setWidthFull();
        fax.setPrefixComponent(new Icon(VaadinIcon.PRINT));
        practiceLayout.add(phone, fax);
        
        email = new EmailField("E-Mail");
        email.setWidthFull();
        Button emailButton = new Button(new Icon(VaadinIcon.ENVELOPE), e -> {
            if (email.getValue() != null && !email.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("mailto:" + email.getValue(), "_self");
            }
        });
        email.setPrefixComponent(emailButton);
        practiceLayout.add(email, 2);
        
        // Bind fields with validation
        binder.forField(practiceName).asRequired("Praxisname ist erforderlich")
                .withNullRepresentation("")
                .bind(Practice::getPracticeName, Practice::setPracticeName);
        
        binder.forField(street).withNullRepresentation("")
                .bind(Practice::getStreet, Practice::setStreet);
        
        binder.forField(houseNumber).withNullRepresentation("")
                .bind(Practice::getHouseNumber, Practice::setHouseNumber);
        
        binder.forField(postalCode).withNullRepresentation("")
                .withValidator(item -> item == null || item.trim().isEmpty() || 
                    (item.trim().matches("\\d{5}")), "Postleitzahl muss 5-stellig sein")
                .bind(Practice::getPostalCode, Practice::setPostalCode);
        
        binder.forField(city).withNullRepresentation("")
                .bind(Practice::getCity, Practice::setCity);
        
        binder.forField(countryField).withNullRepresentation("")
                .bind(Practice::getCountry, Practice::setCountry);
        
        // Phone validation
        binder.forField(phone)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true;
                    }
                    return item.trim().length() <= 50;
                }, "Bitte geben Sie eine gültige Telefonnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true;
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .bind(Practice::getPhone, Practice::setPhone);
        
        // Fax validation
        binder.forField(fax)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true;
                    }
                    return item.trim().length() <= 50;
                }, "Bitte geben Sie eine gültige Faxnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true;
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Faxnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .bind(Practice::getFax, Practice::setFax);
        
        // Email validation
        binder.forField(email)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true;
                    }
                    return item.trim().length() <= 100 && item.contains("@") && item.contains(".");
                }, "Bitte geben Sie eine gültige E-Mail-Adresse ein (max. 100 Zeichen)")
                .bind(Practice::getEmail, Practice::setEmail);
        
        layout.add(practiceLayout);
        
        // Owner Information Section
        FormLayout ownerLayout = new FormLayout();
        ownerLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        
        ownerTitle = new TextField("Titel");
        ownerTitle.setWidthFull();
        ownerTitle.setHelperText("z.B. Dr. med.");
        
        ownerName = new TextField("Name Praxisinhaber");
        ownerName.setWidthFull();
        
        ownerLayout.add(ownerTitle, ownerName);
        
        binder.bind(ownerTitle, Practice::getOwnerTitle, Practice::setOwnerTitle);
        binder.bind(ownerName, Practice::getOwnerName, Practice::setOwnerName);
        
        layout.add(ownerLayout);
        
        // Healthcare Identifiers Section
        FormLayout identifierLayout = new FormLayout();
        identifierLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        
        lanr = new TextField("LANR");
        lanr.setWidthFull();
        lanr.setHelperText("Lebenslange Arztnummer (9-stellig)");
        lanr.setPrefixComponent(new Icon(VaadinIcon.USER));
        
        bsnr = new TextField("BSNR");
        bsnr.setWidthFull();
        bsnr.setHelperText("Betriebsstättennummer (9-stellig)");
        bsnr.setPrefixComponent(new Icon(VaadinIcon.BUILDING));
        
        identifierLayout.add(lanr, bsnr);
        
        // LANR validation: 9-stellige Nummer
        binder.forField(lanr)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // LANR ist optional
                    }
                    String clean = item.trim().replaceAll("\\s", ""); // Entferne Leerzeichen
                    return clean.matches("\\d{9}");
                }, "LANR muss eine 9-stellige Nummer sein")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return rawValue.trim().replaceAll("\\s", "");
                        },
                        formattedValue -> formattedValue)
                .bind(Practice::getLanr, Practice::setLanr);
        
        // BSNR validation: 9-stellige Nummer
        binder.forField(bsnr)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // BSNR ist optional
                    }
                    String clean = item.trim().replaceAll("\\s", ""); // Entferne Leerzeichen
                    return clean.matches("\\d{9}");
                }, "BSNR muss eine 9-stellige Nummer sein")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return rawValue.trim().replaceAll("\\s", "");
                        },
                        formattedValue -> formattedValue)
                .bind(Practice::getBsnr, Practice::setBsnr);
        
        layout.add(identifierLayout);
        
        // Additional Information
        additionalInfo = new TextArea("Zusätzliche Informationen");
        additionalInfo.setWidthFull();
        additionalInfo.setMaxHeight("150px");
        
        binder.bind(additionalInfo, Practice::getAdditionalInfo, Practice::setAdditionalInfo);
        
        layout.add(additionalInfo);
        
        return layout;
    }

    private void savePractice() {
        try {
            if (binder.writeBeanIfValid(binder.getBean())) {
                practiceService.savePractice(binder.getBean());
                Notification.show("Praxisdaten erfolgreich gespeichert", 3000, 
                        Notification.Position.MIDDLE);
            }
        } catch (Exception e) {
            Notification.show("Fehler beim Speichern: " + e.getMessage(), 5000, 
                    Notification.Position.MIDDLE);
        }
    }
}

