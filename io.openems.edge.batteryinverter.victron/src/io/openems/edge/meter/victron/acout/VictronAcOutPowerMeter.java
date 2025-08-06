package io.openems.edge.meter.victron.acout;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;


public interface VictronAcOutPowerMeter extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ENERGY_FROM_AC_IN_1_TO_AC_OUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.WATT_HOURS)), //
		ENERGY_FROM_BATTERY_TO_AC_OUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.WATT_HOURS)), //

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
	 * Gets the Channel for AC-In -> AC-Out energy.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEnergyFromAcInToAcOutChannel() {
		return this.channel(ChannelId.ENERGY_FROM_AC_IN_1_TO_AC_OUT);
	}

	/**
	 * energy for AC-In -> AC-Out [Wh].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEnergyFromAcInToAcOut() {
		return this.getEnergyFromAcInToAcOutChannel().value();
	}		
	
	
	/**
	 * Gets the Channel for battery -> AC-Out energy.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEnergyFromBatteryToAcOutChannel() {
		return this.channel(ChannelId.ENERGY_FROM_BATTERY_TO_AC_OUT);
	}

	/**
	 * energy for battery -> AC-Out [Wh].
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEnergyFromBatteryToAcOut() {
		return this.getEnergyFromAcInToAcOutChannel().value();
	}		
	

}
