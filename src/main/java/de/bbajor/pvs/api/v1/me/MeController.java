package de.bbajor.pvs.api.v1.me;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.institution.service.CurrentInstitutionService;
import de.bbajor.pvs.security.CurrentUser;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final CurrentUser currentUser;
    private final CurrentInstitutionService currentInstitutionService;

    public MeController(CurrentUser currentUser, CurrentInstitutionService currentInstitutionService) {
        this.currentUser = currentUser;
        this.currentInstitutionService = currentInstitutionService;
    }

    @GetMapping
    public MeResponse me() {
        var user = currentUser.require();
        return new MeResponse(
                user.getUserId().toString(),
                user.getPreferredUsername(),
                user.getFullName(),
                currentInstitutionService.getCurrentInstitutionId().orElse(null));
    }
}

