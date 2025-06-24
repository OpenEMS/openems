package io.openems.edge.heat.askoma;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatAskoma extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ANY_ERROR_OCCURRED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Any error occurred")), //
		TEMPERATURE_LIMIT_REACHED(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Temperature limit reached, heaters are switched off")), //
		PUMP_RELAY_FOLLOW_UP_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pump relay follow-up time is active")), //
		AUTO_HEATER_OFF_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Auto heater off is active")), //
		LOAD_FEEDIN_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Load feed-in value is active")), //
		LOAD_SETPOINT_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Load setpoint is active")), //
		ANALOG_INPUT_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Analog input 0-10V is active")), //
		LEGIONELLA_PROTECTION_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Legionella protection is active")), //
		EMERGENCY_MODE_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Emergency mode is active")), //
		HEAT_PUMP_REQUEST_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Heat pump request is active")), //
		HEATER_1_2_3_CURRENT_FLOW(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH) //
				.text("Heater 1-3 current flow")), //
		RELAYBOARD_IS_CONNECTED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Relayboard is connected")), //
		PUMP_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Pump is active")), //
		HEATER3_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Heater 3 is active")), //
		HEATER2_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Heater 2 is active")), //
		HEATER1_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Heater 1 is active")), //
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
	public default BooleanReadChannel getHeaterCurrentFlowChannel() {
		return this.channel(ChannelId.HEATER_1_2_3_CURRENT_FLOW);
	}

	/**
	 * Gets the state of the current heater flow See
	 * {@link ChannelId#HEATER_1_2_3_CURRENT_FLOW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getHeaterCurrentFlow() {
		return this.getHeaterCurrentFlowChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_LIMIT_REACHED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getTemperatureLimiteReachedChannel() {
		return this.channel(ChannelId.TEMPERATURE_LIMIT_REACHED);
	}

	/**
	 * Gets the state of the temperature limit See
	 * {@link ChannelId#TEMPERATURE_LIMIT_REACHED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getTemperatureLimiteReached() {
		return this.getTemperatureLimiteReachedChannel().value();
	}

}
