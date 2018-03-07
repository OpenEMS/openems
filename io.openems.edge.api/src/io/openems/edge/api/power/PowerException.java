package io.openems.edge.api.power;

public class PowerException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5801845530898563853L;

	public PowerException() {
		super();
	}

	public PowerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PowerException(String message, Throwable cause) {
		super(message, cause);
	}

	public PowerException(String message) {
		super(message);
	}

	public PowerException(Throwable cause) {
		super(cause);
	}

	
}
