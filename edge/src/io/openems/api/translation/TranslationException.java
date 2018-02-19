package io.openems.api.translation;

public class TranslationException extends RuntimeException {

	/**
	 *
	 */
	private static final long serialVersionUID = -189217968357389677L;

	public TranslationException() {
		super();
	}

	public TranslationException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public TranslationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public TranslationException(String arg0) {
		super(arg0);
	}

	public TranslationException(Throwable arg0) {
		super(arg0);
	}



}
