package io.openems.edge.evcs.goe.http;

import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_CONNECT_TIMEOUT;
import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_READ_TIMEOUT;
import static io.openems.edge.bridge.http.api.HttpMethod.GET;
import static io.openems.edge.evcs.api.Evcs.calculatePhasesFromActivePowerAndPhaseCurrents;
import static io.openems.edge.evcs.api.Evcs.calculateUsedPhasesFromCurrent;
import static io.openems.edge.evcs.api.PhaseRotation.setPhaseRotatedCurrentChannels;
import static io.openems.edge.evcs.api.PhaseRotation.setPhaseRotatedVoltageChannels;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static java.util.Collections.emptyMap;

import com.google.gson.JsonObject;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.CalculateEnergySession;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.PhaseRotation;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.goe.api.EvcsGoe;
import io.openems.edge.evcs.goe.api.StatusConverter;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
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

import java.net.UnknownHostException;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Goe.Http", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EvcsGoeHttpImpl extends AbstractOpenemsComponent
		implements EvcsGoeHttp, EvcsGoe, Evcs, ElectricityMeter, EventHandler, TimedataProvider, OpenemsComponent {

	// The filtering for the Status Request changed in Version 51.4, so older
	// versions need a different request
	public static final double LEGACY_FIRMWARE = 51.3;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final CalculateEnergyFromPower calculateEnergy = new CalculateEnergyFromPower(this,
			ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergySession calculateEnergySession = new CalculateEnergySession(this);

	protected Config config;

	private final StatusConverter statusConverter = new StatusConverter(this);

	private GoeApiV2 goeApiV2 = null;
	private BridgeHttp httpBridge;

	public EvcsGoeHttpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				EvcsGoe.ChannelId.values(), //
				EvcsGoeHttp.ChannelId.values() //
		);
		calculateAverageVoltageFromPhases(this);
		calculateSumCurrentFromPhases(this);
		calculateUsedPhasesFromCurrent(this);
		calculatePhasesFromActivePowerAndPhaseCurrents(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.installStateListener();
		this.httpBridge = this.httpBridgeFactory.get();
		this.goeApiV2 = new GoeApiV2(this);
		this.httpBridge.subscribeCycle(1, //
				this.createEndpoint(GET, this.goeApiV2.getStatusUrl()), //
				t -> this.filterGoeRequestForFirmware(parseToJsonObject(t.data())),
				t -> this._setChargingstationCommunicationFailed(true));
		this._setMinimumPower(EvcsUtils.milliampereToWatt(this.config.minHwCurrent(), 3));
		this._setMaximumPower(EvcsUtils.milliampereToWatt(this.config.maxHwCurrent(), 3));
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		super.deactivate();
	}

	private void installStateListener() {
		this.channel(EvcsGoe.ChannelId.GOE_STATE).onUpdate(this.statusConverter::applyGoeStatus);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> {
			this.calculateEnergy.update(this.getActivePower().get());
			this.calculateEnergySession.update(this.statusConverter.isVehicleConnected());
			this.updateErrorChannels();
		}
		}
	}

	/**
	 * The Filtering of the Statusrequest has a different formatting in
	 * Firmwareversion below 51.3. The only way to retrieve the Firmwareversion is
	 * to request all data from the Evcs. Afterward the Status will be requested in
	 * a filtered manner.
	 * 
	 * @param json Response from the goe
	 */
	private void filterGoeRequestForFirmware(JsonObject json) {
		if (json == null) {
			this._setChargingstationCommunicationFailed(true);
		} else {
			try {
				Float fwVersion = JsonUtils.getAsFloat(json, "fwv");
				boolean legacy = fwVersion <= LEGACY_FIRMWARE;
				this._setChargingstationCommunicationFailed(false);

				// set up new request format depending on the firmware version.
				this.httpBridge.removeAllCycleEndpoints();
				this.httpBridge.subscribeCycle(1, //
						this.createEndpoint(GET, this.goeApiV2.getFilteredStatusUrl(legacy)), //
						t -> this.handleStatusRequest(parseToJsonObject(t.data())),
						t -> this._setChargingstationCommunicationFailed(true));
			} catch (Exception e) {
				this._setChargingstationCommunicationFailed(true);
			}
		}
	}

	private void handleStatusRequest(JsonObject json) {
		if (json == null) {
			this._setChargingstationCommunicationFailed(true);
			return;
		}
		try {
			// status
			var status = JsonUtils.getAsInt(json, "car");
			this.channel(EvcsGoe.ChannelId.GOE_STATE).setNextValue(status);

			// active current
			var activeCurrent = JsonUtils.getAsInt(json, "amp") * 1000;
			this.channel(EvcsGoeHttp.ChannelId.CURR_USER).setNextValue(activeCurrent);

			// phase voltages and currents
			var nrg = JsonUtils.getAsJsonArray(json, "nrg");
			var voltageL1 = JsonUtils.getAsInt(nrg, 0) * 1000;
			var voltageL2 = JsonUtils.getAsInt(nrg, 1) * 1000;
			var voltageL3 = JsonUtils.getAsInt(nrg, 2) * 1000;
			setPhaseRotatedVoltageChannels(this, voltageL1, voltageL2, voltageL3);

			var currentL1 = JsonUtils.getAsInt(nrg, 4) * 1000;
			var currentL2 = JsonUtils.getAsInt(nrg, 5) * 1000;
			var currentL3 = JsonUtils.getAsInt(nrg, 6) * 1000;
			setPhaseRotatedCurrentChannels(this, currentL1, currentL2, currentL3);

			// active power
			var power = JsonUtils.getAsInt(nrg, 11);
			this._setActivePower(power);

			// Error
			this.channel(EvcsGoe.ChannelId.ERROR).setNextValue(JsonUtils.getAsInt(json, "err"));
			this._setChargingstationCommunicationFailed(false);

		} catch (Exception e) {
			this._setChargingstationCommunicationFailed(true);
		}

	}

	private void updateErrorChannels() {
		// we want to report charging station errors to the customer
		this._setError(this.getStatus() == Status.ERROR);
	}

	protected BridgeHttp.Endpoint createEndpoint(HttpMethod httpMethod, String url) {
		return new BridgeHttp.Endpoint(//
				url, //
				httpMethod, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, //
				"", emptyMap());
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public MeterType getMeterType() {
		if (this.config.readOnly()) {
			return MeterType.CONSUMPTION_METERED;
		} else {
			return MeterType.MANAGED_CONSUMPTION_METERED;
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

}
