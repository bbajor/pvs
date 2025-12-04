package de.bbajor.pvs.security.pin;

import org.springframework.security.authentication.AbstractAuthenticationToken;

/**
 * Authentication token for PIN-based passwordless login.
 */
public class PinAuthenticationToken extends AbstractAuthenticationToken {

    private final String institutionCode;
    private final String username;
    private final String pin;
    private final String mfaCode;

    public PinAuthenticationToken(String institutionCode, String username, String pin, String mfaCode) {
        super(null);
        this.institutionCode = institutionCode;
        this.username = username;
        this.pin = pin;
        this.mfaCode = mfaCode;
        setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return pin;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }

    public String getInstitutionCode() {
        return institutionCode;
    }

    public String getPin() {
        return pin;
    }

    public String getMfaCode() {
        return mfaCode;
    }

    @Override
    public String getName() {
        return username;
    }
}

