package io.openems.edge.evcs.ocpp.server;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.feature.profile.ServerFirmwareManagementEventHandler;
import eu.chargetime.ocpp.model.firmware.DiagnosticsStatusNotificationConfirmation;
import eu.chargetime.ocpp.model.firmware.DiagnosticsStatusNotificationRequest;
import eu.chargetime.ocpp.model.firmware.FirmwareStatusNotificationConfirmation;
import eu.chargetime.ocpp.model.firmware.FirmwareStatusNotificationRequest;

public class FirmwareManagementEventHandlerImpl implements ServerFirmwareManagementEventHandler {

	private final Logger log = LoggerFactory.getLogger(FirmwareManagementEventHandlerImpl.class);
	private final EvcsOcppServer parent;

	public FirmwareManagementEventHandlerImpl(EvcsOcppServer parent) {
		this.parent = parent;
	}

	@Override
	public DiagnosticsStatusNotificationConfirmation handleDiagnosticsStatusNotificationRequest(UUID sessionIndex,
			DiagnosticsStatusNotificationRequest request) {
		this.parent.logInfo(this.log, "Handle DiagnosticsStatusNotificationRequest");

		return new DiagnosticsStatusNotificationConfirmation();
	}

	@Override
	public FirmwareStatusNotificationConfirmation handleFirmwareStatusNotificationRequest(UUID sessionIndex,
			FirmwareStatusNotificationRequest request) {
		this.parent.logInfo(this.log, "Handle FirmwareStatusNotificationRequest");

		return new FirmwareStatusNotificationConfirmation();
	}
}
