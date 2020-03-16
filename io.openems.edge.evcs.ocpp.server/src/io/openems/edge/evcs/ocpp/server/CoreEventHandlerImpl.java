package io.openems.edge.evcs.ocpp.server;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.EvictingQueue;

import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.ocpp.core.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.core.ChargingProperty;
import io.openems.edge.evcs.ocpp.core.OcppInformations;
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

public class CoreEventHandlerImpl implements ServerCoreEventHandler {

	private final Logger log = LoggerFactory.getLogger(CoreEventHandlerImpl.class);

	private OcppServerImpl server;

	public CoreEventHandlerImpl(OcppServerImpl parent) {
		this.server = parent;
	}

	@Override
	public AuthorizeConfirmation handleAuthorizeRequest(UUID sessionIndex, AuthorizeRequest request) {

		server.logInfoInDebug(this.log, "Handle AuthorizeRequest: " + request);

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

		server.logInfoInDebug(this.log, "Handle StartTransactionRequest: " + request);

		StartTransactionConfirmation response = new StartTransactionConfirmation();
		IdTagInfo tag = new IdTagInfo();
		tag.setStatus(AuthorizationStatus.Accepted);
		tag.validate();
		response.setIdTagInfo(tag);
		return response;
	}

	@Override
	public DataTransferConfirmation handleDataTransferRequest(UUID sessionIndex, DataTransferRequest request) {

		server.logInfoInDebug(this.log, "Handle DataTransferRequest: " + request);
		DataTransferConfirmation response = new DataTransferConfirmation();
		response.setStatus(DataTransferStatus.Accepted);

		return response;
	}

	@Override
	public HeartbeatConfirmation handleHeartbeatRequest(UUID sessionIndex, HeartbeatRequest request) {

		server.logInfoInDebug(this.log, "Handle HeartbeatRequest: " + request);

		return new HeartbeatConfirmation();
	}

	@Override
	public MeterValuesConfirmation handleMeterValuesRequest(UUID sessionIndex, MeterValuesRequest request) {

		server.logInfoInDebug(this.log, "Handle MeterValuesRequest: " + request);

		AbstractOcppEvcsComponent evcs = getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
		if (evcs == null) {
			return new MeterValuesConfirmation();
		}

		evcs.status().setNextValue(Status.CHARGING);

		/*
		 * Set the channels depending on the meter values
		 */
		MeterValue[] meterValueArr = request.getMeterValue();
		for (MeterValue meterValue : meterValueArr) {

			SampledValue[] sampledValues = meterValue.getSampledValue();
			for (SampledValue value : sampledValues) {

				// value.getLocation(); Not needed
				String phases = value.getPhase();
				String unitString = value.getUnit();
				Unit unit = Unit.valueOf(unitString.toUpperCase());
				String val = value.getValue();

				if (val != null) {

					/*
					 * Value is formated in RAW data (integer/decimal) or in SignedData (binary data
					 * block, encoded as hex data)
					 */
					ValueFormat format = value.getFormat();
					if (format.equals(ValueFormat.SignedData)) {
						val = fromHexToDezString(val);
					}

					String measurandString = value.getMeasurand();

					OcppInformations measurand = OcppInformations
							.valueOf("CORE_METER_VALUES_" + measurandString.replace(".", "_").toUpperCase());

					if (evcs.getSupportedMeasurements().contains(measurand)) {

						Object correctValue = val;
						switch (measurand) {
						case CORE_METER_VALUES_CURRENT_EXPORT:
						case CORE_METER_VALUES_CURRENT_IMPORT:
						case CORE_METER_VALUES_CURRENT_OFFERED:
							correctValue = Double.valueOf(val) * 1000;
							break;

						case CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_INTERVAL:
						case CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_INTERVAL:
							if (unit.equals(Unit.KWH)) {
								correctValue = divideByThousand(val);
							}
							break;

						case CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_INTERVAL:
						case CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_INTERVAL:
							if (unit.equals(Unit.KVARH)) {
								correctValue = divideByThousand(val);
							}
							break;

						case CORE_METER_VALUES_POWER_ACTIVE_EXPORT:
						case CORE_METER_VALUES_POWER_ACTIVE_IMPORT:
						case CORE_METER_VALUES_POWER_OFFERED:
							if (unit.equals(Unit.KW)) {
								correctValue = divideByThousand(val);
							}
							break;

						case CORE_METER_VALUES_POWER_REACTIVE_EXPORT:
						case CORE_METER_VALUES_POWER_REACTIVE_IMPORT:
							if (unit.equals(Unit.KVAR)) {
								correctValue = divideByThousand(val);
							}
							break;

						case CORE_METER_VALUES_VOLTAGE:
							correctValue = (int) Math.round(Double.valueOf(val));
							break;

						case CORE_METER_VALUES_RPM:
						case CORE_METER_VALUES_SOC:
						case CORE_METER_VALUES_TEMPERATURE:
						case CORE_METER_VALUES_POWER_FACTOR:
						case CORE_METER_VALUES_FREQUENCY:
							break;
						}

						evcs.channel(measurand.getChannelId()).setNextValue(correctValue);
						server.logInfoInDebug(this.log,
								measurandString + ": " + val + " " + unitString + " Phases: " + phases);
					}

					if (measurand.equals(OcppInformations.CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER)) {
						// TODO: Set the evcs.energySession if we know when the charging started and
						// ended.

						if (!evcs.getSupportedMeasurements()
								.contains(OcppInformations.CORE_METER_VALUES_POWER_ACTIVE_IMPORT)) {

							EvictingQueue<ChargingProperty> meterValueQueue = evcs.getMeterValueQueue();
							ChargingProperty meterValueOld = meterValueQueue.peek();

							int power = 0;
							Long currentEnergy = Long.parseLong(val);
							
							if (meterValueOld != null) {
								power = this.calculateChargePower(meterValueOld, currentEnergy, meterValue.getTimestamp());
								evcs.getChargePower().setNextValue(power);
							}
							meterValueQueue.add(new ChargingProperty(power, currentEnergy, meterValue.getTimestamp()));
						}
					}
				}
			}
		}
		return new MeterValuesConfirmation();
	}

