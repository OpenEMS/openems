package io.openems.edge.battery.soltaro.cluster.versionc;

import java.util.Optional;
import java.util.Set;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.common.enums.EmsBaudrate;
import io.openems.edge.battery.soltaro.common.enums.State;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatterySoltaroClusterVersionC extends //
		SoltaroCluster, Battery, //
		StartStoppable, OpenemsComponent, EventHandler, ModbusSlave {

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the common {@link PreChargeControl}. If all Racks share the same
	 * {@link PreChargeControl} state, that one is returned; otherwise
	 * Optional.empty.
	 *
	 * @return the {@link PreChargeControl} state of all Reacks; or empty if they
	 *         are different
	 */
	public Optional<PreChargeControl> getCommonPreChargeControl();

	/**
	 * Gets the active Racks.
	 *
	 * @return a set of Racks
	 */
	public Set<Rack> getRacks();

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for a Rack.
	 *
	 * @param <T>         the expected typed Channel
	 * @param rack        the {@link Rack}
	 * @param rackChannel the {@link RackChannel}
	 * @return the typed Channel
	 */
	public <T extends Channel<?>> T channel(Rack rack, RackChannel rackChannel);

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * EnumWriteChannels
		 */
		EMS_BAUDRATE(Doc.of(EmsBaudrate.values()) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * IntegerWriteChannels
		 */
		EMS_ADDRESS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		EMS_COMMUNICATION_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS) //
				.accessMode(AccessMode.READ_WRITE)), //

		/*
		 * StateChannels
		 */
		// Other Alarm Info
		ALARM_COMMUNICATION_TO_MASTER_BMS(Doc.of(Level.WARNING) //
				.text("Communication Failure to Master BMS")), //
		ALARM_COMMUNICATION_TO_SLAVE_BMS(Doc.of(Level.WARNING) //
				.text("Communication Failure to Slave BMS")), //
		ALARM_COMMUNICATION_SLAVE_BMS_TO_TEMP_SENSORS(Doc.of(Level.WARNING) //
				.text("Communication Failure between Slave BMS and Temperature Sensors")), //
		ALARM_SLAVE_BMS_HARDWARE(Doc.of(Level.WARNING) //
				.text("Slave BMS Hardware Failure")), //
		// Pre-Alarm
		PRE_ALARM_CELL_VOLTAGE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage High Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("Total Voltage High Pre-Alarm")), //
		PRE_ALARM_CHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Charge Current High Pre-Alarm")), //
		PRE_ALARM_CELL_VOLTAGE_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage Low Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_LOW(Doc.of(Level.INFO) //
				.text("Total Voltage Low Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_CURRENT_HIGH(Doc.of(Level.INFO) //
				.text("Discharge Current High Pre-Alarm")), //
		PRE_ALARM_CHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Charge Temperature High Pre-Alarm")), //
		PRE_ALARM_CHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Charge Temperature Low Pre-Alarm")), //
		PRE_ALARM_SOC_LOW(Doc.of(OpenemsType.BOOLEAN) //
				.text("State-Of-Charge Low Pre-Alarm")), //
		PRE_ALARM_TEMP_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Temperature Difference Too Big Pre-Alarm")), //
		PRE_ALARM_POWER_POLE_HIGH(Doc.of(Level.INFO) //
				.text("Power Pole Temperature High Pre-Alarm")), //
		PRE_ALARM_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Cell Voltage Difference Too Big Pre-Alarm")), //
		PRE_ALARM_INSULATION_FAIL(Doc.of(Level.INFO) //
				.text("Insulation Failure Pre-Alarm")), //
		PRE_ALARM_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.INFO) //
				.text("Total Voltage Difference Too Big Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_TEMP_HIGH(Doc.of(Level.INFO) //
				.text("Discharge Temperature High Pre-Alarm")), //
		PRE_ALARM_DISCHARGE_TEMP_LOW(Doc.of(Level.INFO) //
				.text("Discharge Temperature Low Pre-Alarm")), //
		// Alarm Level 1
		LEVEL1_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Discharge Temperature Low Alarm Level 1")), //
		LEVEL1_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Temperature High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Total Voltage Difference Too Big Alarm Level 1")), //
		LEVEL1_INSULATION_VALUE(Doc.of(Level.WARNING) //
				.text("Insulation Value Failure Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Cell Voltage Difference Too Big Alarm Level 1")), //
		LEVEL1_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Power Pole temperature too high Alarm Level 1")), //
		LEVEL1_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Temperature Difference Too Big Alarm Level 1")), //
		LEVEL1_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cell Charge Temperature Low Alarm Level 1")), //
		LEVEL1_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Stage-Of-Charge Low Alarm Level 1")), //
		LEVEL1_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Temperature High Alarm Level 1")), //
		LEVEL1_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Current High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Total Voltage Low Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cell Voltage Low Alarm Level 1")), //
		LEVEL1_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Current High Alarm Level 1")), //
		LEVEL1_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Total Voltage High Alarm Level 1")), //
		LEVEL1_CELL_VOLTAGE_HIGH(Doc.of(OpenemsType.BOOLEAN) //
				.text("Cell Voltage High Alarm Level 1")), //
		// Alarm Level 2
		LEVEL2_DISCHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Discharge Temperature Low Alarm Level 2")), //
		LEVEL2_DISCHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Temperature High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Total Voltage Difference Too Big Alarm Level 2")), //
		LEVEL2_INSULATION_VALUE(Doc.of(Level.WARNING) //
				.text("Insulation Value Failure Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Cell Voltage Difference Too Big Alarm Level 2")), //
		LEVEL2_POWER_POLE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Power Pole temperature too high Alarm Level 2")), //
		LEVEL2_TEMP_DIFF_TOO_BIG(Doc.of(Level.WARNING) //
				.text("Temperature Difference Too Big Alarm Level 2")), //
		LEVEL2_CHARGE_TEMP_LOW(Doc.of(Level.WARNING) //
				.text("Cell Charge Temperature Low Alarm Level 2")), //
		LEVEL2_SOC_LOW(Doc.of(Level.WARNING) //
				.text("Stage-Of-Charge Low Alarm Level 2")), //
		LEVEL2_CHARGE_TEMP_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Temperature High Alarm Level 2")), //
		LEVEL2_DISCHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Discharge Current High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Total Voltage Low Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_LOW(Doc.of(Level.WARNING) //
				.text("Cell Voltage Low Alarm Level 2")), //
		LEVEL2_CHARGE_CURRENT_HIGH(Doc.of(Level.WARNING) //
				.text("Charge Current High Alarm Level 2")), //
		LEVEL2_TOTAL_VOLTAGE_HIGH(Doc.of(Level.WARNING) //
				.text("Total Voltage High Alarm Level 2")), //
		LEVEL2_CELL_VOLTAGE_HIGH(Doc.of(Level.INFO) //
				.text("Cell Voltage High Alarm Level 2")), //

		/*
		 * EnumReadChannels
		 */
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //

		/*
		 * IntegerReadChannels
		 */
		ORIGINAL_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),

		/*
		 * StateChannels
		 */
		// Master BMS Alarm Registers
		MASTER_EMS_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Master EMS Communication Failure")),
		MASTER_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Master PCS Control Failure")),
		MASTER_PCS_COMMUNICATION_FAILURE(Doc.of(Level.WARNING) //
				.text("Master PCS Communication Failure")),
		// Rack #1 cannot be paralleled to DC Bus reasons
		RACK_1_LEVEL_2_ALARM(Doc.of(Level.WARNING) //
				.text("Rack 1 Level 2 Alarm")),
		RACK_1_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 1 PCS Control Failure")),
		RACK_1_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 1 Communication to Master BMS Failure")),
		RACK_1_HARDWARE_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 1 Hardware Failure")),
		RACK_1_OVER_CURRENT(Doc.of(Level.WARNING) //
				.text("Rack 1 Too big circulating Current among clusters (>4A)")),
		RACK_1_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.text("Rack 1 Too big boltage difference among clusters (>50V)")),
		// Rack #2 cannot be paralleled to DC Bus reasons
		RACK_2_LEVEL_2_ALARM(Doc.of(Level.WARNING) //
				.text("Rack 2 Level 2 Alarm")),
		RACK_2_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 2 PCS Control Failure")),
		RACK_2_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 2 Communication to Master BMS Failure")),
		RACK_2_HARDWARE_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 2 Hardware Failure")),
		RACK_2_OVER_CURRENT(Doc.of(Level.WARNING) //
				.text("Rack 2 Too big circulating Current among clusters (>4A)")),
		RACK_2_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.text("Rack 2 Too big boltage difference among clusters (>50V)")),
		// Rack #3 cannot be paralleled to DC Bus reasons
		RACK_3_LEVEL_2_ALARM(Doc.of(Level.WARNING) //
				.text("Rack 3 Level 2 Alarm")),
		RACK_3_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 3 PCS Control Failure")),
		RACK_3_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 3 Communication to Master BMS Failure")),
		RACK_3_HARDWARE_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 3 Hardware Failure")),
		RACK_3_OVER_CURRENT(Doc.of(Level.WARNING) //
				.text("Rack 3 Too big circulating Current among clusters (>4A)")),
		RACK_3_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.text("Rack 3 Too big boltage difference among clusters (>50V)")),
		// Rack #4 cannot be paralleled to DC Bus reasons
		RACK_4_LEVEL_2_ALARM(Doc.of(Level.WARNING) //
				.text("Rack 4 Level 2 Alarm")),
		RACK_4_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 4 PCS Control Failure")),
		RACK_4_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 4 Communication to Master BMS Failure")),
		RACK_4_HARDWARE_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 4 Hardware Failure")),
		RACK_4_OVER_CURRENT(Doc.of(Level.WARNING) //
				.text("Rack 4 Too big circulating Current among clusters (>4A)")),
		RACK_4_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.text("Rack 4 Too big boltage difference among clusters (>50V)")),
		// Rack #5 cannot be paralleled to DC Bus reasons
		RACK_5_LEVEL_2_ALARM(Doc.of(Level.WARNING) //
				.text("Rack 5 Level 2 Alarm")),
		RACK_5_PCS_CONTROL_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 5 PCS Control Failure")),
		RACK_5_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 5 Communication to Master BMS Failure")),
		RACK_5_HARDWARE_FAILURE(Doc.of(Level.WARNING) //
				.text("Rack 5 Hardware Failure")),
		RACK_5_OVER_CURRENT(Doc.of(Level.WARNING) //
				.text("Rack 5 Too big circulating Current among clusters (>4A)")),
		RACK_5_VOLTAGE_DIFFERENCE(Doc.of(Level.WARNING) //
				.text("Rack 5 Too big boltage difference among clusters (>50V)")),

		// OpenEMS Faults
		RUN_FAILED(Doc.of(Level.WARNING) //
				.text("Running the Logic failed")), //
		MAX_START_ATTEMPTS(Doc.of(Level.WARNING) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.WARNING) //
				.text("The maximum number of stop attempts failed")), //
		NUMBER_OF_MODULES_PER_TOWER(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Number Modules per Tower")), //
		NUMBER_OF_TOWERS(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Number of Towers")), //

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
}
