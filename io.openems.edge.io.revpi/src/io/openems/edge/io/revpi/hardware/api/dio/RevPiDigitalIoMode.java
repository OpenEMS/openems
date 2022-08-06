package io.openems.edge.io.revpi.hardware.api.dio;

public enum RevPiDigitalIoMode {
	NOT_USED("Not used"), //
	DIGITAL_IN("Digital In"), //
	DIGITAL_OUT("Digital Out"), //
	PULSE_IN("Pulse In"), //
	PULSE_OUT("Pulse Out"), //
	PWM_IN("PWM In"), //
	PWM_OUT("PWM Out");

	public final String modeDio;

	public boolean isDigitalIn() {
		return (this == DIGITAL_IN);
	}

	public boolean isDigitalOut() {
		return (this == DIGITAL_OUT);
	}

	public boolean isPulseIn() {
		return (this == PULSE_IN);
	}

	public boolean isPulseOut() {
		return (this == PULSE_OUT);
	}

	public boolean isPWMIn() {
		return (this == PWM_IN);
	}

	public boolean isPWMOut() {
		return (this == PWM_OUT);
	}

	private RevPiDigitalIoMode(String modeDIO) {
		this.modeDio = modeDIO;
	}
}
