package io.openems.core.utilities.power.symmetric;

public class PowerException extends Exception {

	public PowerException() {
		super();
	}

	public PowerException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}

	public PowerException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public PowerException(String arg0) {
		super(arg0);
	}

	public PowerException(Throwable arg0) {
		super(arg0);
	}

}
