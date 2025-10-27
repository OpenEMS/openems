package io.openems.edge.controller.api.modbus;

import java.util.function.Consumer;

import com.ghgande.j2mod.modbus.procimg.IllegalAddressException;

public class MyIllegalAddressException extends IllegalAddressException {

	private static final long serialVersionUID = 1L;

	/**
	 * Builds a {@link MyIllegalAddressException} and logs the message.
	 * 
	 * @param logWarn {@link Consumer} for logging a warning message
	 * @param message the message text
	 * @return a new {@link MyIllegalAddressException}
	 */
	public static MyIllegalAddressException fromWithLog(Consumer<String> logWarn, String message) {
		logWarn.accept(message);
		return new MyIllegalAddressException(message);
	}

	private MyIllegalAddressException(String message) {
		super(message);
	}
}
