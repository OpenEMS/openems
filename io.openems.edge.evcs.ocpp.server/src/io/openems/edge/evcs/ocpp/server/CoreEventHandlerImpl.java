package io.openems.edge.evcs.ocpp.server;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.ocpp.common.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.ChargingProperty;
import io.openems.edge.evcs.ocpp.common.OcppInformations;

public class CoreEventHandlerImpl implements ServerCoreEventHandler {

	private final Logger log = LoggerFactory.getLogger(CoreEventHandlerImpl.class);

	private final OcppServerImpl parent;

	public CoreEventHandlerImpl(OcppServerImpl parent) {
		this.parent = parent;
	}

	@Override
	public BootNotificationConfirmation handleBootNotificationRequest(UUID sessionIndex,
			BootNotificationRequest request) {

		this.logDebug("Handle BootNotificationRequest: " + request);

		BootNotificationConfirmation response = new BootNotificationConfirmation(ZonedDateTime.now(), 100,
				RegistrationStatus.Accepted);
		this.logDebug("Send BootNotificationConfirmation: " + response.toString());

		return response;
	}

	@Override
	public AuthorizeConfirmation handleAuthorizeRequest(UUID sessionIndex, AuthorizeRequest request) {

		this.logDebug("Handle AuthorizeRequest: " + request);

		IdTagInfo tag = new IdTagInfo(AuthorizationStatus.Accepted);
		tag.setParentIdTag(request.getIdTag());
		AuthorizeConfirmation response = new AuthorizeConfirmation(tag);
		return response;
	}

	@Override
	public DataTransferConfirmation handleDataTransferRequest(UUID sessionIndex, DataTransferRequest request) {

		this.logDebug("Handle DataTransferRequest: " + request);
		DataTransferConfirmation response = new DataTransferConfirmation(DataTransferStatus.Accepted);

		return response;
	}

	@Override
	public HeartbeatConfirmation handleHeartbeatRequest(UUID sessionIndex, HeartbeatRequest request) {

		this.logDebug("Handle HeartbeatRequest: " + request);
		return new HeartbeatConfirmation(ZonedDateTime.now());
	}

