package io.openems.common.channel;

import io.openems.common.jsonrpc.notification.AggregatedDataNotification;
import io.openems.common.jsonrpc.notification.TimestampedDataNotification;

/**
 * The {@link PersistencePriority} is used by...
 * 
 * <ul>
 * <li>Timedata.Rrd4j: persist Channel values locally
 * <li>Controller.Api.MQTT: transmit Channel values via MQTT
 * <li>Controller.Api.Backend: transmit Channel values to OpenEMS Backend
 * </ul>
 * 
 * <p>
 * These services use the {@link PersistencePriority} to distinguish whether a
 * value of a Channel should be:
 * <ul>
 * <li>persisted/transmitted in high resolution (e.g. once per second)
 * <li>persisted/transmitted in low resolution (e.g. aggregated to 5 minutes
 * values)
 * <li>not persisted
 * </ul>
 * 
 * <p>
 * The {@link PersistencePriority} of a Channel may be set via
 * AbstractDoc::persistencePriority(). Defaults are:
 * <ul>
 * <li>{@link #HIGH} for StateChannels
 * <li>{@link #LOW} for everything else
 * </ul>
 */
public enum PersistencePriority {

	/**
	 * Channels with at least this priority are by default (if not configured
	 * differently)...
	 * 
	 * <ul>
	 * <li>Timedata.Rrd4j: not persisted
	 * <li>Controller.Api.MQTT: transmitted in high resolution
	 * <li>Controller.Api.Backend: not transmitted
	 * </ul>
	 */
	VERY_LOW(0), //

	/**
	 * Channels with at least this priority are by default (if not configured
	 * differently)...
	 * 
	 * <ul>
	 * <li>Controller.Api.Backend: transmitted as aggregated values (via
	 * {@link AggregatedDataNotification})
	 * </ul>
	 */
	LOW(1), //

	/**
	 * Channels with at least this priority are by default (if not configured
	 * differently)...
	 * 
	 * <ul>
	 * <li>Timedata.Rrd4j: persisted as aggregated values
	 * </ul>
	 */
	MEDIUM(2), //

	/**
	 * Channels with at least this priority are by default (if not configured
	 * differently)...
	 * 
	 * <ul>
	 * <li>Controller.Api.Backend: transmitted in high resolution (via
	 * {@link TimestampedDataNotification}), i.e. on every change or at least once
	 * every 5 minutes
	 * </ul>
	 */
	HIGH(3), //

	/**
	 * {@link PersistencePriority#VERY_HIGH} is reserved for Channels of the
	 * `Core.Sum` Component.
	 */
	VERY_HIGH(4), //
	;

	private final int value;

	private PersistencePriority(int value) {
		this.value = value;
	}

	/**
	 * Is this {@link PersistencePriority} at least as high as the given
	 * {@link PersistencePriority}?.
	 *
	 * @param other the given {@link PersistencePriority}
	 * @return true if this is equal or higher than other
	 */
	public boolean isAtLeast(PersistencePriority other) {
		return this.value >= other.value;
	}

	/**
	 * Is this {@link PersistencePriority} at lower than the given
	 * {@link PersistencePriority}?.
	 *
	 * @param other the given {@link PersistencePriority}
	 * @return true if this is strictly lower than other
	 */
	public boolean isLowerThan(PersistencePriority other) {
		return this.value < other.value;
	}

}
