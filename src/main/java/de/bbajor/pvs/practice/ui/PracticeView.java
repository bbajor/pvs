package de.bbajor.pvs.practice.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
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
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.practice.service.PracticeService;
import jakarta.annotation.security.PermitAll;

@Route("praxis")
@PageTitle("Eigene Praxisdaten")
@Menu(order = 99, icon = "vaadin:building", title = "Eigene Praxisdaten")
@PermitAll
public class PracticeView extends Main {

    private final PracticeService practiceService;
    private final Binder<Practice> binder = new Binder<>(Practice.class);
    
    private TextField practiceName;
    private TextField street;
    private TextField houseNumber;
    private TextField postalCode;
    private TextField city;
    private TextField country;
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
        
        TextField country = new TextField("Land");
        country.setWidthFull();
        practiceLayout.add(country, 2);
        
        phone = new TextField("Telefon");
        phone.setWidthFull();
        fax = new TextField("Fax");
        fax.setWidthFull();
        practiceLayout.add(phone, fax);
        
        email = new EmailField("E-Mail");
        email.setWidthFull();
        practiceLayout.add(email, 2);
        
        // Bind fields
        binder.bind(practiceName, Practice::getPracticeName, Practice::setPracticeName);
        binder.bind(street, Practice::getStreet, Practice::setStreet);
        binder.bind(houseNumber, Practice::getHouseNumber, Practice::setHouseNumber);
        binder.bind(postalCode, Practice::getPostalCode, Practice::setPostalCode);
        binder.bind(city, Practice::getCity, Practice::setCity);
        binder.bind(country, Practice::getCountry, Practice::setCountry);
        binder.bind(phone, Practice::getPhone, Practice::setPhone);
        binder.bind(fax, Practice::getFax, Practice::setFax);
        binder.bind(email, Practice::getEmail, Practice::setEmail);
        
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
        lanr.setHelperText("Leistungserbringer-Abrechnungsnummer");
        
        bsnr = new TextField("BSNR");
        bsnr.setWidthFull();
        bsnr.setHelperText("Betriebsstättennummer");
        
        identifierLayout.add(lanr, bsnr);
        
        binder.bind(lanr, Practice::getLanr, Practice::setLanr);
        binder.bind(bsnr, Practice::getBsnr, Practice::setBsnr);
        
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

