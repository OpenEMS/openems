package io.openems.edge.bridge.can.io.hw;

import java.io.IOException;

public class CanDeviceException extends IOException {

	private static final long serialVersionUID = -7688131957437783883L;

	public CanDeviceException() {
	}

	public CanDeviceException(String txt) {
		super(txt);
	}

}
