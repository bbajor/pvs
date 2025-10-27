package de.bbajor.pvs.security.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@Route("admin/users")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.OWNER })
@PageTitle("Benutzerverwaltung")
@Menu(order = 6, icon = "vaadin:cog-o", title = "Benutzerverwaltung")
@PermitAll
public class UserAdminView extends VerticalLayout {

    // Placeholder/sample UI for role visibility; actual persistence can be added later
    private final Grid<UserRow> grid = new Grid<>(UserRow.class, false);
    private final FormLayout formLayout = new FormLayout();
    private final TextField usernameField = new TextField("Benutzername");
    private final PasswordField passwordField = new PasswordField("Passwort");
    private final Select<String> roleSelect = new Select<>();
    private final Checkbox enabledCheckbox = new Checkbox("Aktiv");
    private final Button saveButton = new Button("Speichern");
    private final Button cancelButton = new Button("Abbrechen");

    private final UserAccountRepository userAccountRepository;

    public UserAdminView(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        H2 title = new H2("Benutzerverwaltung");
        header.add(title);
        header.expand(title);

        roleSelect.setLabel("Rolle");
        roleSelect.setItems(AppRoles.ADMIN, AppRoles.OWNER, AppRoles.DOCTOR, AppRoles.MEDICAL_STAFF, AppRoles.TECH_USER,
                AppRoles.USER);
        roleSelect.setEmptySelectionAllowed(false);

        grid.addColumn(UserRow::username).setHeader("Benutzername");
        grid.addColumn(UserRow::role).setHeader("Rolle");
        grid.addColumn(ur -> ur.enabled() ? "Ja" : "Nein").setHeader("Aktiv");
        grid.setItems(userAccountRepository.findAll().stream()
                .map(ua -> new UserRow(ua.getUsername(), String.join(", ", ua.getRoles()), ua.isEnabled())));
        grid.setSizeFull();

        formLayout.add(usernameField, passwordField, roleSelect, enabledCheckbox);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            String username = usernameField.getValue();
            String role = roleSelect.getValue();
            boolean enabled = enabledCheckbox.getValue();
            if (username == null || role == null) {
                return;
            }
            UserAccount ua = userAccountRepository.findByUsername(username).orElseGet(UserAccount::new);
            ua.setUsername(username);
            ua.getRoles().clear();
            ua.getRoles().add(role);
            ua.setEnabled(enabled);
            if (ua.getPasswordHash() == null || ua.getPasswordHash().isEmpty()) {
                ua.setPasswordHash("{noop}" + (passwordField.getValue() == null ? "" : passwordField.getValue()));
            }
            userAccountRepository.save(ua);
            grid.setItems(userAccountRepository.findAll().stream()
                    .map(u -> new UserRow(u.getUsername(), String.join(", ", u.getRoles()), u.isEnabled())));
        });
        HorizontalLayout actions = new HorizontalLayout(saveButton, cancelButton);
        VerticalLayout detailLayout = new VerticalLayout(formLayout, actions);
        detailLayout.setWidth("400px");
        detailLayout.setPadding(true);

        HorizontalLayout split = new HorizontalLayout(grid, detailLayout);
        split.setSizeFull();
        split.setFlexGrow(1, grid);
        split.setFlexGrow(0, detailLayout);

        add(header, split);
        expand(split);
    }

    public record UserRow(String username, String role, boolean enabled) {}

    // private final Grid<User> grid = new Grid<>(User.class, false);
    // private final FormLayout formLayout = new FormLayout();
    // private final Binder<User> binder = new Binder<>(User.class);
    // private User selectedUser;

    // private final TextField usernameField = new TextField("Benutzername");
    // private final PasswordField passwordField = new PasswordField("Passwort");
    // private final Select<AppRoles> roleSelect;
    // private final Checkbox enabledCheckbox = new Checkbox("Aktiv");

    // private final Button saveButton = new Button("Speichern");
    // private final Button deleteButton = new Button("Löschen");
    // private final Button cancelButton = new Button("Abbrechen");

    // public UserAdminView(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    //     this.userRepository = userRepository;
    //     this.passwordEncoder = passwordEncoder;

    //     setSizeFull();
    //     setSpacing(false);
    //     setPadding(false);

    //     // --- Master Layout (Grid + Add Button)
    //     HorizontalLayout header = new HorizontalLayout();
    //     header.setWidthFull();
    //     header.setAlignItems(Alignment.CENTER);

    //     H2 title = new H2("Benutzerverwaltung");
    //     Button addButton = new Button("Neuer Benutzer", e -> addNewUser());
    //     addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    //     header.add(title, addButton);
    //     header.expand(title);

    //     roleSelect  = new Select<>();
    //     roleSelect.setLabel("Rollen");
    //     roleSelect.setItems(AppRoles.values());
    //     roleSelect.setItemLabelGenerator(AppRoles::getRoleName);
    //     roleSelect.setEmptySelectionAllowed(false);

    //     // grid.addColumn(User::getId).setHeader("ID").setWidth("80px");
    //     // grid.addColumn(User::getUsername).setHeader("Benutzername").setAutoWidth(true);
    //     // grid.addColumn(u -> {
    //     //     StringBuilder roles = new StringBuilder();
    //     //     u.getRoles().forEach(role -> roles.append(role).append(" "));
    //     //     return roles.toString().trim();
    //     // }).setHeader("Rollen");
    //     // grid.addColumn(u -> u.isEnabled() ? "Ja" : "Nein").setHeader("Aktiv");
    //     grid.setItems(userRepository.findAll());
    //     grid.setSizeFull();

    //     grid.asSingleSelect().addValueChangeListener(e -> {
    //         if (e.getValue() != null) {
    //             editUser(e.getValue());
    //         } else {
    //             clearForm();
    //         }
    //     });

    //     // --- Detail Layout (Formular)
    //     roleSelect.setLabel("Rolle");
    //     binder.bindInstanceFields(this);

    //     formLayout.add(usernameField, passwordField, roleSelect, enabledCheckbox);
    //     formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
    //             new FormLayout.ResponsiveStep("600px", 2));

    //     HorizontalLayout actions = new HorizontalLayout(saveButton, deleteButton, cancelButton);
    //     actions.setSpacing(true);

    //     saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
    //     deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);

    //     saveButton.addClickListener(e -> saveUser());
    //     deleteButton.addClickListener(e -> deleteUser());
    //     cancelButton.addClickListener(e -> clearForm());

    //     VerticalLayout detailLayout = new VerticalLayout(formLayout, actions);
    //     detailLayout.setWidth("400px");
    //     detailLayout.setPadding(true);
    //     detailLayout.setVisible(false);

    //     // --- Combine Master + Detail
    //     SplitLayout splitLayout = new SplitLayout(grid, detailLayout);
    //     splitLayout.setSizeFull();
    //     splitLayout.setSplitterPosition(70);

    //     add(header, splitLayout);
    //     expand(splitLayout);
    // }

    // private void addNewUser() {
    //     selectedUser = new User();
    //     binder.setBean(selectedUser);
    //     passwordField.clear();
    //     getDetailLayout().setVisible(true);
    // }

    // private void editUser(User user) {
    //     selectedUser = user;
    //     binder.setBean(selectedUser);
    //     passwordField.clear();
    //     getDetailLayout().setVisible(true);
    // }

    // private void saveUser() {
    //     if (binder.validate().isOk() && selectedUser != null) {
    //         if (passwordField.getValue() != null && !passwordField.getValue().isEmpty()) {
    //             // selectedUser.setPassword(passwordEncoder.encode(passwordField.getValue()));
    //         }
    //         userRepository.save(selectedUser);
    //         updateGrid();
    //         clearForm();
    //     }
    // }

    // private void deleteUser() {
    //     // if (selectedUser != null && selectedUser.getId() != null) {
    //     //     userRepository.delete(selectedUser);
    //     //     updateGrid();
    //     //     clearForm();
    //     // }
    // }

    // private void clearForm() {
    //     selectedUser = null;
    //     binder.setBean(null);
    //     passwordField.clear();
    //     getDetailLayout().setVisible(false);
    //     grid.deselectAll();
    // }

    // private void updateGrid() {
    //     grid.setItems(userRepository.findAll());
    // }

    // private Component getDetailLayout() {
    //     // Das Detail-Layout ist der zweite Teil des SplitLayouts:
    //     return ((SplitLayout) getComponentAt(1)).getSecondaryComponent();
    // }
}
