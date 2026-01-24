package io.openems.edge.system.fenecon.home.enums;

import io.openems.common.channel.Level;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public enum StateLed {
	BLUE(Color.BLUE, true), //
	BLUE_DOTTED(Color.BLUE, false);

	public final Color color;
	public final boolean isPermanent;

	private StateLed(Color color, boolean isPermanent) {
		this.color = color;
		this.isPermanent = isPermanent;
	}

	/**
	 * Determines the {@link StateLed} depending on the different component
	 * parameters.
	 * 
	 * @param sum     the {@link Sum} component
	 * @param backend the {@link ControllerApiBackend} component
	 * @return the {@link StateLed}; possibly null
	 */
	public static StateLed determineFrom(Sum sum, ControllerApiBackend backend) {
		if (!sum.getState().isAtLeast(Level.WARNING)) {
			return StateLed.BLUE;
		}

		if (backend != null && !backend.isConnected()) {
			return StateLed.BLUE_DOTTED;
		}

		return null;
	}
}
