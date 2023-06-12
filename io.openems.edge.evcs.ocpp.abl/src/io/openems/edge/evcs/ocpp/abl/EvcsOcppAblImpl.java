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
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
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
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.Abl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EvcsOcppAblImpl extends AbstractManagedOcppEvcsComponent
		implements EvcsOcppAbl, Evcs, MeasuringEvcs, ManagedEvcs, OpenemsComponent, EventHandler {

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
	private static final HashSet<OcppInformations> MEASUREMENTS = new HashSet<>(//
			Arrays.asList(//
					OcppInformations.values()) //
	);

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private ComponentManager componentManager;

	public EvcsOcppAblImpl() {
		super(//
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				AbstractManagedOcppEvcsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MeasuringEvcs.ChannelId.values(), //
				EvcsOcppAbl.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		this._setChargingType(ChargingType.AC);
		this._setPowerPrecision(230);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
	public void handleEvent(Event event) {
		super.handleEvent(event);
	}

	@Override
	public OcppStandardRequests getStandardRequests() {
		AbstractManagedOcppEvcsComponent evcs = this;

		return new OcppStandardRequests() {

			@Override
			public Request setChargePowerLimit(int chargePower) {
				var request = new DataTransferRequest("ABL");
				var phases = evcs.getPhasesAsInt();
				var target = Math.round(chargePower / phases / 230.0);
				var maxCurrent = evcs.getMaximumHardwarePower().orElse(DEFAULT_HARDWARE_LIMIT) / phases / 230;

				target = target > maxCurrent ? maxCurrent : target;
				request.setMessageId("SetLimit");
				request.setData(
						"logicalid=" + EvcsOcppAblImpl.this.config.limitId() + ";value=" + String.valueOf(target));
				return request;
			}

			@Override
			public Request setDisplayText(String text) {
				// Feature not supported
				return null;
			}
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
		// TODO: Not tested for now
		return false;
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return false;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
