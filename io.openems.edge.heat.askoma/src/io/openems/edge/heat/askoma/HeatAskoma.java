package io.openems.edge.heat.askoma;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heat.askoma.statemachine.StateMachine;

public interface HeatAskoma extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ANY_ERROR_OCCURRED(Doc.of(OpenemsType.BOOLEAN)//
				.text("Any error occurred")), //
		TEMPERATURE_LIMIT_REACHED(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Temperature limit reached, heaters are switched off")), //
		PUMP_RELAY_FOLLOW_UP_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Pump relay follow-up time is active")), //
		AUTO_HEATER_OFF_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Auto heater off is active")), //
		LOAD_FEEDIN_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Load feed-in value is active")), //
		LOAD_SETPOINT_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Load setpoint is active")), //
		ANALOG_INPUT_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Analog input 0-10V is active")), //
		LEGIONELLA_PROTECTION_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Legionella protection is active")), //
		EMERGENCY_MODE_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Emergency mode is active")), //
		HEAT_PUMP_REQUEST_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Heat pump request is active")), //
		HEATER_1_2_3_CURRENT_FLOW(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Heater 1-3 current flow")), //
		RELAYBOARD_IS_CONNECTED(Doc.of(OpenemsType.BOOLEAN)//
				.text("Relayboard is connected")), //
		PUMP_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Pump is active")), //
		HEATER3_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Heater 3 is active")), //
		HEATER2_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Heater 2 is active")), //
		HEATER1_ACTIVE(Doc.of(OpenemsType.BOOLEAN)//
				.text("Heater 1 is active")), //
		MODE(Doc.of(ChannelMode.values())//
				.text("Current mode of the device")//
				.persistencePriority(PersistencePriority.HIGH)), //
		STATE_MACHINE(Doc.of(StateMachine.State.values())//
				.text("Current state-machine state")//
				.persistencePriority(PersistencePriority.HIGH)), //
		FAST_HEAT_POWER_NOT_APPLIED(Doc.of(Level.WARNING)//
				.text("Fast Heat power not applied")), //

		/**
		 * Target temperature (SOLL) read from Modbus register 597
		 * (MODBUS_CON_TEMPERATURE_LOAD_SETPOINT).
		 *
		 * <ul>
		 * <li>Interface: HeatAskoma
		 * <li>Type: Integer
		 * <li>Unit: Deci-Degree Celsius
		 * </ul>
		 */
		TEMPERATURE_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)//
				.text("Target temperature setpoint")), //
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
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_LIMIT_REACHED}.
	 *
	 * @return the Channel
	 */
	default BooleanReadChannel getHeaterCurrentFlowChannel() {
		return this.channel(ChannelId.HEATER_1_2_3_CURRENT_FLOW);
	}

	/**
	 * Gets the state of the current heater flow See
	 * {@link ChannelId#HEATER_1_2_3_CURRENT_FLOW}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getHeaterCurrentFlow() {
		return this.getHeaterCurrentFlowChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_LIMIT_REACHED}.
	 *
	 * @return the Channel
	 */
	default BooleanReadChannel getTemperatureLimiteReachedChannel() {
		return this.channel(ChannelId.TEMPERATURE_LIMIT_REACHED);
	}

	/**
	 * Gets the state of the temperature limit See
	 * {@link ChannelId#TEMPERATURE_LIMIT_REACHED}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getTemperatureLimiteReached() {
		return this.getTemperatureLimiteReachedChannel().value();
	}

}
