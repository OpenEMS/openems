package io.openems.edge.evcs.hardybarth;

import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static java.lang.Math.round;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.function.BooleanConsumer;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evse.chargepoint.hardybarth.common.AbstractHardyBarthHandler;
import io.openems.edge.evse.chargepoint.hardybarth.common.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

public class EvcsHandler extends AbstractHardyBarthHandler<EvcsHardyBarthImpl> {

	private int errorCounter = 0;

	public EvcsHandler(EvcsHardyBarthImpl parent, String ip, String apikey, PhaseRotation phaseRotation,
			LogVerbosity logVerbosity, BiConsumer<Logger, String> logInfo, BridgeHttpFactory httpBridgeFactory,
			HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition, BooleanConsumer communicationFailed) {
		super(parent, ip, apikey, phaseRotation, logVerbosity, logInfo, httpBridgeFactory,
				httpBridgeCycleServiceDefinition, communicationFailed);
	}

	@Override
	protected void setChannels(EvcsHardyBarthImpl hb, JsonElement json, PhaseRotation phaseRotation, Integer currentL1,
			Integer currentL2, Integer currentL3, Integer activePower) {
		// Energy
		hb._setEnergySession(getValueFromJson(STRING, json, //
				value -> {
					if (value == null) {
						return null;
					}
					var chargedata = TypeUtils.<String>getAsType(STRING, value).split("\\|");
					if (chargedata.length == 3) {
						return round(TypeUtils.<Float>getAsType(FLOAT, chargedata[2]) * 1000);
					}
					return null;
				}, "secc", "port0", "salia", "chargedata"));

		// Phases: keep last value if no power value was given
		final var phases = evaluatePhaseCountFromCurrent(currentL1, currentL2, currentL3);
		if (phases != null) {
			hb._setPhases(phases);
			switch (this.logVerbosity) {
			case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
			case READS, WRITES -> this.logInfo("Used phases: " + phases);
			}
		}

		// STATUS
		final var status = getValueFromJson(STRING, json, value -> {
			var stringValue = TypeUtils.<String>getAsType(STRING, value);
			if (stringValue == null) {
				this.errorCounter++;
				switch (this.logVerbosity) {
				case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
				case READS, WRITES -> this.logInfo("Hardy Barth RAW_STATUS would be null! Raw value: " + value);
				}
				if (this.errorCounter > 3) {
					return Status.ERROR;
				}
				return hb.getStatus();
			}

			Status rawStatus = switch (stringValue) {
			case "A" -> Status.NOT_READY_FOR_CHARGING;
			case "B" -> {
				var tmpStatus = Status.READY_FOR_CHARGING;

				// Detect if the car is full
				if (!(hb.getSetChargePowerLimit().orElse(0) >= hb.getMinimumHardwarePower().orElse(0)
						&& activePower <= 0)) {
					// Charging rejected because we are forcing to pause charging
					if (hb.getSetChargePowerLimit().orElse(0) == 0) {
						tmpStatus = Status.CHARGING_REJECTED;
					}
				}
				yield tmpStatus;
			}
			case "C", "D" -> Status.CHARGING;
			case "E", "F" -> {
				this.errorCounter++;
				switch (this.logVerbosity) {
				case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
				case READS, WRITES -> this.logInfo("Hardy Barth RAW_STATUS would be an error! Raw value: " + stringValue
						+ " - Error counter: " + this.errorCounter);
				}
				if (this.errorCounter > 3) {
					yield Status.ERROR;
				}
				yield hb.getStatus();
			}
			default -> {
				switch (this.logVerbosity) {
				case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
				case READS, WRITES -> this.logInfo("State " + stringValue + " is not a valid state");
				}
				yield Status.UNDEFINED;
			}
			};

			if (!stringValue.equals("E") || !stringValue.equals("F")) {
				this.errorCounter = 0;
			}

			return rawStatus;
		}, "secc", "port0", "ci", "charge", "cp", "status");

		hb._setStatus(status);
	}
}