# React-Migration Inventar (Vaadin → React)

Zweck: **Feature-Parität prüfbar machen**. Diese Liste ist die Minimal-Checkliste, bevor Vaadin final entfernt wird.

## Top-Level Navigation (Menu)

Aus `@Menu(...)`:

- **IVOM-Planer**: Route `ivom`  
  Quelle: `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentPlanMainView.java`
- **Patienten**: Route `patient-search`  
  Quelle: `src/main/java/de/bbajor/pvs/patient/ui/view/PatientMainView.java`
- **Terminkalender**: Route `appointment-calendar`  
  Quelle: `src/main/java/de/bbajor/pvs/appointment/ui/AppointmentCalendarView.java`
- **OP-Planer**: Route `surgicalcenter`  
  Quelle: `src/main/java/de/bbajor/pvs/surgicalcenter/ui/SurgicalCenterMainView.java`
- **Hilfe**: Route `help`  
  Quelle: `src/main/java/de/bbajor/pvs/base/ui/view/HelpView.java`
- **Einstellungen**: Route `settings`  
  Quelle: `src/main/java/de/bbajor/pvs/settings/ui/SettingsView.java`
- **Auswertungen**: Route `analytics`  
  Quelle: `src/main/java/de/bbajor/pvs/analytics/ui/AnalyticsOverviewView.java`

## Routen (Views)

Aus `@Route(...)`:

### Basis / Shell

- `/` (Main/Dashboard): `src/main/java/de/bbajor/pvs/base/ui/view/MainView.java`
- `/dashboard` (Redirect): `src/main/java/de/bbajor/pvs/base/ui/view/DashboardRedirectView.java`
- Layout/Shell (SideNav, UserMenu, MFA/Password Redirect): `src/main/java/de/bbajor/pvs/base/ui/view/MainLayout.java`

### Patienten

- `/patient-search`: `src/main/java/de/bbajor/pvs/patient/ui/view/PatientMainView.java`

### IVOM / Behandlung

- `/ivom`: `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentPlanMainView.java`
- `/ivom/:id`: `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentPlanDetailView.java`

### Termine / Scheduler

- `/appointment-calendar`: `src/main/java/de/bbajor/pvs/appointment/ui/AppointmentCalendarView.java`
- `/scheduler-management`: `src/main/java/de/bbajor/pvs/appointment/ui/SchedulerManagementView.java`

### OP-Zentrum

- `/surgicalcenter`: `src/main/java/de/bbajor/pvs/surgicalcenter/ui/SurgicalCenterMainView.java`
- `/surgicalcenter/:id`: `src/main/java/de/bbajor/pvs/surgicalcenter/ui/SurgicalCenterDetailView.java`

### Medikamente

- `/ivom-drugs`: `src/main/java/de/bbajor/pvs/medication/ui/MedicationView.java`

### Aufgaben

- `/aufgabenliste`: `src/main/java/de/bbajor/pvs/taskmanagement/ui/view/TaskListView.java`

### Auswertungen (Analytics)

- `/analytics`: `src/main/java/de/bbajor/pvs/analytics/ui/AnalyticsOverviewView.java`
- `/analytics/treatments-over-time`: `src/main/java/de/bbajor/pvs/analytics/ui/TreatmentsOverTimeView.java`
- `/analytics/treatments-by-timeslot`: `src/main/java/de/bbajor/pvs/analytics/ui/TreatmentsByTimeSlotView.java`
- `/analytics/patients-by-age`: `src/main/java/de/bbajor/pvs/analytics/ui/PatientsByAgeGroupView.java`
- `/analytics/patients-by-insurance-type`: `src/main/java/de/bbajor/pvs/analytics/ui/PatientsByInsuranceTypeView.java`
- `/analytics/patients-by-insurance-provider`: `src/main/java/de/bbajor/pvs/analytics/ui/PatientsByInsuranceProviderView.java`
- `/analytics/treatments-by-medication`: `src/main/java/de/bbajor/pvs/analytics/ui/TreatmentsByMedicationView.java`

### Hilfe

- `/help`: `src/main/java/de/bbajor/pvs/base/ui/view/HelpView.java`
- `/help/ivom`: `src/main/java/de/bbajor/pvs/base/ui/view/help/IvomHelpView.java`
- `/help/patient-search`: `src/main/java/de/bbajor/pvs/base/ui/view/help/PatientSearchHelpView.java`
- `/help/appointment-calendar`: `src/main/java/de/bbajor/pvs/base/ui/view/help/AppointmentCalendarHelpView.java`
- `/help/surgicalcenter`: `src/main/java/de/bbajor/pvs/base/ui/view/help/SurgicalCenterHelpView.java`
- `/help/ivom-drugs`: `src/main/java/de/bbajor/pvs/base/ui/view/help/MedicationHelpView.java`
- `/help/aufgabenliste`: `src/main/java/de/bbajor/pvs/base/ui/view/help/TaskListHelpView.java`
- `/help/augen-termine`: `src/main/java/de/bbajor/pvs/base/ui/view/help/OphthalmologyHelpView.java`
- `/help/settings`: `src/main/java/de/bbajor/pvs/base/ui/view/help/SettingsHelpView.java`
- `/help/roles`: `src/main/java/de/bbajor/pvs/base/ui/view/help/RolesHelpView.java`

