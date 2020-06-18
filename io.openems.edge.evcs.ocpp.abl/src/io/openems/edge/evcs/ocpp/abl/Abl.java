package io.openems.edge.evcs.ocpp.abl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.chargetime.ocpp.NotConnectedException;
import eu.chargetime.ocpp.OccurenceConstraintException;
import eu.chargetime.ocpp.UnsupportedFeatureException;
import eu.chargetime.ocpp.model.Request;
import eu.chargetime.ocpp.model.core.ChangeConfigurationRequest;
import eu.chargetime.ocpp.model.core.DataTransferRequest;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequest;
import eu.chargetime.ocpp.model.remotetrigger.TriggerMessageRequestType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.ocpp.common.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Evcs.Ocpp.Abl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class Abl extends AbstractOcppEvcsComponent
		implements Evcs, MeasuringEvcs, ManagedEvcs, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(Abl.class);

	// Default value for the hardware limit
	private static final Integer DEFAULT_HARDWARE_LIMIT = 22080;

	// Profiles that a ABL is supporting
	private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE //
	};

	/*
	 * Values that a ABL is supporting Info: It is not sure that the ABL is using
	 * all of them, but in particular it is not supporting the information of the
	 * current power.
	 */
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<OcppInformations>( //
			Arrays.asList( //
					OcppInformations.values()) //
	);

	private Config config;

	@Reference
	protected ComponentManager componentManager;

	public Abl() {
		super( //
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				AbstractOcppEvcsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), MeasuringEvcs.ChannelId.values() //
		);
	}

	@Activate
	public void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this._setChargingType(ChargingType.AC);
	}

	@Override
	public Set<OcppInformations> getSupportedMeasurements() {
		return MEASUREMENTS;
	}

	@Override
	public String getConfiguredOcppId() {
		return this.config.ocpp_id();
	}

	@Override
	public Integer getConfiguredConnectorId() {
		return this.config.connectorId();
	}

	private int dynamicMaximumHardwarePower = 0;

	@Override
	public Integer getConfiguredMaximumHardwarePower() {
		if (this.sessionId == null || this.ocppServer == null) {
			return this.config.maxHwPower();
		}
		DataTransferRequest request = new DataTransferRequest("ABL");
		request.setMessageId("GetLimit");
		request.setData(this.config.limitId());

		try {
			this.ocppServer.send(this.sessionId, request).whenComplete((confirmation, throwable) -> {

				dynamicMaximumHardwarePower = Integer.valueOf(confirmation.toString());
				this.logInfo(log, confirmation.toString());
			});
			return dynamicMaximumHardwarePower;
		} catch (OccurenceConstraintException e) {
			e.printStackTrace();
		} catch (UnsupportedFeatureException e) {
			e.printStackTrace();
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
		return this.config.maxHwPower();
	}

	@Override
	public Integer getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public OcppStandardRequests getStandardRequests() {
		AbstractOcppEvcsComponent evcs = this;

		return new OcppStandardRequests() {

			@Override
			public Request setChargePowerLimit(int chargePower) {

				DataTransferRequest request = new DataTransferRequest("ABL");

				int phases = evcs.getPhases().orElse(3);

				long target = Math.round(chargePower / phases / 230.0) /* voltage */ ;

				int maxCurrent = evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT) / phases / 230;
				target = target > maxCurrent ? maxCurrent : target;

				request.setMessageId("SetLimit");
				request.setData("logicalid=" + config.limitId() + ";value=" + String.valueOf(target));
				return request;
			}
		};
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {
		List<Request> requests = new ArrayList<Request>();

		ChangeConfigurationRequest setMeterValueSampleInterval = new ChangeConfigurationRequest(
				"MeterValueSampleInterval", "10");
		requests.add(setMeterValueSampleInterval);

		ChangeConfigurationRequest setMeterValueSampledData = new ChangeConfigurationRequest("MeterValuesSampledData",
				"Energy.Active.Import.Register,Current.Import,Voltage,Power.Active.Import,");
		requests.add(setMeterValueSampledData);

		return requests;
	}

	@Override
	public List<Request> getRequiredRequestsDuringConnection() {
		List<Request> requests = new ArrayList<Request>();

		TriggerMessageRequest requestMeterValues = new TriggerMessageRequest(TriggerMessageRequestType.MeterValues);
		requestMeterValues.setConnectorId(this.getConfiguredConnectorId());
		requests.add(requestMeterValues);

		TriggerMessageRequest requestStatus = new TriggerMessageRequest(TriggerMessageRequestType.StatusNotification);
		requestStatus.setConnectorId(this.getConfiguredConnectorId());
		requests.add(requestStatus);

		ChangeConfigurationRequest setMeterValueSampledData = new ChangeConfigurationRequest("MeterValuesSampledData",
				"Energy.Active.Import.Register,Current.Import,Voltage,Power.Active.Import,Temperature");
		requests.add(setMeterValueSampledData);

		return requests;
	}
}
