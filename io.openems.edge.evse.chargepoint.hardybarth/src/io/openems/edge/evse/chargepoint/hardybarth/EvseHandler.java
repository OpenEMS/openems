package io.openems.edge.evse.chargepoint.hardybarth;

import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.common.channel.ChannelUtils.setValue;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

import com.google.gson.JsonElement;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.function.BooleanConsumer;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.chargepoint.hardybarth.common.AbstractHardyBarthHandler;
import io.openems.edge.evse.chargepoint.hardybarth.common.LogVerbosity;
import io.openems.edge.meter.api.PhaseRotation;

public class EvseHandler extends AbstractHardyBarthHandler<EvseChargePointHardyBarthImpl> {

	public static final float SCALE_FACTOR_MINUS_1 = 0.1F;

	private int errorCounter = 0;

	public EvseHandler(EvseChargePointHardyBarthImpl parent, String ip, String apikey, PhaseRotation phaseRotation,
			LogVerbosity logVerbosity, BiConsumer<Logger, String> logInfo, BridgeHttpFactory httpBridgeFactory,
			HttpBridgeCycleServiceDefinition httpBridgeCycleServiceDefinition, BooleanConsumer communicationFailed) {
		super(parent, ip, apikey, phaseRotation, logVerbosity, logInfo, httpBridgeFactory,
				httpBridgeCycleServiceDefinition, communicationFailed);
	}

	@Override
	protected void setChannels(EvseChargePointHardyBarthImpl hb, JsonElement json, PhaseRotation phaseRotation,
			Integer currentL1, Integer currentL2, Integer currentL3, Integer activePower) {
		// STATUS
		final var status = getValueFromJson(STRING, json, value -> {
			final var stringValue = TypeUtils.<String>getAsType(STRING, value);
			if (stringValue == null) {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					return ChargePointStatus.UNDEFINED;
				}
			}

			final ChargePointStatus rawStatus = switch (stringValue) {
			case "A" -> ChargePointStatus.A;
			case "B" -> ChargePointStatus.B;
			case "C" -> ChargePointStatus.C;
			case "D" -> ChargePointStatus.D;
			case "E" -> {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					yield ChargePointStatus.E;
				}
				yield hb.getChargePointStatus();
			}
			case "F" -> {
				this.errorCounter++;
				if (this.errorCounter > 3) {
					yield ChargePointStatus.F;
				}
				yield hb.getChargePointStatus();

			}
			default -> {
				yield ChargePointStatus.UNDEFINED;
			}
			};
			if (!stringValue.equals("E") || !stringValue.equals("F")) {
				this.errorCounter = 0;
			}
			return rawStatus;
		}, "secc", "port0", "ci", "charge", "cp", "status");
		setValue(hb, EvseChargePointHardyBarth.ChannelId.STATUS, status);

		final var isReadyForCharging = switch (status) {
		case A, E, F, UNDEFINED -> false;
		case B, C, D -> true;
		};
		setValue(hb, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, isReadyForCharging);
	}
}
