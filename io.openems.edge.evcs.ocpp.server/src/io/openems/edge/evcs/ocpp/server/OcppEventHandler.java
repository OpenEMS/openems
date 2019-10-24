package io.openems.edge.evcs.ocpp.server;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.feature.profile.ServerCoreEventHandler;
import eu.chargetime.ocpp.model.core.AuthorizationStatus;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.AuthorizeRequest;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.BootNotificationRequest;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import eu.chargetime.ocpp.model.core.DataTransferConfirmation;
import eu.chargetime.ocpp.model.core.DataTransferRequest;
import eu.chargetime.ocpp.model.core.DataTransferStatus;
import eu.chargetime.ocpp.model.core.HeartbeatConfirmation;
import eu.chargetime.ocpp.model.core.HeartbeatRequest;
import eu.chargetime.ocpp.model.core.IdTagInfo;
import eu.chargetime.ocpp.model.core.MeterValue;
import eu.chargetime.ocpp.model.core.MeterValuesConfirmation;
import eu.chargetime.ocpp.model.core.MeterValuesRequest;
import eu.chargetime.ocpp.model.core.RegistrationStatus;
import eu.chargetime.ocpp.model.core.SampledValue;
import eu.chargetime.ocpp.model.core.StartTransactionConfirmation;
import eu.chargetime.ocpp.model.core.StartTransactionRequest;
import eu.chargetime.ocpp.model.core.StatusNotificationConfirmation;
import eu.chargetime.ocpp.model.core.StatusNotificationRequest;
import eu.chargetime.ocpp.model.core.StopTransactionConfirmation;
import eu.chargetime.ocpp.model.core.StopTransactionRequest;
import eu.chargetime.ocpp.model.core.ValueFormat;
import io.openems.edge.evcs.api.OcppEvcs;
import io.openems.edge.evcs.api.Status;

public class OcppEventHandler implements ServerCoreEventHandler {

	private final Logger log = LoggerFactory.getLogger(OcppEventHandler.class);
	private OcppServer parent;

	public OcppEventHandler(OcppServer parent) {
		this.parent = parent;
	}

	@Override
	public AuthorizeConfirmation handleAuthorizeRequest(UUID sessionIndex, AuthorizeRequest request) {

		parent.logInfo(this.log, "Handle AuthorizeRequest: " + request);

		AuthorizeConfirmation response = new AuthorizeConfirmation();
		IdTagInfo tag = new IdTagInfo();

		tag.setParentIdTag(request.getIdTag());
		tag.setStatus(AuthorizationStatus.Accepted);
		response.setIdTagInfo(tag);

		return response;
	}

	@Override
	public StartTransactionConfirmation handleStartTransactionRequest(UUID sessionIndex,
			StartTransactionRequest request) {

		parent.logInfo(this.log, "Handle StartRransactionRequest: " + request);

		StartTransactionConfirmation response = new StartTransactionConfirmation();
		IdTagInfo tag = new IdTagInfo();
		tag.setStatus(AuthorizationStatus.Accepted);
		tag.validate();
		response.setIdTagInfo(tag);
		return response;
	}

	@Override
	public DataTransferConfirmation handleDataTransferRequest(UUID sessionIndex, DataTransferRequest request) {

		parent.logInfo(this.log, "Handle DataTransferRequest: " + request);
		DataTransferConfirmation response = new DataTransferConfirmation();
		response.setStatus(DataTransferStatus.Accepted);

		return response;
	}

	@Override
	public HeartbeatConfirmation handleHeartbeatRequest(UUID sessionIndex, HeartbeatRequest request) {

		parent.logInfo(this.log, "Handle HeartbeatRequest: " + request);

		return new HeartbeatConfirmation();
	}