### Augen-Termine

- `/augen-termine`: `src/main/java/de/bbajor/pvs/ophthalmology/ui/OphthalmologyAppointmentView.java`

### Einstellungen

- `/settings`: `src/main/java/de/bbajor/pvs/settings/ui/SettingsView.java`

### Admin / Institution / Standort

- `/admin/institutions`: `src/main/java/de/bbajor/pvs/institution/ui/InstitutionManagementView.java`
- `/admin/locations`: `src/main/java/de/bbajor/pvs/location/ui/LocationManagementView.java`
- `/admin/send-mail`: `src/main/java/de/bbajor/pvs/security/email/ui/SendMailView.java`
- `/admin/users`: `src/main/java/de/bbajor/pvs/security/ui/UserAdminView.java`
- `/admin/super-settings`: `src/main/java/de/bbajor/pvs/institution/ui/SuperAdminSettingsView.java`
- `/admin/super-admin-settings`: `src/main/java/de/bbajor/pvs/security/ui/SuperAdminSettingsView.java`

### Security / Account Flows

- `/post-login`: `src/main/java/de/bbajor/pvs/security/ui/PostLoginRedirectView.java`
- `/password-change`: `src/main/java/de/bbajor/pvs/security/ui/PasswordChangeView.java`
- `/mfa-setup`: `src/main/java/de/bbajor/pvs/security/mfa/ui/MfaSetupView.java`
- `/mfa-verify`: `src/main/java/de/bbajor/pvs/security/mfa/ui/MfaVerificationView.java`
- `/mfa-reset`: `src/main/java/de/bbajor/pvs/security/mfa/ui/MfaResetView.java`
- `/pin-reset-request`: `src/main/java/de/bbajor/pvs/security/pin/ui/PinResetRequestView.java`
- `/pin-reset`: `src/main/java/de/bbajor/pvs/security/pin/ui/PinResetView.java`

## Dialoge (Vaadin)

Diese Dialoge sind typischerweise „use-case heavy“ (State + Validierung + Service-Aufrufe) und müssen in React als Modals/Flows neu gebaut werden:

- `src/main/java/de/bbajor/pvs/patient/ui/view/PatientDialog.java`
- `src/main/java/de/bbajor/pvs/appointment/ui/AppointmentDialog.java`
- `src/main/java/de/bbajor/pvs/appointment/ui/OfficeHoursDialog.java`
- `src/main/java/de/bbajor/pvs/appointment/ui/SchedulerDialog.java`
- `src/main/java/de/bbajor/pvs/appointment/ui/SchedulerAssignmentDialog.java`
- `src/main/java/de/bbajor/pvs/institution/ui/InstitutionAdministratorDialog.java`
- `src/main/java/de/bbajor/pvs/institution/ui/InstitutionEmailContactDialog.java`
- `src/main/java/de/bbajor/pvs/medication/ui/MedicationCreateDialog.java`
- `src/main/java/de/bbajor/pvs/medication/ui/MedicationDetailDialog.java`
- `src/main/java/de/bbajor/pvs/taskmanagement/ui/view/TaskReviewDialog.java`
- `src/main/java/de/bbajor/pvs/settings/ui/WhisperInstallationDialog.java`
- `src/main/java/de/bbajor/pvs/settings/ui/tabs/InsuranceDialog.java`
- `src/main/java/de/bbajor/pvs/settings/ui/tabs/LocationDialog.java`
- `src/main/java/de/bbajor/pvs/settings/ui/tabs/UserDialog.java`
- `src/main/java/de/bbajor/pvs/ai/ui/VoiceInputDialog.java`
- `src/main/java/de/bbajor/pvs/ai/ui/EntityVerificationDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentPlanDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/TreatmentDetailDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/NextTreatmentBookingDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/WeekListDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/AppointmentCalendarDialog.java`
- `src/main/java/de/bbajor/pvs/intravitreal/treatment/ui/AppointmentOverviewDialog.java`

## Bestehende REST-APIs (bereits heute)

Diese Endpunkte existieren bereits und müssen in das neue API-Konzept (Versionierung, Auth, CORS, Errors) eingepasst werden:

- AI: `src/main/java/de/bbajor/pvs/ai/controller/ExtractionController.java`, `src/main/java/de/bbajor/pvs/ai/controller/VoiceInputController.java`
- eGK: `src/main/java/de/bbajor/pvs/egk/api/EgkAgentController.java`

