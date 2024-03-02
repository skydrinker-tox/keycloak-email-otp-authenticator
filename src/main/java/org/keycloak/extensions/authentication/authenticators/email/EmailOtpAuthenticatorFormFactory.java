package org.keycloak.extensions.authentication.authenticators.email;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;


public class EmailOtpAuthenticatorFormFactory implements AuthenticatorFactory {
    @Override
    public String getId() {
        return "email-otp-authenticator";
    }

    @Override
    public String getDisplayType() {
        return "Email OTP Form";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    public static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
            AuthenticationExecutionModel.Requirement.REQUIRED, AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Email otp authenticator.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
                new ProviderConfigProperty(EmailOtpConstants.CODE_LENGTH, "Code length",
                        "The number of digits of the generated code.",
                        ProviderConfigProperty.STRING_TYPE, String.valueOf(EmailOtpConstants.DEFAULT_LENGTH)),
                new ProviderConfigProperty(EmailOtpConstants.CODE_TTL, "Time-to-live",
                        "The time to live in seconds for the code to be valid.", ProviderConfigProperty.STRING_TYPE,
                        String.valueOf(EmailOtpConstants.DEFAULT_TTL)),
                new ProviderConfigProperty(EmailOtpConstants.RESEND_WAIT_DELAY, "Min delay between resends",
                        "The minimum delay in seconds before user can ask for a code resend", ProviderConfigProperty.STRING_TYPE,
                        String.valueOf(EmailOtpConstants.DEFAULT_RESEND_WAIT_DELAY)));
    }

    @Override
    public void close() {
        // NO-OP
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new EmailOtpAuthenticatorForm(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NO-OP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NO-OP
    }
}