	@Override
	public MeterValuesConfirmation handleMeterValuesRequest(UUID sessionIndex, MeterValuesRequest request) {
		this.logDebug("Handle MeterValuesRequest: " + request);

		AbstractOcppEvcsComponent evcs = this.getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
		if (evcs == null) {
			return new MeterValuesConfirmation();
		}

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
				String measurandString = value.getMeasurand();

				if (val != null) {

					/*
					 * Value is formated in RAW data (integer/decimal) or in SignedData (binary data
					 * block, encoded as hex data)
					 */
					ValueFormat format = value.getFormat();
					if (format.equals(ValueFormat.SignedData)) {
						val = this.fromHexToDezString(val);
					}

					OcppInformations measurand = OcppInformations
							.valueOf("CORE_METER_VALUES_" + measurandString.replace(".", "_").toUpperCase());

					this.logDebug(measurandString + ": " + val + " " + unitString + " Phases: " + phases);

					if (evcs.getSupportedMeasurements().contains(measurand)) {

						Object correctValue = val;
						switch (measurand) {
						case CORE_METER_VALUES_CURRENT_EXPORT:
						case CORE_METER_VALUES_CURRENT_IMPORT:
						case CORE_METER_VALUES_CURRENT_OFFERED:
							correctValue = (int) Math.round(Double.valueOf(val) * 1000);
							break;

						case CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_INTERVAL:
						case CORE_METER_VALUES_ENERGY_ACTIVE_EXPORT_INTERVAL:
							if (unit.equals(Unit.KWH)) {
								val = this.multipliedByThousand(val);
							}
							correctValue = Double.valueOf(val);
							break;

						case CORE_METER_VALUES_ENERGY_ACTIVE_IMPORT_REGISTER:
							if (unit.equals(Unit.KWH)) {
								val = this.multipliedByThousand(val);
							}
							correctValue = Double.valueOf(val);

							if (!evcs.getSupportedMeasurements()
									.contains(OcppInformations.CORE_METER_VALUES_POWER_ACTIVE_IMPORT)) {
								this.setPowerDependingOnEnergy(evcs, (Double) correctValue, meterValue.getTimestamp());
								// TODO: Currently not working with session energy values
							}

							long energy = (long) Math.round((Double) correctValue);
							if (!evcs.getSessionStart().isChargeSessionStampPresent()) {
								break;
							}

							int sessionEnergy = 0;
							long totalEnergy = 0;

							/*
							 * Calculating the energy in this session and in total for the given energy
							 * value.
							 */
							if (evcs.returnsSessionEnergy()) {
								sessionEnergy = (int) energy;
								totalEnergy = evcs.getSessionStart().getEnergy() + energy;
							} else {
								sessionEnergy = (int) (energy - evcs.getSessionStart().getEnergy());
								totalEnergy = energy;
							}
							evcs._setEnergySession(sessionEnergy);
							evcs._setActiveConsumptionEnergy(totalEnergy);
							break;

						case CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_REGISTER:
						case CORE_METER_VALUES_ENERGY_REACTIVE_EXPORT_INTERVAL:
						case CORE_METER_VALUES_ENERGY_REACTIVE_IMPORT_INTERVAL:
							if (unit.equals(Unit.KVARH)) {
								val = this.multipliedByThousand(val);
							}
							correctValue = Double.valueOf(val);
							break;

						case CORE_METER_VALUES_POWER_ACTIVE_EXPORT:
						case CORE_METER_VALUES_POWER_ACTIVE_IMPORT:
						case CORE_METER_VALUES_POWER_OFFERED:
							if (unit.equals(Unit.KW)) {
								val = this.multipliedByThousand(val);
							}
							correctValue = (int) Math.round(Double.valueOf(val));

							/*
							 * Sets the start and end session stamp depending on the the current power.
							 */
							Instant now = Instant.now(this.parent.componentManager.getClock());

							if ((int) correctValue > 0) {
								evcs._setStatus(Status.CHARGING);
							}
							
							// Has to provide a not null energy value
							Optional<Long> currEnergy = evcs.getActiveConsumptionEnergy().asOptional();
							if (currEnergy.isPresent()) {
								if ((int) correctValue > 0) {
									evcs.getSessionStart().setChargeSessionStampIfNotPresent(now, currEnergy.get());
									evcs.getSessionEnd().resetChargeSessionStampIfPresent();
								} else {
									evcs.getSessionStart().resetChargeSessionStampIfPresent();
									evcs.getSessionEnd().setChargeSessionStampIfNotPresent(now, currEnergy.get());
								}
							}
							break;

						case CORE_METER_VALUES_POWER_REACTIVE_EXPORT:
						case CORE_METER_VALUES_POWER_REACTIVE_IMPORT:
							if (unit.equals(Unit.KVAR)) {
								val = this.multipliedByThousand(val);
							}
							correctValue = (int) Math.round(Double.valueOf(val));
							break;

						case CORE_METER_VALUES_VOLTAGE:
						case CORE_METER_VALUES_SOC:
							correctValue = (int) Math.round(Double.valueOf(val));
							break;

						case CORE_METER_VALUES_RPM:
						case CORE_METER_VALUES_TEMPERATURE:
						case CORE_METER_VALUES_POWER_FACTOR:
						case CORE_METER_VALUES_FREQUENCY:
							break;
						}
						evcs.channel(measurand.getChannelId()).setNextValue(correctValue);
					}
				}
			}
		}
		return new MeterValuesConfirmation();
	}

	@Override
	public StatusNotificationConfirmation handleStatusNotificationRequest(UUID sessionIndex,
			StatusNotificationRequest request) {

		this.logDebug("Handle StatusNotificationRequest: " + request);
		AbstractOcppEvcsComponent evcs = this.getEvcsBySessionIndexAndConnector(sessionIndex, request.getConnectorId());
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
			evcs._setChargingstationCommunicationFailed(false);
			evcsStatus = Status.NOT_READY_FOR_CHARGING;
			break;
		case Charging:
			evcsStatus = Status.CHARGING;

			// Reset the end charge session stamp
			evcs.getSessionEnd().resetChargeSessionStampIfPresent();

			// Set the start charge session stamp
			evcs.getSessionStart().setChargeSessionStampIfNotPresent(
					Instant.now(this.parent.componentManager.getClock()), evcs.getActiveConsumptionEnergy().orElse(0L));
			break;
		case Faulted:
			evcsStatus = Status.ERROR;
			break;
		case Finishing:
			evcsStatus = Status.CHARGING_FINISHED;

			// Reset the start charge session stamp
			evcs.getSessionStart().resetChargeSessionStampIfPresent();

			evcs.getSessionEnd().setChargeSessionStampIfNotPresent(Instant.now(this.parent.componentManager.getClock()),
					evcs.getActiveConsumptionEnergy().orElse(0L));
			break;
		case Preparing:
			evcsStatus = Status.READY_FOR_CHARGING;
			break;
		case Reserved:
			this.logDebug("Reservation currently not supported");
			break;
		case SuspendedEV:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case SuspendedEVSE:
			evcsStatus = Status.CHARGING_REJECTED;
			break;
		case Unavailable:
			this.logDebug("Charging Station is Unavailable.");
			evcs._setChargingstationCommunicationFailed(true);
			evcsStatus = Status.ERROR;
			break;
		}

		if (ocppStatus != ChargePointStatus.Unavailable) {
			evcs._setChargingstationCommunicationFailed(false);
		}

		if (evcsStatus != null) {
			evcs._setStatus(evcsStatus);
		}
		return new StatusNotificationConfirmation();
	}

	@Override
	public StartTransactionConfirmation handleStartTransactionRequest(UUID sessionIndex,
			StartTransactionRequest request) {

		this.logDebug("Handle StartTransactionRequest: " + request);

		IdTagInfo idTagInfo = new IdTagInfo(AuthorizationStatus.Accepted);
		idTagInfo.setParentIdTag(request.getIdTag());

		StartTransactionConfirmation response = new StartTransactionConfirmation(idTagInfo, 1);
		return response;
	}

	@Override
	public StopTransactionConfirmation handleStopTransactionRequest(UUID sessionIndex, StopTransactionRequest request) {

		this.logDebug("Handle StopTransactionRequest: " + request);

		IdTagInfo tag = new IdTagInfo(AuthorizationStatus.Accepted);
		tag.setParentIdTag(request.getIdTag());
		tag.validate();

		StopTransactionConfirmation response = new StopTransactionConfirmation();
		response.setIdTagInfo(tag);
		response.validate();
		return response;
	}

	/**
	 * Get the EVCSs that are in this session.
	 * 
	 * <p>
	 * One charging station has one session but can have more connectors. Every
	 * connector is one EVCS in our System because each can be managed and monitored
	 * by itself.
	 * 
	 * @param sessionIndex given session
	 * @return List of AbstractOcppEvcsComponent
	 */
	private List<AbstractOcppEvcsComponent> getEvcssBySessionIndex(UUID sessionIndex) {
		List<AbstractOcppEvcsComponent> evcss = this.parent.activeEvcsSessions.getOrDefault(sessionIndex,
				new ArrayList<AbstractOcppEvcsComponent>());
		return evcss;
	}

	/**
	 * Get the EVCS that are in this session and with the given connector id.
	 * 
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
		List<AbstractOcppEvcsComponent> evcss = this.getEvcssBySessionIndex(sessionIndex);
		if (evcss != null) {
			for (AbstractOcppEvcsComponent ocppEvcs : evcss) {
				if (ocppEvcs.getConfiguredConnectorId().equals(connectorId)) {
					return ocppEvcs;
				}
			}
		}
		this.logDebug("No Chargingstation for session " + sessionIndex + " and connector " + connectorId + " found.");
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
	private String multipliedByThousand(String val) {
		if (val.isEmpty()) {
			return val;
		}
		return String.valueOf((Double.parseDouble(val) * 1000.0));
	}

	/**
	 * Sets the calculated power to the given EVCS.
	 * 
	 * @param evcs          Corresponding EVCS component.
	 * @param currentEnergy Current measured Energy.
	 * @param timestamp     Time when the current Energy was measured.
	 */
	private void setPowerDependingOnEnergy(AbstractOcppEvcsComponent evcs, Double currentEnergy,
			ZonedDateTime timestamp) {

		ChargingProperty lastChargingProperty = evcs.getLastChargingProperty();
		int power = 0;
		if (lastChargingProperty != null) {

			power = this.calculateChargePower(lastChargingProperty, currentEnergy, timestamp);
			evcs._setChargePower(power);
		}
		evcs.setLastChargingProperty(new ChargingProperty(power, currentEnergy, timestamp));
	}

	/**
	 * Calculates the power depending on the last and current measured Energy.
	 * 
	 * @param meterValueOld Last measured meter values.
	 * @param currentEnergy Current measured Energy.
	 * @param timestamp     Time when the current Energy was measured.
	 * @return current power
	 */
	private int calculateChargePower(ChargingProperty lastMeterValue, double currentEnergy, ZonedDateTime timestamp) {

		double diffseconds = Duration.between(timestamp, lastMeterValue.getTimestamp()).getSeconds();

		double lastEnergy = lastMeterValue.getTotalMeterEnergy();

		int power = (int) (Math.round((currentEnergy - lastEnergy) / (diffseconds / 3600.0)));

		this.logDebug("Last: " + String.valueOf(lastEnergy) + "Wh, Current: " + String.valueOf(currentEnergy)
				+ "Wh. Calculated Power: " + power + "; Sekunden differenz: " + diffseconds);

		return power;
	}

	private void logDebug(String message) {
		this.parent.logDebug(this.log, message);
	}
}
