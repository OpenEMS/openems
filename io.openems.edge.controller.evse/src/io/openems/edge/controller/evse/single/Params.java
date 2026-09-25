package io.openems.edge.controller.evse.single;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Clock;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.controller.evse.single.Types.History;
import io.openems.edge.controller.evse.single.Types.Hysteresis;
import io.openems.edge.controller.evse.single.Types.Payload;

/**
 * Parameters of one Evse.Controller.Single. Contains configuration settings,
 * runtime parameters and CombinedAbilities of Charge-Point and
 * Electric-Vehicle.
 */
public record Params(//
		/**
		 * Unique Component-ID of Evse.Controller.Single.
		 */
		String ctrlSingleId,
		/**
		 * Unique Component-ID of the EvseChargePoint.
		 */
		String chargePointId,
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
		 * The configured Session-Energy Limit; possibly null.
		 */
		Integer sessionEnergyLimit, //
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
		 * Externally requested maximum charge power in [W]; possibly null.
		 */
		Integer externalMaximumChargePower, //
		/**
		 * Externally requested charging enable flag; possibly null.
		 */
		Boolean externalChargingEnabled, //
		/**
		 * JSCalendar configuration.
		 */
		JSCalendar.Tasks<Payload> tasks) {

	public Params(String ctrlSingleId, String chargePointId, Mode mode, Integer activePower, int sessionEnergy,
			Integer sessionEnergyLimit, History history, PhaseSwitching phaseSwitching,
			CombinedAbilities combinedAbilities, JSCalendar.Tasks<Payload> tasks) {
		this(ctrlSingleId, chargePointId, mode, activePower, sessionEnergy, sessionEnergyLimit, history,
				phaseSwitching, combinedAbilities, null, null, tasks);
	}

	public Params(String ctrlSingleId, String chargePointId, Mode mode, Integer activePower, int sessionEnergy,
			Integer sessionEnergyLimit, History history, PhaseSwitching phaseSwitching,
			CombinedAbilities combinedAbilities, Integer externalMaximumChargePower, Boolean externalChargingEnabled,
			JSCalendar.Tasks<Payload> tasks) {
		this(ctrlSingleId, chargePointId, mode, activePower, sessionEnergy, sessionEnergyLimit, history,
				Hysteresis.from(history), phaseSwitching, history.getAppearsToBeFullyCharged(), combinedAbilities,
				externalMaximumChargePower, externalChargingEnabled, tasks);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link EshConfig}.
	 *
	 * @param clock the {@link Clock}
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Params> serializer(Clock clock) {
		return jsonObjectSerializer(json -> {
			return new Params(//
					json.getString("ctrlSingleId"), //
					json.getString("chargePointId"), //
					json.getEnum("mode", Mode.class), //
					json.getOptionalInt("activePower").orElse(null), //
					json.getInt("sessionEnergy"), //
					json.getOptionalInt("sessionEnergyLimit").orElse(null), //
					new History(), // TODO
					json.getEnum("phaseSwitching", PhaseSwitching.class), //
					json.getObject("combinedAbilities", CombinedAbilities.serializer()), //
					json.getOptionalInt("externalMaximumChargePower").orElse(null), //
					json.getOptionalBoolean("externalChargingEnabled").orElse(null), //
					json.getObject("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()))); //
		}, obj -> {
			return buildJsonObject() //
					.addProperty("ctrlSingleId", obj.ctrlSingleId) //
					.addProperty("chargePointId", obj.chargePointId) //
					.addProperty("mode", obj.mode) //
					.addProperty("activePower", obj.activePower) //
					.addProperty("sessionEnergy", obj.sessionEnergy) //
					.addProperty("sessionEnergyLimit", obj.sessionEnergyLimit) //
					.addProperty("history", "") // TODO
					.addProperty("phaseSwitching", obj.phaseSwitching) //
					.add("combinedAbilities", CombinedAbilities.serializer().serialize(obj.combinedAbilities)) //
					.addProperty("externalMaximumChargePower", obj.externalMaximumChargePower) //
					.addProperty("externalChargingEnabled", obj.externalChargingEnabled) //
					.add("tasks", JSCalendar.Tasks.serializer(clock, Payload.serializer()).serialize(obj.tasks)) //
					.build();
		});
	}
}
