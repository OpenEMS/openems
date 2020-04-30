package io.openems.edge.battery.soltaro.cluster.versionc;

import java.util.Optional;
import java.util.Set;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.State;
import io.openems.edge.battery.soltaro.cluster.SoltaroCluster;
import io.openems.edge.battery.soltaro.cluster.enums.Rack;
import io.openems.edge.battery.soltaro.single.versionc.enums.PreChargeControl;
import io.openems.edge.battery.soltaro.versionc.SoltaroBatteryVersionC;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;

public interface ClusterVersionC extends //
		SoltaroBatteryVersionC, SoltaroCluster, Battery, //
		OpenemsComponent, EventHandler, ModbusSlave {

	/**
	 * Is the system stopped?
	 * 
	 * @return true if stopped
	 */
	public boolean isSystemStopped();

	/**
	 * Is the system running?
	 * 
	 * @return true if running
	 */
	public boolean isSystemRunning();

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
	Set<Rack> getRacks();

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
		MASTER_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Master PCS Control Failure")),
		MASTER_PCS_COMMUNICATION_FAILURE(Doc.of(Level.FAULT) //
				.text("Master PCS Communication Failure")),
		// Rack #1 cannot be paralleled to DC Bus reasons
		RACK_1_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 1 Level 2 Alarm")),
		RACK_1_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 PCS Control Failure")),
		RACK_1_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 Communication to Master BMS Failure")),
		RACK_1_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 1 Hardware Failure")),
		RACK_1_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 1 Too big circulating Current among clusters (>4A)")),
		RACK_1_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 1 Too big boltage difference among clusters (>50V)")),
		// Rack #2 cannot be paralleled to DC Bus reasons
		RACK_2_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 2 Level 2 Alarm")),
		RACK_2_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 PCS Control Failure")),
		RACK_2_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 Communication to Master BMS Failure")),
		RACK_2_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 2 Hardware Failure")),
		RACK_2_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 2 Too big circulating Current among clusters (>4A)")),
		RACK_2_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 2 Too big boltage difference among clusters (>50V)")),
		// Rack #3 cannot be paralleled to DC Bus reasons
		RACK_3_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 3 Level 2 Alarm")),
		RACK_3_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 PCS Control Failure")),
		RACK_3_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 Communication to Master BMS Failure")),
		RACK_3_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 3 Hardware Failure")),
		RACK_3_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 3 Too big circulating Current among clusters (>4A)")),
		RACK_3_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 3 Too big boltage difference among clusters (>50V)")),
		// Rack #4 cannot be paralleled to DC Bus reasons
		RACK_4_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 4 Level 2 Alarm")),
		RACK_4_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 PCS Control Failure")),
		RACK_4_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 Communication to Master BMS Failure")),
		RACK_4_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 4 Hardware Failure")),
		RACK_4_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 4 Too big circulating Current among clusters (>4A)")),
		RACK_4_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 4 Too big boltage difference among clusters (>50V)")),
		// Rack #5 cannot be paralleled to DC Bus reasons
		RACK_5_LEVEL_2_ALARM(Doc.of(Level.FAULT) //
				.text("Rack 5 Level 2 Alarm")),
		RACK_5_PCS_CONTROL_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 PCS Control Failure")),
		RACK_5_COMMUNICATION_TO_MASTER_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 Communication to Master BMS Failure")),
		RACK_5_HARDWARE_FAILURE(Doc.of(Level.FAULT) //
				.text("Rack 5 Hardware Failure")),
		RACK_5_OVER_CURRENT(Doc.of(Level.FAULT) //
				.text("Rack 5 Too big circulating Current among clusters (>4A)")),
		RACK_5_VOLTAGE_DIFFERENCE(Doc.of(Level.FAULT) //
				.text("Rack 5 Too big boltage difference among clusters (>50V)")),;

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
