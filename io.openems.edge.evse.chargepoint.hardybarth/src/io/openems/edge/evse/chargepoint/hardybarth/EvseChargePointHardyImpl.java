package io.openems.edge.evse.chargepoint.hardybarth;

import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_CONNECT_TIMEOUT;
import static io.openems.edge.bridge.http.api.BridgeHttp.DEFAULT_READ_TIMEOUT;
import static io.openems.edge.bridge.http.api.HttpMethod.GET;
import static io.openems.edge.bridge.http.api.HttpMethod.PUT;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evse.api.SingleThreePhase.THREE_PHASE;
import static java.util.Collections.emptyMap;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableList;

import io.openems.common.types.MeterType;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.PhaseRotation;
import io.openems.edge.evse.api.chargepoint.Profile;
import io.openems.edge.evse.api.chargepoint.Profile.Command;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.HardyBarth", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseChargePointHardyImpl extends AbstractOpenemsComponent implements EvseChargePointHardy,
		OpenemsComponent, EvseChargePoint, ElectricityMeter, TimedataProvider, EventHandler {

	protected final ReadUtils readUtils = new ReadUtils(this);

	private Config config = null;
	private BridgeHttp httpBridge;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	public EvseChargePointHardyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvseChargePointHardy.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values());

		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeCycle(1, //
				this.createEndpoint(GET, "/api", null), //
				t -> this.readUtils.handleGetApiCallResponse(t), //
				t -> FunctionUtils.doNothing());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	private Endpoint createEndpoint(HttpMethod httpMethod, String url, String body) {
		return createEndpoint(this.config.ip(), httpMethod, url, body);
	}

	protected static Endpoint createEndpoint(String ip, HttpMethod httpMethod, String url, String body) {
		return new Endpoint(//
				new StringBuilder("http://").append(ip).append(url).toString(), //
				httpMethod, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, //
				body, //
				emptyMap());
	}

	/**
	 * Set manual mode.
	 * 
	 * <p>
	 * Sets the chargemode to manual if not set.
	 */
	private void setManualMode() {
		StringReadChannel channelChargeMode = this.channel(EvseChargePointHardy.ChannelId.RAW_SALIA_CHARGE_MODE);
		Optional<String> valueOpt = channelChargeMode.value().asOptional();
		if (valueOpt.map(t -> !t.equals("manual")).orElse(true)) {
			return;
		}
		this.httpBridge //
				.requestJson(this.createEndpoint(PUT, "/api/secc", "{\"salia/chargemode\":\"manual\"}"));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
			-> this.setManualMode();
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.CONSUMPTION_METERED;
	}

	@Override
	public ChargeParams getChargeParams() {
		var isReadyForCharging = this.getIsReadyForCharging();
		var params = createChargeParams(isReadyForCharging);
		return params;
	}

	/**
	 * Creates the chargeParams.
	 * 
	 * @param isReadyForCharging is ready for charging?
	 * @return the {@link ChargeParams}
	 */
	protected static ChargeParams createChargeParams(boolean isReadyForCharging) {
		var limit = new Limit(THREE_PHASE, 6000, 16000);
		var profiles = ImmutableList.<Profile>builder();
		// no plug info available
		var params = new ChargeParams(isReadyForCharging, limit, profiles.build());

		return params;
	}

	@Override
	public void apply(int current, ImmutableList<Command> profileCommands) {
		// TODO find a way to provide only full [A] values
		final var currentInA = (int) Math.round(current / 1000f);
		this.handleApplyCharge(currentInA);
	}

	private void handleApplyCharge(int current) {
		this.httpBridge.requestJson(//
				this.createEndpoint(PUT, "/api/secc", "{\"" + "grid_current_limit" + "\":\"" + current + "\"}"));
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

}
