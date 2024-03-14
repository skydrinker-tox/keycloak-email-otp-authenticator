# 🔒 Keycloak Email OTP Authenticator

This repository is a Keycloak Authenticator SPI that can be use as a second factor to authenticate the user via an OTP sent by Email.

Tested with Keycloak version 23.0.7 (Docker) ✅ 

If you're not familiar with custom Keycloak extensions, check the [Keycloak documentation - Server Development part](https://www.keycloak.org/docs/latest/server_development/index.html).


# 🔨 Build

Use `mvn package` to build the SPI and generate the jar file (will be generated in `target`). 

# 🚀 Deployment

Copy the jar file to the `/opt/keycloak/providers/` directory before running the `kc.sh build` command.\
 If you're not familiar with Keycloak customization, see Keycloak [guides](https://www.keycloak.org/guides#server) and [documentation](https://www.keycloak.org/documentation).

# ✨ Usage

## Customize an authentication flow
In order to add Email 2FA to your authentication flow, you'll need to add this authenticator in your flow. If you don't know how to do it, see [Keycloak documention about authentication flows](https://www.keycloak.org/docs/latest/server_admin/index.html#_authentication-flows).

## Configure email sender
If you're using the built-in email sender, remember to configure your realm's SMTP settings. From the admin UI :
1. Go to the `Realm settings` section.
2. Click on the `Email` tab and configure the SMTP email sender.


# 🎨 Theme override

This SPI uses the standard Keycloak login theme to display the OTP form, and has its own email theme (`email-2fa-theme`) for the OTP mail template. Both can be overriden in your own custom theme.

If you're not using any custom theme for Keycloak, configure your realm (or specific client) to use the `email-2fa-theme` as email theme (no mail with be send if you don't)

If you already have your own custom theme you can: 
- override the OTP form page by adding the `email-code-form.ftl` to your `custom-theme/login` directory. You can retrieve this ftl file [here in the sources](./src/main/resources/theme-resources/templates/email-code-form.ftl) or extract it from the keycloak-email-otp-authenticator jar file. For localization, see the available messages properties [here](./src/main/resources/theme-resources/messages/) and override the values in your own `messages_<lang>.properties` file(s).
- override the default email template by adding the `code-email.ftl` to the html and text templates in your own `custom-theme/email` folder. The original FTLs can be found in the jar file of this SPI, or [here in the sources](./src/main/resources/theme/email-otp-theme/email). Like the OTP form page, you can see available messages properties for localization [here](./src/main/resources/theme/email-otp-theme/email/messages/) and override the values in your own `messages_<lang>.properties` file(s).

Here is the simplified structure of the directories for both login and email theme :

```
my-custom-theme/
    email/
        html/
            code-email.ftl
        text/
            code-email.ftl
    login/
        email-code-form.ftl

```