	/**
	 * Calculates the power depending on the last and current measured Energy.
	 * 
	 * @param meterValueOld    Last measured meter values
	 * @param currentEnergy    Current measured Energy
	 * @param currentTimestamp Time when the current Energy was measured
	 * @return current power
	 */
	private int calculateChargePower(ChargingProperty lastMeterValue, long currentEnergy, Calendar currentTimestamp) {

		long diffseconds = Math
				.round((currentTimestamp.getTimeInMillis() - lastMeterValue.getTimestamp().getTimeInMillis()) / 1000.0);

		long lastEnergy = lastMeterValue.getTotalMeterEnergy();

		int power = (int) (Math.round((currentEnergy - lastEnergy) / (diffseconds / 3600.0)));

		this.server.logInfoInDebug(log,
				"Last: " + lastEnergy + "Wh, Current: " + currentEnergy + "Wh. Calculated Power: " + power);

		return power;
	}

	@Override
	public StatusNotificationConfirmation handleStatusNotificationRequest(UUID sessionIndex,
			StatusNotificationRequest request) {

		server.logInfoInDebug(this.log, "Handle StatusNotificationRequest: " + request);
		MeasuringEvcs evcs = getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
		if (evcs == null) {
			return new StatusNotificationConfirmation();
		}

		/*
		 * Set the EVCS status based on the status from the StatusNotificationRequest
		 */
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
			server.logInfoInDebug(this.log, "Reservation currently not supported");
			break;
		case SuspendedEV:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case SuspendedEVSE:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case Unavailable:
			server.logInfoInDebug(this.log, "Charging Station is Unavailable.");
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

		server.logInfoInDebug(this.log, "Handle StopTransactionRequest: " + request);

		IdTagInfo tag = new IdTagInfo();
		tag.setParentIdTag(request.getIdTag());
		tag.setStatus(AuthorizationStatus.Accepted);
		tag.validate();

		StopTransactionConfirmation response = new StopTransactionConfirmation();
		response.setIdTagInfo(tag);
		response.validate();
		return response;
	}

	@Override
	public BootNotificationConfirmation handleBootNotificationRequest(UUID sessionIndex,
			BootNotificationRequest request) {

		server.logInfoInDebug(this.log, "Handle BootNotificationRequest: " + request);
		List<AbstractOcppEvcsComponent> evcss = getEvcssBySessionIndex(sessionIndex);
		for (AbstractOcppEvcsComponent ocppEvcs : evcss) {
			ocppEvcs.getChargingstationCommunicationFailed().setNextValue(false);
			Status state = ocppEvcs.status().value().asEnum();
			if (state == null || state.equals(Status.UNDEFINED) || state.equals(Status.CHARGING_FINISHED)) {
				ocppEvcs.status().setNextValue(Status.NOT_READY_FOR_CHARGING);
			}
		}

		BootNotificationConfirmation response = new BootNotificationConfirmation();
		response.setInterval(100);
		response.setStatus(RegistrationStatus.Accepted);
		response.setCurrentTime(Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin")));
		server.logInfoInDebug(this.log, "Send BootNotificationConfirmation: " + response.toString());

		return response;
	}

	/**
	 * Get the EVCSs that are in this session.
	 * <p>
	 * One charging station has one session but can have more connectors. Every
	 * connector is one EVCS in our System because each can be managed and monitored
	 * by itself.
	 * 
	 * @param sessionIndex given session
	 * @return List of AbstractOcppEvcsComponent
	 */
	private List<AbstractOcppEvcsComponent> getEvcssBySessionIndex(UUID sessionIndex) {
		int index = this.server.getActiveSessions().indexOf(new EvcsSession(sessionIndex));
		return this.server.getActiveSessions().get(index).getOcppEvcss();
	}

	/**
	 * Get the EVCS that are in this session and with the given connector id.
	 * <p>
	 * One charging station has one session but can have more connectors. Every
	 * connector is one EVCS in our System because each can be managed and monitored
	 * by itself.
	 * 
	 * @param sessionIndex given session
	 * @param connectorId  given connector id
	 * @return EVCS Component with the given session and connectorId.
	 */
	private AbstractOcppEvcsComponent getEvcsBySessionIndexAndConnector(UUID sessionIndex, int connectorId) {
		List<AbstractOcppEvcsComponent> evcss = getEvcssBySessionIndex(sessionIndex);
		for (AbstractOcppEvcsComponent ocppEvcs : evcss) {
			if (ocppEvcs.getConnectorId().value().orElse(0) == connectorId) {
				return ocppEvcs;
			}
		}
		server.logInfoInDebug(this.log,
				"No Chargingstation for session " + sessionIndex + " and connector " + connectorId + " found.");
		return null;
	}

	/**
	 * Return the decimal value of the given Hexadecimal value.
	 * 
	 * @param hex given value in hex
	 * @return Decimal value as String
	 */
	public String fromHexToDezString(String hex) {
		int dezValue = Integer.parseInt(hex, 16);
		return String.valueOf(dezValue);
	}

	/**
	 * Divide the given String value by thousand.
	 * 
	 * @param val value
	 * @return Value / 1000 as String
	 */
	private String divideByThousand(String val) {
		if (val.isEmpty()) {
			return val;
		}
		return String.valueOf((Double.parseDouble(val) / 1000.0));
	}
}
