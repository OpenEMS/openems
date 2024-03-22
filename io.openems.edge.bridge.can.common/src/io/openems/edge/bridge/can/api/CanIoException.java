package io.openems.edge.bridge.can.api;

/**
 * CAN IO exception, used when a concrete error with the CAN hardware was
 * detected.
 */
public class CanIoException extends CanException {

	private static final long serialVersionUID = 2881690676333354436L;

	public CanIoException() {
	}

	public CanIoException(String msg) {
		super(msg);
	}

	public CanIoException(String msg, String hexMessage, String hexMessage2) {
		super(msg + " " + hexMessage + " " + hexMessage2);
	}

}
