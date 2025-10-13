package de.bbajor.pvs.security.ui;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@Route("admin/users")
@RolesAllowed("ADMIN")
@PageTitle("Benutzerverwaltung")
@Menu(order = 6, icon = "vaadin:cog-o", title = "Benutzerverwaltung")
@PermitAll
public class UserAdminView extends VerticalLayout {

    // private final UserRepository userRepository;
    // private final PasswordEncoder passwordEncoder;

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
