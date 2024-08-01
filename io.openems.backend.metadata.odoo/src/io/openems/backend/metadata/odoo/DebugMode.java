package io.openems.backend.metadata.odoo;

public enum DebugMode {

	OFF, SIMPLE, DETAILED;

	/**
	 * Is this {@link DebugMode} at least as high as the other {@link DebugMode}?.
	 * 
	 * @param other the other {@link DebugMode}
	 * @return true if yes
	 */
	public boolean isAtLeast(DebugMode other) {
		return this.ordinal() >= other.ordinal();
	}
}
