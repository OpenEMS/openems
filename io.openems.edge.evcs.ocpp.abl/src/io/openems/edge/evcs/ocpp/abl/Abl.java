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
import io.openems.edge.evcs.api.EvcsPower;
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
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class Abl extends AbstractOcppEvcsComponent
		implements Evcs, MeasuringEvcs, ManagedEvcs, OpenemsComponent, EventHandler {

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
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<>( //
			Arrays.asList( //
					OcppInformations.values()) //
	);

	private Config config;

	@Reference
	private EvcsPower evcsPower;

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
		this._setPowerPrecision(230);
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

	@Override
	public Integer getConfiguredMaximumHardwarePower() {
		// TODO: Set dynamically. Problem: No phases calculation possible.
		return (int) (this.config.maxHwCurrent() / 1000.0) * 230 * 3;
	}

	@Override
	public Integer getConfiguredMinimumHardwarePower() {
		// TODO: Set dynamically. Problem: No phases calculation possible.
		return (int) (this.config.minHwCurrent() / 1000.0) * 230 * 3;
	}

	@Override
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public OcppStandardRequests getStandardRequests() {
		AbstractOcppEvcsComponent evcs = this;

		return chargePower -> {

			var request = new DataTransferRequest("ABL");

			int phases = evcs.getPhases().orElse(3);

			var target = Math.round(chargePower / phases / 230.0) /* voltage */ ;

			var maxCurrent = evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT) / phases / 230;
			target = target > maxCurrent ? maxCurrent : target;

			request.setMessageId("SetLimit");
			request.setData("logicalid=" + Abl.this.config.limitId() + ";value=" + String.valueOf(target));
			return request;
		};
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {
		List<Request> requests = new ArrayList<>();

		var setMeterValueSampleInterval = new ChangeConfigurationRequest("MeterValueSampleInterval", "10");
		requests.add(setMeterValueSampleInterval);

		var setMeterValueSampledData = new ChangeConfigurationRequest("MeterValuesSampledData",
				"Energy.Active.Import.Register,Current.Import,Voltage,Power.Active.Import,Temperature");
		requests.add(setMeterValueSampledData);

		return requests;
	}

	@Override
	public List<Request> getRequiredRequestsDuringConnection() {
		List<Request> requests = new ArrayList<>();

		var requestMeterValues = new TriggerMessageRequest(TriggerMessageRequestType.MeterValues);
		requestMeterValues.setConnectorId(this.getConfiguredConnectorId());
		requests.add(requestMeterValues);

		var requestStatus = new TriggerMessageRequest(TriggerMessageRequestType.StatusNotification);
		requestStatus.setConnectorId(this.getConfiguredConnectorId());
		requests.add(requestStatus);

		var setMeterValueSampledData = new ChangeConfigurationRequest("MeterValuesSampledData",
				"Energy.Active.Import.Register,Current.Import,Voltage,Power.Active.Import,Temperature");
		requests.add(setMeterValueSampledData);

		return requests;
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean returnsSessionEnergy() {
		return false;
	}
}
