package io.openems.backend.metadata.odoo.odoo;

public class SessionExpiredException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SessionExpiredException() {
	}

	public SessionExpiredException(String message) {
		super(message);
	}

	public SessionExpiredException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionExpiredException(Throwable cause) {
		super(cause);
	}

	public SessionExpiredException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
