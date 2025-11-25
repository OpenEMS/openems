package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableList;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.cleverpv.Types.SendData;
import io.openems.edge.controller.cleverpv.Types.SendData.ActivateControlModes;
import io.openems.edge.controller.cleverpv.Types.SendData.ActiveControlModes;
import io.openems.edge.controller.cleverpv.Types.SendData.AvailableControlModes;
import io.openems.edge.controller.cleverpv.Types.SendData.CurrentData;
import io.openems.edge.controller.cleverpv.Types.SendData.Ess;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffController;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Clever-PV", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerCleverPvImpl extends AbstractOpenemsComponent
		implements TimedataProvider, ControllerCleverPv, Controller, OpenemsComponent {

	private static final int SEND_SECONDS = 5;

	private final CalculateActiveTime calculateCumulatedNoDischargeTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_NO_DISCHARGE_TIME);

	private final CalculateActiveTime calculateCumulatedInactiveTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_INACTIVE_TIME);

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policyOption = GREEDY, cardinality = OPTIONAL, target = "(enabled=true)")
	private volatile TimeOfUseTariffController timeOfUseTariffController;

	@Reference
	private Power power;

	@Reference
	private Host host;

	@Reference
	private Sum sum;

	private Config config;

	private RemoteControlMode remoteControlMode = RemoteControlMode.OFF;
	private ActiveControlModes activeControlModes = null;

	public ControllerCleverPvImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerCleverPv.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.setDebugMode(config.debugMode());

		this.sendData();
	}

	/**
	 * Send data and async parse response.
	 */
	private void sendData() {

		final var delay = DelayTimeProvider.Delay.of(Duration.ofSeconds(SEND_SECONDS));
		final var delayTime = new DefaultDelayTimeProvider(DelayTimeProvider.Delay::immediate, t -> delay, t -> delay);
		final var timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		timeService.subscribeJsonTime(delayTime, () -> {
			var data = this.collectData();
			return BridgeHttp.create(this.config.url()) //
					.setBodyJson(SendData.serializer().serialize(data)) //
					.build();
		}, (jsonHttpResponse, httpError) -> {
			setValue(this, ControllerCleverPv.ChannelId.UNABLE_TO_SEND, httpError != null);

			if (httpError != null || this.config.controlMode() != ControlMode.REMOTE_CONTROL) {
				this.resetControlMode();
				return;
			}

			var parsedResponse = SendData.Response.serializer().deserialize(jsonHttpResponse.data());

			if (parsedResponse.activateControlModes().ess() == null) {
				this.resetControlMode();
				return;
			}

			switch (parsedResponse.activateControlModes().ess().remoteControlMode()) {
			case OFF, UNDEFINED -> {
				this.resetControlMode();
			}
			case NO_DISCHARGE -> {
				this.setActiveMode(RemoteControlMode.NO_DISCHARGE, null);
			}
			}
		});
	}

	@Override
	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	public void run() throws OpenemsNamedException {

		if (!this.config.readOnly() && this.config.controlMode() == ControlMode.REMOTE_CONTROL) {
			switch (this.remoteControlMode) {
			case OFF, UNDEFINED -> {
				setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.OFF);
				this.calculateCumulatedNoDischargeTime.update(false);
				this.calculateCumulatedInactiveTime.update(true);
			}
			case NO_DISCHARGE -> {
				setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.NO_DISCHARGE);
				this.ess.setActivePowerEqualsWithPid(0);
				this.calculateCumulatedInactiveTime.update(false);
				this.calculateCumulatedNoDischargeTime.update(true);
			}
			}
		}

		if (this.remoteControlMode == RemoteControlMode.OFF) {
			this.calculateCumulatedInactiveTime.update(true);
			setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.OFF);
		}
	}

	protected SendData collectData() {
		final var currentData = CurrentData.fromComponentManager(this.componentManager);
		final var ess = new Ess(this.remoteControlMode);

		final AvailableControlModes availableControlModes;

		if (this.config.readOnly()) {
			return new SendData(currentData, null, this.activeControlModes, this.host, this.sum);
		}

		final var essModes = ImmutableList.<Ess>builder();
		if (Optional.ofNullable(this.timeOfUseTariffController) //
				.map(TimeOfUseTariffController::getStateMachine) //
				.map(s -> switch (s) {
				case BALANCING, DELAY_DISCHARGE -> true; // allow NO_DISCHARGE
				case CHARGE_GRID, DISCHARGE_GRID -> false; // no available modes in these cases
				}) //
				.orElse(true)) { // No ToU-Ctrl -> allow NO_DISCHARGE
			essModes.add(new Ess(RemoteControlMode.NO_DISCHARGE));
		}

		availableControlModes = new AvailableControlModes(essModes.build());
		this.activeControlModes = new ActiveControlModes(ess);

		return new SendData(currentData, availableControlModes, this.activeControlModes, this.host, this.sum);
	}

	private void setActiveMode(RemoteControlMode mode, Integer limit) {
		this.remoteControlMode = mode;
		int setLimit = 0;
		if (limit != null) {
			setLimit = limit;
		}

		if (mode == RemoteControlMode.NO_DISCHARGE) {
			setLimit = 0;
		}

		JsonUtils.JsonObjectBuilder builder = buildJsonObject().add("mode", JsonUtils.toJson(mode.toString()));

		builder.add("power", JsonUtils.toJson(setLimit));
	}

	private void resetControlMode() {
		this.remoteControlMode = RemoteControlMode.OFF;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
