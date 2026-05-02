package io.openems.edge.evse.chargepoint.mennekes.common;

import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Mennekes Amtron Professional charging protocol interface.
 * 
 * <p>
 * Defines the interface for Mennekes Amtron Professional
 */
public interface Mennekes extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Apply charge current limit.
		 * 
		 * <p>
		 * WriteChannel for the modbus register to apply the charge power given by the
		 * applyChargePowerLimit method
		 */
		SET_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.accessMode(WRITE_ONLY)), //

		SET_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(WATT)//
				.accessMode(WRITE_ONLY)), //

		EMS_CURRENT_LIMIT(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		
		HEMS_MIN_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //

		HEMS_MAX_POWER(Doc.of(INTEGER)//
				.unit(WATT)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_POWER_LIMIT}.
	 * Used for EVSE.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyPowerLimitChannel() {
		return this.channel(ChannelId.SET_POWER_LIMIT);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#SET_CURRENT_LIMIT}.
	 * Used for EVCS.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getApplyCurrentLimitChannel() {
		return this.channel(ChannelId.SET_CURRENT_LIMIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEMS_MIN_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHemsMinPowerChannel() {
		return this.channel(ChannelId.HEMS_MIN_POWER);
	}
	
	/**
	 * Gets the minimum power value of the charging station.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHemsMinPower() {
		return this.getHemsMinPowerChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#HEMS_MAX_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHemsMaxPowerChannel() {
		return this.channel(ChannelId.HEMS_MAX_POWER);
	}
	
	/**
	 * Gets the maximum power value of the charging station.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHemsMaxPower() {
		return this.getHemsMaxPowerChannel().value();
	}
}
