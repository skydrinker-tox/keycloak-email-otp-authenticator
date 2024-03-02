package org.keycloak.extensions.authentication.authenticators.email;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.SecretGenerator;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailOtpAuthenticatorForm extends AbstractUsernameFormAuthenticator {
    private final KeycloakSession session;
    private static final Logger logger = Logger.getLogger(EmailOtpAuthenticatorForm.class);
    public EmailOtpAuthenticatorForm(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        challenge(context, null);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        generateAndSendEmailCode(context);

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (error != null) {
            if (field != null) {
                form.addError(new FormMessage(field, error));
            } else {
                form.setError(error);
            }
        }
        Response response = form.createForm("email-code-form.ftl");
        context.challenge(response);
        return response;
    }

    private void generateAndSendEmailCode(AuthenticationFlowContext context) {
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        AuthenticationSessionModel session = context.getAuthenticationSession();

        if (session.getAuthNote(EmailOtpConstants.CODE) != null) {
            // skip sending email code
            return;
        }

        int length = EmailOtpConstants.DEFAULT_LENGTH;
        int ttl = EmailOtpConstants.DEFAULT_TTL;
        if (config != null) {
            // get config values
            length = Integer.parseInt(config.getConfig().get(EmailOtpConstants.CODE_LENGTH));
            ttl = Integer.parseInt(config.getConfig().get(EmailOtpConstants.CODE_TTL));
        }

        String code = SecretGenerator.getInstance().randomString(length, SecretGenerator.DIGITS);
        sendEmailWithCode(context.getRealm(), context.getUser(), code, ttl);
        session.setAuthNote(EmailOtpConstants.CODE, code);
        session.setAuthNote(EmailOtpConstants.CODE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
        session.setAuthNote(EmailOtpConstants.PREVIOUS_SENT_TIME, Long.toString(System.currentTimeMillis()));
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            context.resetFlow();
            return;
        }
        UserModel userModel = context.getUser();
        if (!enabledUser(context, userModel)) {
            // error in context is present in enabledUser/isDisabledByBruteForce
            return;
        }

        if (formData.containsKey("resend")) {
            long delayBeforeResend = getSecondsUntilResendAvailable(context);
            if (delayBeforeResend <= 0) {
                resetEmailCode(context);
                challenge(context, null);
            } else {
                context.form().setError(EmailOtpMessages.RESEND_DELAY_ERROR, delayBeforeResend);
                challenge(context, null);
            }

            return;
        }

        AuthenticationSessionModel session = context.getAuthenticationSession();
        String code = session.getAuthNote(EmailOtpConstants.CODE);
        String ttl = session.getAuthNote(EmailOtpConstants.CODE_TTL);
        String enteredCode = formData.getFirst(EmailOtpConstants.CODE);

        if (enteredCode.equals(code)) {
            if (Long.parseLong(ttl) < System.currentTimeMillis()) {
                // expired
                context.getEvent().user(userModel).error(Errors.EXPIRED_CODE);
                Response challengeResponse = challenge(context, EmailOtpMessages.EXPIRED_EMAIL_CODE);
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, challengeResponse);
            } else {
                // valid
                resetEmailCode(context);
                context.success();
            }
        } else {
            // invalid
            AuthenticationExecutionModel execution = context.getExecution();
            if (execution.isRequired()) {
                context.getEvent().user(userModel).error(Errors.INVALID_USER_CREDENTIALS);
                Response challengeResponse = challenge(context, Messages.INVALID_ACCESS_CODE);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, challengeResponse);
            } else if (execution.isConditional() || execution.isAlternative()) {
                context.attempted();
            }
        }
    }

    @Override
    protected String disabledByBruteForceError() {
        return Messages.INVALID_ACCESS_CODE;
    }

    private void resetEmailCode(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(EmailOtpConstants.CODE);
    }

    private long getSecondsUntilResendAvailable(AuthenticationFlowContext context) {
        AuthenticationSessionModel session = context.getAuthenticationSession();
        String previousSentTime = session.getAuthNote(EmailOtpConstants.PREVIOUS_SENT_TIME);
        if (previousSentTime == null) {
            return 0;
        }
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int resendWaitDelay = EmailOtpConstants.DEFAULT_RESEND_WAIT_DELAY;
        if (config != null) {
            resendWaitDelay = Integer.parseInt(config.getConfig().get(EmailOtpConstants.RESEND_WAIT_DELAY));
        }
        long timeSinceLastSent = System.currentTimeMillis() - Long.parseLong(previousSentTime);
        long remainingMillisecond = (resendWaitDelay * 1000L) - timeSinceLastSent;
        return remainingMillisecond / 1000L;
    }
    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    private void sendEmailWithCode(RealmModel realm, UserModel user, String code, int ttl) {
        if (user.getEmail() == null) {
            logger.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER, "detail event", "userErrorMessage");
            // Attention ici en l"état ca termine le flux avec un event LOGIN_ERROR et un erreur "invalid_user_credentials"
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);
        mailBodyAttributes.put("ttl", ttl);

        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);
        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            logger.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }
}
