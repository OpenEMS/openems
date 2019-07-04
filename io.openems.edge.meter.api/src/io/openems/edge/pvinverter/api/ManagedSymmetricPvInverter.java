package io.openems.edge.pvinverter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Represents a 3-Phase, symmetric PV-Inverter.
 */
public interface ManagedSymmetricPvInverter extends SymmetricMeter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds the maximum possible apparent power. This value is defined by the
		 * inverter limitations.
		 * 
		 * <ul>
		 * <li>Interface: SymmetricPvInverter
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: zero or positive value
		 * </ul>
		 */
		MAX_APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		/**
		 * Read/Set Active Power Limit.
		 * 
		 * <ul>
		 * <li>Interface: PV-Inverter Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(channel -> { //
					// on each Write to the channel -> set the value
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.setNextValue(value);
					});
				}));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the type of this Meter.
	 * 
	 * @return the MeterType
	 */
	default MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	/**
	 * Gets the Maximum Apparent Power in [VA], range "&gt;= 0".
	 * 
	 * @return the Channel
	 */
	default Channel<Integer> getMaxApparentPower() {
		return this.channel(ChannelId.MAX_APPARENT_POWER);
	}

	/**
	 * Gets the Active Power Limit in [W].
	 * 
	 * @return the Channel
	 */
	default IntegerWriteChannel getActivePowerLimit() {
		return this.channel(ChannelId.ACTIVE_POWER_LIMIT);
	}
}