	@Override
	public MeterValuesConfirmation handleMeterValuesRequest(UUID sessionIndex, MeterValuesRequest request) {

		parent.logInfo(this.log, "Handle MeterValuesRequest: " + request);

		OcppEvcs evcs = getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
		if (evcs == null) {
			return new MeterValuesConfirmation();
		}

		MeterValue[] meterValueArr = request.getMeterValue();
		for (MeterValue meterValue : meterValueArr) {

			SampledValue[] sampledValues = meterValue.getSampledValue();
			for (SampledValue value : sampledValues) {

				// value.getLocation(); Not needed
				String phases = value.getPhase();
				String unitString = value.getUnit();
				Unit unit = Unit.valueOf(unitString.toUpperCase());
				String val = value.getValue();

				/**
				 * Value is formated in RAW data (integer/decimal) or in SignedData (binary data
				 * block, encoded as hex data)
				 */
				if (val != null) {

					ValueFormat format = value.getFormat();
					if (format.equals(ValueFormat.SignedData)) {
						val = fromHexToDezString(val);
					}

					String measurandString = value.getMeasurand();
					SampleValueMeasurand measurand = SampleValueMeasurand
							.valueOf(measurandString.replace(".", "_").toUpperCase());

					switch (measurand) {
					case CURRENT_IMPORT:
						evcs.getCurrentToEV().setNextValue(Double.valueOf(val) * 1000);
						break;
					case CURRENT_EXPORT:
						evcs.getCurrentToGrid().setNextValue(Double.valueOf(val) * 1000);
						break;
					case CURRENT_OFFERED:
						evcs.getCurrentOffered().setNextValue(Double.valueOf(val) * 1000);
						break;
					case ENERGY_ACTIVE_EXPORT_REGISTER:
						if (unit.equals(Unit.KWH)) {
							val = divideByThousand(val);
						}
						evcs.getActiveEnergyToGrid().setNextValue(val);
						break;
					case ENERGY_ACTIVE_IMPORT_REGISTER:
						if (unit.equals(Unit.KWH)) {
							val = divideByThousand(val);
						}
						evcs.getEnergySession().setNextValue(val);
						break;
					case ENERGY_REACTIVE_EXPORT_REGISTER:
						if (unit.equals(Unit.KVARH)) {
							val = divideByThousand(val);
						}
						evcs.getReactiveEnergyToGrid().setNextValue(val);
						break;
					case ENERGY_REACTIVE_IMPORT_REGISTER:
						if (unit.equals(Unit.KVARH)) {
							val = divideByThousand(val);
						}
						evcs.getReactiveEnergyToEV().setNextValue(val);
						break;
					case ENERGY_ACTIVE_IMPORT_INTERVAL:
						if (unit.equals(Unit.KWH)) {
							val = divideByThousand(val);
						}
						evcs.getActiveEnergyToEVInInterval().setNextValue(val);
						break;
					case ENERGY_ACTIVE_EXPORT_INTERVAL:
						if (unit.equals(Unit.KWH)) {
							val = divideByThousand(val);
						}
						evcs.getActiveEnergyToGridInInterval().setNextValue(val);
						break;
					case ENERGY_REACTIVE_IMPORT_INTERVAL:
						if (unit.equals(Unit.KVARH)) {
							val = divideByThousand(val);
						}
						evcs.getReactiveEnergyToEvInInterval().setNextValue(val);
						break;
					case ENERGY_REACTIVE_EXPORT_INTERVAL:
						if (unit.equals(Unit.KVARH)) {
							val = divideByThousand(val);
						}
						evcs.getReactiveEnergyToGridInInterval().setNextValue(val);
						break;
					case FREQUENCY:
						evcs.getFrequency().setNextValue(val);
						break;
					case POWER_ACTIVE_EXPORT:
						if (unit.equals(Unit.KW)) {
							val = divideByThousand(val);
						}
						evcs.getActivePowerToGrid();
						break;
					case POWER_ACTIVE_IMPORT:
						if (unit.equals(Unit.KW)) {
							val = divideByThousand(val);
						}
						evcs.getChargePower().setNextValue(val);
						break;
					case POWER_FACTOR:
						evcs.getPowerFactor().setNextValue(val);
						break;
					case POWER_OFFERED:
						if (unit.equals(Unit.KW)) {
							val = divideByThousand(val);
						}
						evcs.getPowerOffered().setNextValue(val);
						break;
					case POWER_REACTIVE_EXPORT:
						if (unit.equals(Unit.KVAR)) {
							val = divideByThousand(val);
						}
						evcs.getReactivePowerToGrid().setNextValue(val);
						break;
					case POWER_REACTIVE_IMPORT:
						if (unit.equals(Unit.KVAR)) {
							val = divideByThousand(val);
						}
						evcs.getReactivePowerToEV().setNextValue(val);
						break;
					case RPM:
						evcs.getFanSpeed().setNextValue(val);
						;
						break;
					case SOC:
						evcs.getSoC().setNextValue(val);
						break;
					case TEMPERATURE:
						evcs.getTemperature().setNextValue(val);
						break;
					case VOLTAGE:
						evcs.getVoltage().setNextValue(Math.round(Double.valueOf(val)));
						break;
					}
					parent.logInfo(this.log, measurandString + ": " + val + " " + unitString + " Phases: " + phases);
				}
			}
		}
		return new MeterValuesConfirmation();
	}

