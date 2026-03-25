package io.openems.backend.common.mail;

/**
 * Exception thrown, when sending a mail fails. This can be used to distinguish
 * between exceptions thrown by the mailer and other exceptions.
 */
public class SendMailException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SendMailException(String message) {
		super(message);
	}

}
