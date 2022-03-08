package io.openems.edge.evcs.ocpp.ies.keywatt.singleccs;

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
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.ocpp.common.AbstractOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.IesKeywattSingle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		})
public class IesKeywattSingleCcs extends AbstractOcppEvcsComponent
		implements Evcs, ManagedEvcs, MeasuringEvcs, OpenemsComponent, EventHandler, SocEvcs {

	// Profiles that a Ies KeyWatt is supporting
	private static final OcppProfileType[] PROFILE_TYPES = { //
			OcppProfileType.CORE, //
			OcppProfileType.SMART_CHARGING //
	};

	// Values that a Ies KeyWatt is supporting
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<>(
			Arrays.asList(OcppInformations.values()));

	private Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ComponentManager componentManager;

	public IesKeywattSingleCcs() {
		super(//
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				AbstractOcppEvcsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MeasuringEvcs.ChannelId.values(), //
				SocEvcs.ChannelId.values() //
		);
	}

	@Activate
	public void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this._setChargingType(ChargingType.CCS);
		this._setPowerPrecision(1);
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
		return chargePower -> new ChangeConfigurationRequest("PowerLimit", String.valueOf(chargePower));
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {

		var requests = new ArrayList<Request>();

		var setMeterValueSampleInterval = new ChangeConfigurationRequest("MeterValueSampleInterval", "10");
		requests.add(setMeterValueSampleInterval);

		var requestStatus = new TriggerMessageRequest(TriggerMessageRequestType.StatusNotification);
		requests.add(requestStatus);

		return requests;
	}

	@Override
	public List<Request> getRequiredRequestsDuringConnection() {
		return new ArrayList<>();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public boolean returnsSessionEnergy() {
		return true;
	}
}
