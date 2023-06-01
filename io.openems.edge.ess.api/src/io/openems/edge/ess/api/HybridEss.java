package io.openems.edge.ess.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

/**
 * A {@link HybridEss} is a {@link SymmetricEss} with one or more
 * {@link EssDcCharger}s.
 */
@ProviderType
public interface HybridEss extends SymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * DC Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: HybridEss
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * <li>This is the
		 * {@link io.openems.edge.ess.api.SymmetricEss.ChannelId#ACTIVE_POWER} minus
		 * {@link io.openems.edge.ess.dccharger.api.EssDcCharger.ChannelId#ACTUAL_POWER},
		 * i.e. the power that is actually charged to or discharged from the battery.
		 * </ul>
		 */
		DC_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Actual AC-side battery discharge power of Energy Storage System. " //
						+ "Negative values for charge; positive for discharge")),
		/**
		 * DC Charge Energy.
		 *
		 * <ul>
		 * <li>Interface: HybridEss
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		DC_CHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)), //
		/**
		 * DC Discharge Energy.
		 *
		 * <ul>
		 * <li>Interface: HybridEss
		 * <li>Type: Long
		 * <li>Unit: Wh
		 * </ul>
		 */
		DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)); //

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
	 * Gets the Surplus Power of the {@link EssDcCharger}s of this
	 * {@link HybridEss}.
	 *
	 * <p>
	 * This value is usually calculated from the
	 * {@link EssDcCharger#getActualPower()} when the battery is full. It is used by
	 * the Ess.Hybrid.Surplus-Feed-To-Grid Controller to feed the surplus power to
	 * grid.
	 *
	 * @return the surplus power, or 'null' if there is no surplus power
	 */
	public Integer getSurplusPower();

	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargePower(Integer value) {
		this.getDcDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_CHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getDcChargeEnergyChannel() {
		return this.channel(ChannelId.DC_CHARGE_ENERGY);
	}

	/**
	 * Gets the DC Charge Energy in [Wh]. See {@link ChannelId#DC_CHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getDcChargeEnergy() {
		return this.getDcChargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_CHARGE_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcChargeEnergy(Long value) {
		this.getDcChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_CHARGE_ENERGY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcChargeEnergy(long value) {
		this.getDcChargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getDcDischargeEnergyChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_ENERGY);
	}

	/**
	 * Gets the DC Discharge Energy in [Wh]. See
	 * {@link ChannelId#DC_DISCHARGE_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getDcDischargeEnergy() {
		return this.getDcDischargeEnergyChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargeEnergy(Long value) {
		this.getDcDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_ENERGY} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargeEnergy(long value) {
		this.getDcDischargeEnergyChannel().setNextValue(value);
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(HybridEss.class, accessMode, 100) //
				.channel(0, ChannelId.DC_DISCHARGE_POWER, ModbusType.UINT16) //
				.build();
	}
}
