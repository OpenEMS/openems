package io.openems.edge.pytes.internalmeterbackup;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Unit;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface PytesInternalMeterBackupPort extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Backup side Phase A apparent power (reg 33523, S16)
		 * Calculated using L1-N voltage and L1 current.
		 * Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
		 * Unit: VA
		 */
		APPARENT_POWER_L1(Doc.of(INTEGER) //
					.accessMode(READ_ONLY) //
					.unit(Unit.VOLT_AMPERE)),

		/**
		 * Backup side Phase B apparent power (reg 33523, S16)
		 * Calculated using L1-N voltage and L1 current.
		 * Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
		 * Unit: VA
		 */
		APPARENT_POWER_L2(Doc.of(INTEGER) //
					.accessMode(READ_ONLY) //
					.unit(Unit.VOLT_AMPERE)),

		/**
		 * Backup side Phase C apparent power (reg 33523, S16)
		 * Calculated using L1-N voltage and L1 current.
		 * Datasheet: 10 VA -> SCALE_FACTOR_1 -> VA
		 * Unit: VA
		 */
		APPARENT_POWER_L3(Doc.of(INTEGER) //
					.accessMode(READ_ONLY) //
					.unit(Unit.VOLT_AMPERE)),


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
	 * Channel for {@link ChannelId#APPARENT_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL1Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L1);
	}

	/**
	 * Phase A apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL1() {
		return this.getApparentPowerL1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#APPARENT_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL2Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L2);
	}

	/**
	 * Phase B apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL2() {
		return this.getApparentPowerL2Channel().value();
	}


	/**
	 * Channel for {@link ChannelId#APPARENT_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL3Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L3);
	}

	/**
	 * Phase C apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL3() {
		return this.getApparentPowerL3Channel().value();
	}


}