	@Override
	public StatusNotificationConfirmation handleStatusNotificationRequest(UUID sessionIndex,
			StatusNotificationRequest request) {

		parent.logInfo(this.log, "Handle StatusNotificationRequest: " + request);
		OcppEvcs evcs = getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
		if (evcs == null) {
			return new StatusNotificationConfirmation();
		}

		Status evcsStatus = null;
		ChargePointStatus ocppStatus = request.getStatus();
		switch (ocppStatus) {
		case Available:
			evcs.getChargingstationCommunicationFailed().setNextValue(false);
			break;
		case Charging:
			evcsStatus = Status.CHARGING;
			break;
		case Faulted:
			evcsStatus = Status.ERROR;
			break;
		case Finishing:
			evcsStatus = Status.CHARGING_FINISHED;
			break;
		case Preparing:
			evcsStatus = Status.READY_FOR_CHARGING;
			break;
		case Reserved:
			parent.logInfo(this.log, "Reservation currently not supported");
			break;
		case SuspendedEV:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case SuspendedEVSE:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case Unavailable:
			parent.logInfo(this.log, "Charging Station is Unavailable.");
			evcs.getChargingstationCommunicationFailed().setNextValue(true);
			evcsStatus = Status.ERROR;
			break;
		}
		if (evcsStatus != null) {
			evcs.status().setNextValue(evcsStatus);
		}
		return new StatusNotificationConfirmation();
	}

	@Override
	public StopTransactionConfirmation handleStopTransactionRequest(UUID sessionIndex, StopTransactionRequest request) {

		parent.logInfo(this.log, "Handle StopTransactionRequest: " + request);

		StopTransactionConfirmation response = new StopTransactionConfirmation();
		IdTagInfo tag = new IdTagInfo();
		tag.setParentIdTag(request.getIdTag());
		tag.setStatus(AuthorizationStatus.Accepted);
		tag.validate();
		response.setIdTagInfo(tag);
		response.validate();
		return response;
	}

	@Override
	public BootNotificationConfirmation handleBootNotificationRequest(UUID sessionIndex,
			BootNotificationRequest request) {

		parent.logInfo(this.log, "Handle BootNotificationRequest: " + request);
		List<OcppEvcs> evcss = getEvcssBySessionIndex(sessionIndex);
		for (OcppEvcs ocppEvcs : evcss) {
			ocppEvcs.getChargingstationCommunicationFailed().setNextValue(false);
			ocppEvcs.status().setNextValue(Status.NOT_READY_FOR_CHARGING);
		}

		BootNotificationConfirmation response = new BootNotificationConfirmation();
		response.setInterval(100);
		response.setStatus(RegistrationStatus.Accepted);
		response.setCurrentTime(Calendar.getInstance());

		return response;
	}

	private List<OcppEvcs> getEvcssBySessionIndex(UUID sessionIndex) {
		return this.parent.sessionMap.get(sessionIndex);
	}

	private OcppEvcs getEvcsBySessionIndexAndConnector(UUID sessionIndex, int connectorId) {

		List<OcppEvcs> evcss = this.parent.sessionMap.get(sessionIndex);
		for (OcppEvcs ocppEvcs : evcss) {
			if (ocppEvcs.getConnectorId().value().orElse(0) == connectorId) {
				return ocppEvcs;
			}
		}
		parent.logInfo(this.log,
				"No Chargingstation for session " + sessionIndex + " and connector " + connectorId + " found.");
		return null;
	}

	public String fromHexToDezString(String hex) {
		int dezValue = Integer.parseInt(hex, 16);
		return String.valueOf(dezValue);
	}

	private String divideByThousand(String val) {
		if (val.isEmpty()) {
			return val;
		}
		return String.valueOf((Double.parseDouble(val) / 1000.0));
	}
}