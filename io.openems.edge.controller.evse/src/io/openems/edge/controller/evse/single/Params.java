package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.single.Types.Payload;
import io.openems.edge.evse.api.chargepoint.Mode;

/**
 * Parameters of one Evse.Controller.Single. Contains configuration settings,
 * runtime parameters and CombinedAbilities of Charge-Point and
 * Electric-Vehicle.
 */
public record Params(//
		/**
		 * Unique Component-ID of Evse.Controller.Single.
		 */
		String componentId,
		/**
		 * Mode configuration of Evse.Controller.Single.
		 */
		Mode mode, //
		/**
		 * The measured ActivePower; possibly null.
		 */
		Integer activePower, //
		/**
		 * The recorded Session-Energy.
		 */
		int sessionEnergy, //
		/**
		 * The configured Session-Energy Limit; 0 is no limit.
		 */
		int sessionEnergyLimit, //
		/**
		 * History data
		 */
		History history, //
		/**
		 * Hysteresis data
		 */
		Hysteresis hysteresis, //
		/**
		 * PhaseSwitching configuration of Evse.Controller.Single.
		 */
		PhaseSwitching phaseSwitching, //
		/**
		 * EV appears to be fully charged.
		 */
		boolean appearsToBeFullyCharged, //
		/**
		 * The CombinedAbilities of Charge-Point and Electric-Vehicle.
		 */
		CombinedAbilities combinedAbilities, //
		/**
		 * Smart-Mode configuration.
		 */
		JSCalendar.Tasks<Payload> smartConfig) {

	public Params(String componentId, Mode mode, Integer activePower, int sessionEnergy, int sessionEnergyLimit,
			History history, PhaseSwitching phaseSwitching, CombinedAbilities combinedAbilities,
			JSCalendar.Tasks<Payload> smartConfig) {
		this(componentId, mode, activePower, sessionEnergy, sessionEnergyLimit, history, Hysteresis.from(history),
				phaseSwitching, history.getAppearsToBeFullyCharged(), combinedAbilities, smartConfig);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link EshConfig}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Params> serializer() {
		return jsonObjectSerializer(json -> {
			return new Params(//
					json.getString("componentId"), //
					json.getEnum("mode", Mode.class), //
					json.getOptionalInt("activePower").orElse(null), //
					json.getInt("sessionEnergy"), //
					json.getInt("sessionEnergyLimit"), //
					new History(), // TODO
					json.getEnum("phaseSwitching", PhaseSwitching.class), //
					json.getObject("combinedAbilities", CombinedAbilities.serializer()), //
					json.getObject("smartConfig", JSCalendar.Tasks.serializer(Payload.serializer()))); //
		}, obj -> {
			return buildJsonObject() //
					.addProperty("componentId", obj.componentId) //
					.addProperty("mode", obj.mode) //
					.addProperty("activePower", obj.activePower) //
					.addProperty("sessionEnergy", obj.sessionEnergy) //
					.addProperty("sessionEnergyLimit", obj.sessionEnergyLimit) //
					.addProperty("history", "") // TODO
					.addProperty("phaseSwitching", obj.phaseSwitching) //
					.add("combinedAbilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
					.add("smartConfig", JSCalendar.Tasks.serializer(Payload.serializer()).serialize(obj.smartConfig)) //
					.build();
		});
	}
}
