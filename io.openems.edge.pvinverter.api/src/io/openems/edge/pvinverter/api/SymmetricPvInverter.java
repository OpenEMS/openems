package io.openems.edge.pvinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.AccessMode;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Represents a 3-Phase, symmetric PV-Inverter.
 */
@ProviderType
public interface SymmetricPvInverter extends SymmetricMeter {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Read/Set Active Power Limit
		 * 
		 * <ul>
		 * <li>Interface: PV-Inverter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER_LIMIT(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the type of this Meter
	 * 
	 * @return
	 */
	default MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	/**
	 * Sets the PV limit in [W].
	 * 
	 * @return
	 */
	public default WriteChannel<Integer> getActivePowerLimit() {
		return this.channel(ChannelId.ACTIVE_POWER_LIMIT);
	}
}
