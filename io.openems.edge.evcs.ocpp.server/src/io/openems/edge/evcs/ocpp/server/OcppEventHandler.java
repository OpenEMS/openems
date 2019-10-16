package io.openems.edge.evcs.ocpp.server;

import java.util.Calendar;
import java.util.UUID;

import eu.chargetime.ocpp.feature.profile.ServerCoreEventHandler;
import eu.chargetime.ocpp.model.core.AuthorizationStatus;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.AuthorizeRequest;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.BootNotificationRequest;
import eu.chargetime.ocpp.model.core.ChargePointStatus;
import eu.chargetime.ocpp.model.core.DataTransferConfirmation;
import eu.chargetime.ocpp.model.core.DataTransferRequest;
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

	private OcppServer parent;

	public OcppEventHandler(OcppServer parent) {
		this.parent = parent;
	}

	@Override
	public AuthorizeConfirmation handleAuthorizeRequest(UUID sessionIndex, AuthorizeRequest request) {

		System.out.println("handleAuthorizeRequest: " + request);
		
		// ... handle event
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

		System.out.println("handleStartTransactionRequest: " + request);

		StartTransactionConfirmation response = new StartTransactionConfirmation();
		IdTagInfo tag = new IdTagInfo();
		tag.setStatus(AuthorizationStatus.Accepted);
		// tag.setParentIdTag(authorizeRequest.getIdTag());
		tag.validate();
		response.setIdTagInfo(tag);
		/*
		 * 
		 * // ... handle event
		 * System.out.println("ID Tag Authorisation: "+this.authorizeRequest.getIdTag())
		 * ;
		 * 
		 * IdTagInfo tag = new IdTagInfo(); tag.setStatus(AuthorizationStatus.Accepted);
		 * tag.setParentIdTag("abc"); tag.validate(); response.setIdTagInfo(tag);
		 * response.setTransactionId(11);
		 * 
		 * System.out.println(response.getIdTagInfo());
		 * System.out.println(response.getTransactionId());
		 * System.out.println(response);
		 * 
		 * return response;
		 */
		return response;
	}

	@Override
	public DataTransferConfirmation handleDataTransferRequest(UUID sessionIndex, DataTransferRequest request) {

		System.out.println("handleDataTransferRequest: " + request);
		// ... handle event

		return null; // returning null means unsupported feature
	}

	@Override
	public HeartbeatConfirmation handleHeartbeatRequest(UUID sessionIndex, HeartbeatRequest request) {

		System.out.println("handleHeartbeatRequest: " + request);
		// ... handle event

		return new HeartbeatConfirmation();
	}

	@Override
	public MeterValuesConfirmation handleMeterValuesRequest(UUID sessionIndex, MeterValuesRequest request) {

		System.out.println("handleMeterValuesRequest: " + request);

		OcppEvcs evcs = getEvcsBySessionIndex(sessionIndex);
		if (evcs == null) {
			return new MeterValuesConfirmation();
		}

		MeterValue[] meterValueArr = request.getMeterValue();
		for (MeterValue meterValue : meterValueArr) {

			SampledValue[] sampledValues = meterValue.getSampledValue();
			for (SampledValue value : sampledValues) {

				// value.getLocation(); Not really needed

				String phases = value.getPhase();
				System.out.println("Phases: " + phases);

				String unitString = value.getUnit();
				Unit unit = Unit.valueOf(unitString.toUpperCase());
				System.out.println();

				String val = value.getValue();

				if (val != null) {
					// Value is formated in RAW data (integer/decimal) or in SignedData (binary data
					// block, encoded as hex data)
					ValueFormat format = value.getFormat();
					if (format.equals(ValueFormat.SignedData)) {
						System.out.println("Signed data: " + val);
						val = fromHexToString(val);
						System.out.println("Raw data: " + val);
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
						System.out.println("Temp:" + val);
						// evcs.getTemperature().setNextValue(val);
						break;
					case VOLTAGE:
						evcs.getVoltage().setNextValue(Math.round(Double.valueOf(val)));
						break;
					}

					System.out.println(val);
					System.out.println(unitString);
				}
			}
		}
		return new MeterValuesConfirmation();
	}

	@Override
	public StatusNotificationConfirmation handleStatusNotificationRequest(UUID sessionIndex,
			StatusNotificationRequest request) {

		System.out.println("handleStatusNotificationRequest: " + request);
		OcppEvcs evcs = getEvcsBySessionIndex(sessionIndex);
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
			System.out.println("Reservation currently not supported");
			break;
		case SuspendedEV:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case SuspendedEVSE:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case Unavailable:
			System.out.println("Charging Station is Unavailable.");
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

		System.out.println("handleStopTransactionRequest: " + request);
		// ... handle event

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

		System.out.println("handleBootNotificationRequest: " + request);

		// ... handle event
		BootNotificationConfirmation response = new BootNotificationConfirmation();
		response.setInterval(100);
		response.setStatus(RegistrationStatus.Accepted);
		response.setCurrentTime(Calendar.getInstance());

		return response;
	}

	private OcppEvcs getEvcsBySessionIndex(UUID sessionIndex) {
		OcppEvcs evcs = this.parent.sessionMap.get(sessionIndex);
		if (evcs == null) {
			System.out.println("No Chargingstation for session " + sessionIndex + " found.");
			return null;
		}
		return evcs;
	}

	public String fromHexToString(String hex) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			str.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
		}
		return str.toString();
	}

	private String divideByThousand(String val) {
		if (val.isEmpty()) {
			return val;
		}
		return String.valueOf((Double.parseDouble(val) / 1000.0));
	}

}