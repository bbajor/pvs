package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MainErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(MainErrorHandler.class);

    @Bean
    public VaadinServiceInitListener errorHandlerInitializer() {
        return (event) -> event.getSource().addSessionInitListener(
                sessionInitEvent -> sessionInitEvent.getSession().setErrorHandler(errorEvent -> {
                    Throwable throwable = errorEvent.getThrowable();
                    // Log error without PII (DSGVO compliance)
                    log.error("An unexpected error occurred: {}", throwable.getMessage(), throwable);
                    
                    errorEvent.getComponent().flatMap(Component::getUI).ifPresent(ui -> {
                        // DSGVO-compliant error message (no PII, no technical details)
                        var notification = new Notification(
                                "Ein unerwarteter Fehler ist aufgetreten. Bitte versuchen Sie es erneut oder kontaktieren Sie den Administrator.");
                        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                        notification.setPosition(Notification.Position.TOP_CENTER);
                        notification.setDuration(5000);
                        ui.access(notification::open);
                    });
                }));
    }
}
