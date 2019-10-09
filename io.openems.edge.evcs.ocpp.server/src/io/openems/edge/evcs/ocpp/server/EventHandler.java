package io.openems.edge.evcs.ocpp.server;

import java.util.Calendar;
import java.util.UUID;

import org.omg.CORBA.ValueMember;

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
		//tag.setParentIdTag(authorizeRequest.getIdTag());
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

		MeterValue[] meterValueArr = request.getMeterValue();
		for (MeterValue meterValue : meterValueArr) {

			SampledValue[] sampledValues = meterValue.getSampledValue();
			for (SampledValue value : sampledValues) {
				
				// Value is formated in RAW data (integer/decimal) or in SignedData (binary data block, encoded as hex data)
				ValueFormat format = value.getFormat();
				
				//value.getLocation(); Not really needed
				
				String measurand = value.getMeasurand();
				String phases = value.getPhase();
				String unit = value.getUnit();
				String val = value.getValue();
				
				System.out.println(value);
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

}