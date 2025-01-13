package io.openems.edge.bridge.modbus.sunspec;

import io.openems.edge.bridge.modbus.sunspec.Point.ChannelIdPoint;
import io.openems.edge.common.channel.ChannelId;

/**
 * Holds one "Point" or "Register" within a SunSpec "Model" or "Block".
 */
public interface SunSpecPoint {

	/**
	 * Gets the Point-ID.
	 *
	 * <p>
	 * This method refers to {@link Enum#name()}.
	 *
	 * @return the ID.
	 */
	public String name();

	/**
	 * The internal {@link Point} object for easier handling in Enums.
	 *
	 * @return the internal PointImpl
	 */
	public Point get();

	/**
	 * Gets the {@link ChannelId} for this Point.
	 *
	 * @return the ChannelId.
	 */
	public default ChannelId getChannelId() {
		var p = this.get();
		if (p instanceof ChannelIdPoint cp) {
			return cp.channelId;
		}
		throw new IllegalAccessError("SunSpecPoint [" + this.name() + "] has no Channel-ID");
	}
}