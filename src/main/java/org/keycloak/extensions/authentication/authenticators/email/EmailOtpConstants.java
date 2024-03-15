package org.keycloak.extensions.authentication.authenticators.email;


public class EmailOtpConstants {
	public static final String CODE = "emailCode";
	public static final String CODE_LENGTH = "length";
	public static final String CODE_TTL = "ttl";
	public static final String RESEND_WAIT_DELAY = "resendWaitDelay";
	public static final String HARDCODED_OTP = "hardcodedOtp";
	public static final String DISABLE_MAILING = "disableMailing";
	public static final String PREVIOUS_SENT_TIME = "codeSentAt";
	public static final int DEFAULT_LENGTH = 6;
	public static final int DEFAULT_TTL = 300;
	public static final int DEFAULT_RESEND_WAIT_DELAY = 0;
	public static final boolean DEFAULT_DISABLE_MAILING = false;


}
