package io.openems.edge.ess.stabl;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.stabl.enums.ActivatePowerStage;
import io.openems.edge.ess.stabl.enums.ActiveGridType;
import io.openems.edge.ess.stabl.enums.ActualMainState;

public interface EssStabl extends OpenemsComponent, ManagedSymmetricEss, SymmetricEss, ModbusComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		COM_TIME_OUT_EMS(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.SECONDS)), // default 120 seconds, 0 deactivate the time out
		SOFTWARE_RESET(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		STANDBY_MODE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		ACTIVATE_POWER_STAGE(Doc.of(OpenemsType.INTEGER)// Doc.of(ActivatePowerStage.values())//
				.accessMode(AccessMode.READ_WRITE)), //
		RESET_ALARM_FLAG(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		GRID_TYPE(new IntegerDoc()//
				.accessMode(AccessMode.READ_WRITE) //
				.<EssStabl>onChannelChange((self, value) -> {
					final GridMode gridMode;
					if (!value.isDefined()) {
						gridMode = GridMode.ON_GRID;
					} else if (value.get() == 0) {
						gridMode = GridMode.ON_GRID;
					} else {
						gridMode = GridMode.ON_GRID;
					}
					self.getGridModeChannel().setNextValue(gridMode);
				})), //
		GRID_CODE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		SYSTEM_SIGNED_POWER_SET_POINT_AC(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.KILOVOLT_AMPERE)),
		COS_PHI_SET_POINT_AC(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)),
		ISLAND_FREQUENCY_OFFSET(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //

		TEMPERATURE_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MIN_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_MAX_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //
		TEMPERATURE_AVG_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS)), //

		VOLTAGE_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MIN_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_MAX_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //
		VOLTAGE_AVG_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT)), //

		CURRENT_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MIN_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_MAX_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //
		CURRENT_AVG_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE)), //

		SOC_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MIN_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_MAX_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOC_AVG_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //

		SOH_MIN_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MIN_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_MAX_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_SYSTEM(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_1_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_2(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_2_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_3(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //
		SOH_AVG_STRING_3_MODULE_X(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)), //

		ACTIVE_POWER_M0D_SETPOINT_STRING_1_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.WATT)),
		ACTIVE_POWER_M0D_SETPOINT_STRING_2_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.WATT)),
		ACTIVE_POWER_M0D_SETPOINT_STRING_3_MODULEX(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.WRITE_ONLY)//
				.unit(Unit.WATT)),

		CURRENT_LIMIT_DISCHARGE_SYSTEM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_DISCHARGE_SYSTEM_1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_DISCHARGE_SYSTEM_2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_DISCHARGE_SYSTEM_3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //

		CURRENT_LIMIT_CHARGE_SYSTEM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_CHARGE_SYSTEM_1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_CHARGE_SYSTEM_2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_LIMIT_CHARGE_SYSTEM_3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //

		ACTUAL_MAIN_STATE(Doc.of(ActualMainState.values())), //

		NUMBER_OF_CONNECTED_STABLE_MODULES_1(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY)//
				.onChannelChange(EssStablImpl::initializeAllChannelsModule1)//
				.text("Total number modules in String 1")), //
		NUMBER_OF_CONNECTED_STABLE_MODULES_2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY)//
				.onChannelChange(EssStablImpl::initializeAllChannelsModule2)//
				.text("Total number modules in String 2")),
		NUMBER_OF_CONNECTED_STABLE_MODULES_3(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY)//
				.onChannelChange(EssStablImpl::initializeAllChannelsModule3)//
				.text("Total number modules in String 3")),

		AC_DC_ACTIVE_GRID_TYPE(Doc.of(ActiveGridType.values())), //

		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //

		STABL_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),
		STABL_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),
		STABL_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)),

		INLET_AIR_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MCU_CORE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //

		MAINTAIN_MODE(Doc.of(OpenemsType.INTEGER)), //

		TOTAL_WARNING_CNT(Doc.of(OpenemsType.INTEGER)), //
		WARNING1(Doc.of(Level.WARNING)), //
		WARNING2(Doc.of(Level.WARNING)), //
		WARNING3(Doc.of(Level.WARNING)), //
		WARNING4(Doc.of(Level.WARNING)), //
		WARNING5(Doc.of(Level.WARNING)), //
		WARNING6(Doc.of(Level.WARNING)), //
		WARNING7(Doc.of(Level.WARNING)), //
		WARNING8(Doc.of(Level.WARNING)), //
		WARNING9(Doc.of(Level.WARNING)), //
		WARNING10(Doc.of(Level.WARNING)), //
		WARNING11(Doc.of(Level.WARNING)), //
		WARNING12(Doc.of(Level.WARNING)), //
		WARNING13(Doc.of(Level.WARNING)), //
		WARNING14(Doc.of(Level.WARNING)), //
		WARNING15(Doc.of(Level.WARNING)), //
		WARNING16(Doc.of(Level.WARNING)), //
		WARNING17(Doc.of(Level.WARNING)), //
		WARNING18(Doc.of(Level.WARNING)), //
		WARNING19(Doc.of(Level.WARNING)), //
		WARNING20(Doc.of(Level.WARNING)), //

		TOTAL_ALARMS_CNT(Doc.of(OpenemsType.INTEGER)), //
		ALARM1(Doc.of(Level.FAULT)), //
		ALARM2(Doc.of(Level.FAULT)), //
		ALARM3(Doc.of(Level.FAULT)), //
		ALARM4(Doc.of(Level.FAULT)), //
		ALARM5(Doc.of(Level.FAULT)), //
		ALARM6(Doc.of(Level.FAULT)), //
		ALARM7(Doc.of(Level.FAULT)), //
		ALARM8(Doc.of(Level.FAULT)), //
		ALARM9(Doc.of(Level.FAULT)), //
		ALARM10(Doc.of(Level.FAULT)), //
		ALARM11(Doc.of(Level.FAULT)), //
		ALARM12(Doc.of(Level.FAULT)), //
		ALARM13(Doc.of(Level.FAULT)), //
		ALARM14(Doc.of(Level.FAULT)), //
		ALARM15(Doc.of(Level.FAULT)), //
		ALARM16(Doc.of(Level.FAULT)), //
		ALARM17(Doc.of(Level.FAULT)), //
		ALARM18(Doc.of(Level.FAULT)), //
		ALARM19(Doc.of(Level.FAULT)), //
		ALARM20(Doc.of(Level.FAULT)), //

		NS_PROTECTION_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //

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
	 * Gets the Channel for {@link ChannelId#NUMBER_OF_CONNECTED_STABLE_MODULES_1}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getNumberOfConnectedStableModules1Channel() {
		return this.channel(ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_1);
	}

	/**
	 * Number of connected Stabl modules in String 1.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfConnectedStableModules1() {
		return this.getNumberOfConnectedStableModules1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#NUMBER_OF_CONNECTED_STABLE_MODULES_2}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getNumberOfConnectedStableModules2Channel() {
		return this.channel(ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_2);
	}

	/**
	 * Number of connected Stabl modules in String 2.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfConnectedStableModules2() {
		return this.getNumberOfConnectedStableModules2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#NUMBER_OF_CONNECTED_STABLE_MODULES_3}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getNumberOfConnectedStableModules3Channel() {
		return this.channel(ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_3);
	}

	/**
	 * Number of connected Stabl modules in String 3.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getNumberOfConnectedStableModules3() {
		return this.getNumberOfConnectedStableModules3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVATE_POWER_STAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ActivatePowerStage> getActivePowerStageChannel() {
		return this.channel(ChannelId.ACTIVATE_POWER_STAGE);
	}

	/**
	 * Gets the {@link ActivatePowerStage} for
	 * {@link ChannelId#ACTIVATE_POWER_STAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<ActivatePowerStage> getActivePowerStage() {
		return this.getActivePowerStageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ACTIVATE_POWER_STAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void setActivePowerStage(ActivatePowerStage value) {
		this.getActivePowerStageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTUAL_MAIN_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ActualMainState> getActualMainStateChannel() {
		return this.channel(ChannelId.ACTUAL_MAIN_STATE);
	}

	/**
	 * Gets the {@link ActualMainState} for {@link ChannelId#ACTUAL_MAIN_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ActualMainState getActualMainState() {
		return this.getActualMainStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_LIMIT_DISCHARGE_SYSTEM}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentLimitDischargeSystemChannel() {
		return this.channel(ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM);
	}

	/**
	 * System-level discharge current limit in Ampere.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getCurrentLimitDischargeSystem() {
		return this.getCurrentLimitDischargeSystemChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_LIMIT_CHARGE_SYSTEM}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentLimitChargeSystemChannel() {
		return this.channel(ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM);
	}

	/**
	 * System-level charge current limit in Ampere.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getCurrentLimitChargeSystem() {
		return this.getCurrentLimitChargeSystemChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STABL_ACTIVE_POWER_L1}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssStablActivePowerL1Channel() {
		return this.channel(ChannelId.STABL_ACTIVE_POWER_L1);
	}

	/**
	 * Active power on L1 in Watt.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getEssStablActivePowerL1() {
		return this.getEssStablActivePowerL1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STABL_ACTIVE_POWER_L2}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssStablActivePowerL2Channel() {
		return this.channel(ChannelId.STABL_ACTIVE_POWER_L2);
	}

	/**
	 * Active power on L2 in Watt.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getEssStablActivePowerL2() {
		return this.getEssStablActivePowerL2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STABL_ACTIVE_POWER_L3}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getEssStablActivePowerL3Channel() {
		return this.channel(ChannelId.STABL_ACTIVE_POWER_L3);
	}

	/**
	 * Active power on L3 in Watt.
	 *
	 * @return the channel {@link Value}
	 */
	public default Value<Integer> getEssStablActivePowerL3() {
		return this.getEssStablActivePowerL3Channel().value();
	}
}