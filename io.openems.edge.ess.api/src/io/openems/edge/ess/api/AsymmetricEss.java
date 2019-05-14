package io.openems.edge.ess.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

@ProviderType
public interface AsymmetricEss extends SymmetricEss {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Active Power L1
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Active Power L2
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Active Power L3
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Reactive Power L1
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Reactive Power L2
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
		),
		/**
		 * Reactive Power L3
		 * 
		 * <ul>
		 * <li>Interface: Ess Asymmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
		);

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(AsymmetricEss.class, accessMode, 100) //
				.channel(0, ChannelId.ACTIVE_POWER_L1, ModbusType.FLOAT32) //
				.channel(2, ChannelId.ACTIVE_POWER_L2, ModbusType.FLOAT32) //
				.channel(4, ChannelId.ACTIVE_POWER_L3, ModbusType.FLOAT32) //
				.build();
	}

	/**
	 * Gets the Active Power on L1 in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL1() {
		return this.channel(ChannelId.ACTIVE_POWER_L1);
	}

	/**
	 * Gets the Active Power on L2 in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL2() {
		return this.channel(ChannelId.ACTIVE_POWER_L2);
	}

	/**
	 * Gets the Active Power on L3 in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePowerL3() {
		return this.channel(ChannelId.ACTIVE_POWER_L3);
	}

	/**
	 * Gets the Reactive Power on L1 in [var]. Negative values for Charge; positive
	 * for Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL1() {
		return this.channel(ChannelId.REACTIVE_POWER_L1);
	}

	/**
	 * Gets the Reactive Power on L2 in [var]. Negative values for Charge; positive
	 * for Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL2() {
		return this.channel(ChannelId.REACTIVE_POWER_L2);
	}

	/**
	 * Gets the Reactive Power on L3 in [var]. Negative values for Charge; positive
	 * for Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePowerL3() {
		return this.channel(ChannelId.REACTIVE_POWER_L3);
	}

	/**
	 * Initializes Channel listeners to set the Active- and Reactive-Power Channel
	 * value as the sum of L1 + L2 + L3.
	 * 
	 * @param ess
	 */
	public static void initializePowerSumChannels(AsymmetricEss ess) {
		// Active Power
		final Consumer<Value<Integer>> activePowerSum = ignore -> {
			ess.getActivePower().setNextValue(TypeUtils.sum(//
					ess.getActivePowerL1().value().get(), //
					ess.getActivePowerL2().value().get(), //
					ess.getActivePowerL3().value().get()));
		};
		ess.getActivePowerL1().onSetNextValue(activePowerSum);
		ess.getActivePowerL2().onSetNextValue(activePowerSum);
		ess.getActivePowerL3().onSetNextValue(activePowerSum);

		// Reactive Power
		final Consumer<Value<Integer>> reactivePowerSum = ignore -> {
			ess.getReactivePower().setNextValue(TypeUtils.sum(//
					ess.getReactivePowerL1().value().get(), //
					ess.getReactivePowerL2().value().get(), //
					ess.getReactivePowerL3().value().get()));
		};
		ess.getReactivePowerL1().onSetNextValue(reactivePowerSum);
		ess.getReactivePowerL2().onSetNextValue(reactivePowerSum);
		ess.getReactivePowerL3().onSetNextValue(reactivePowerSum);
	}
}
