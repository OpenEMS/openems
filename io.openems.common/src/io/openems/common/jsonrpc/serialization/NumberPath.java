package io.openems.common.jsonrpc.serialization;

public interface NumberPath extends JsonPath {

	/**
	 * Gets the string value of the current path.
	 * 
	 * @return the value
	 */
	public Number get();

	/**
	 * Gets the current {@link Number} as a primitive {@link Double}.
	 * 
	 * @return the double value
	 */
	public default double getAsDouble() {
		return this.get().doubleValue();
	}

	/**
	 * Gets the current {@link Number} as a primitive {@link Float}.
	 * 
	 * @return the float value
	 */
	public default float getAsFloat() {
		return this.get().floatValue();
	}

	/**
	 * Gets the current {@link Number} as a primitive {@link Long}.
	 * 
	 * @return the long value
	 */
	public default long getAsLong() {
		return this.get().longValue();
	}

	/**
	 * Gets the current {@link Number} as a primitive {@link Integer}.
	 * 
	 * @return the integer value
	 */
	public default int getAsInt() {
		return this.get().intValue();
	}

	/**
	 * Gets the current {@link Number} as a primitive {@link Short}.
	 * 
	 * @return the short value
	 */
	public default short getAsShort() {
		return this.get().shortValue();
	}

	/**
	 * Gets the current {@link Number} as a primitive {@link Byte}.
	 * 
	 * @return the byte value
	 */
	public default byte getAsByte() {
		return this.get().byteValue();
	}

}
