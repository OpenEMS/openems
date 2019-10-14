package io.openems.edge.evcs.ocpp.server;

import java.util.Calendar;
import java.util.UUID;

import eu.chargetime.ocpp.feature.profile.ServerCoreEventHandler;
import eu.chargetime.ocpp.model.core.AuthorizationStatus;
import eu.chargetime.ocpp.model.core.AuthorizeConfirmation;
import eu.chargetime.ocpp.model.core.AuthorizeRequest;
import eu.chargetime.ocpp.model.core.BootNotificationConfirmation;
import eu.chargetime.ocpp.model.core.BootNotificationRequest;
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

public class EventHandler implements ServerCoreEventHandler {

	private AuthorizeRequest authorizeRequest;

	private OcppServer parent;

	public EventHandler(OcppServer parent) {
		this.parent = parent;
	}

	@Override
	public AuthorizeConfirmation handleAuthorizeRequest(UUID sessionIndex, AuthorizeRequest request) {

		System.out.println("handleAuthorizeRequest: " + request);
		authorizeRequest = request;

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

		OcppEvcs evcs = this.parent.sessionMap.get(sessionIndex);
		if (evcs == null) {
			System.out.println("No Chargingstation for session " + sessionIndex + " found.");
			return new MeterValuesConfirmation();
		}

		MeterValue[] meterValueArr = request.getMeterValue();
		for (MeterValue meterValue : meterValueArr) {

			SampledValue[] sampledValues = meterValue.getSampledValue();
			for (SampledValue value : sampledValues) {

				// value.getLocation(); Not really needed

				String phases = value.getPhase();

				String unitString = value.getUnit();
				Unit unit = Unit.valueOf(unitString.toUpperCase());

				String val = value.getValue();

				if (val != null) {
					// Value is formated in RAW data (integer/decimal) or in SignedData (binary data block, encoded as hex data)
					ValueFormat format = value.getFormat();
					if (format.equals(ValueFormat.SignedData)) {
						val = fromHexToString(val);
					}

					String measurandString = value.getMeasurand();
					SampleValueMeasurand measurand = SampleValueMeasurand
							.valueOf(measurandString.replace(".", ":").toUpperCase());

					switch (measurand) {
					case CURRENT_IMPORT:
						break;
					case CURRENT_EXPORT:
						
						break;
					case CURRENT_OFFERED:
						break;
					case ENERGY_ACTIVE_EXPORT_INTERVAL:
						break;
					case ENERGY_ACTIVE_EXPORT_REGISTER:
						break;
					case ENERGY_ACTIVE_IMPORT_INTERVAL:
						break;
					case ENERGY_ACTIVE_IMPORT_REGISTER:
						break;
					case ENERGY_REACTIVE_EXPORT_INTERVAL:
						break;
					case ENERGY_REACTIVE_EXPORT_REGISTER:
						break;
					case ENERGY_REACTIVE_IMPORT_INTERVAL:
						break;
					case ENERGY_REACTIVE_IMPORT_REGISTER:
						break;
					case FREQUENCY:
						break;
					case POWER_ACTIVE_EXPORT:
						break;
					case POWER_ACTIVE_IMPORT:
						if (unit.equals(Unit.KW)) {
							val = divideByThousand(val);
						}
						evcs.getChargePower().setNextValue(val); 
						break;
					case POWER_FACTOR:
						break;
					case POWER_OFFERED:
						break;
					case POWER_REACTIVE_EXPORT:
						break;
					case POWER_REACTIVE_IMPORT:
						break;
					case RPM:
						break;
					case SOC:
						break;
					case TEMPERATURE:
						break;
					case VOLTAGE:
						break;
					}

					System.out.println(value);
				}
			}
		}

		// ... handle event

		MeterValuesConfirmation response = new MeterValuesConfirmation();

		return response;
	}

	@Override
	public StatusNotificationConfirmation handleStatusNotificationRequest(UUID sessionIndex,
			StatusNotificationRequest request) {

		System.out.println("handleStatusNotificationRequest: " + request);
		// ... handle event

		StatusNotificationConfirmation response = new StatusNotificationConfirmation();
		response.validate();

		return response; // returning null means unsupported feature
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

	public String fromHexToString(String hex) {
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < hex.length(); i += 2) {
			str.append((char) Integer.parseInt(hex.substring(i, i + 2), 16));
		}
		return str.toString();
	}


	private String divideByThousand(String val) {
		if(val.isEmpty()) {
			return val;
		}
		return String.valueOf((Double.parseDouble(val)/1000.0));
	}
	
}