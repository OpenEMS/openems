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
import org.osgi.service.component.annotations.Modified;
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
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.MeasuringEvcs;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.ocpp.common.AbstractManagedOcppEvcsComponent;
import io.openems.edge.evcs.ocpp.common.OcppInformations;
import io.openems.edge.evcs.ocpp.common.OcppProfileType;
import io.openems.edge.evcs.ocpp.common.OcppStandardRequests;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Ocpp.IesKeywattSingle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class IesKeywattSingleCcs extends AbstractManagedOcppEvcsComponent
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

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ComponentManager componentManager;

	public IesKeywattSingleCcs() {
		super(//
				PROFILE_TYPES, //
				OpenemsComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				AbstractManagedOcppEvcsComponent.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MeasuringEvcs.ChannelId.values(), //
				SocEvcs.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) {
		this.setInitalSettings(config);
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	protected void modified(ComponentContext context, Config config) {
		this.setInitalSettings(config);
		super.modified(context, config.id(), config.alias(), config.enabled());
	}

	private void setInitalSettings(Config config) {
		this.config = config;
		this._setPowerPrecision(1);
		this._setChargingType(ChargingType.CCS);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
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
		return new OcppStandardRequests() {

			@Override
			public Request setChargePowerLimit(int chargePower) {
				// There is no best practice for DC Chargers for now. Currently, the user has to
				// set the AC-Target and not the DC

				return new ChangeConfigurationRequest("PowerLimit", String.valueOf(chargePower));
			}

			@Override
			public Request setDisplayText(String text) {
				return null;
			}
		};
	}

	@Override
	public List<Request> getRequiredRequestsAfterConnection() {

		var requests = new ArrayList<Request>();

		// TODO: Try to set lower Intervals
		var setMeterValueSampleInterval = new ChangeConfigurationRequest("MeterValueSampleInterval", "2");
		requests.add(setMeterValueSampleInterval);

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

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		// Default for now - MeterValues coming every two seconds
		return 30;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return this.config.minHwPower();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return this.config.maxHwPower();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
