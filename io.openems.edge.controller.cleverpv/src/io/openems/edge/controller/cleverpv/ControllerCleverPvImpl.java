package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.util.Optional;

import io.openems.edge.common.meta.Meta;
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
	private static final int VOLT_PER_AMPERE = 230;

	private final CalculateActiveTime calculateCumulatedNoDischargeTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_NO_DISCHARGE_TIME);

	private final CalculateActiveTime calculateCumulatedChargeFromGridTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_CHARGE_FROM_GRID_TIME);

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
	protected Meta meta;

	@Reference
	private Sum sum;

	private Config config;

	private RemoteControlMode remoteControlMode = RemoteControlMode.OFF;
	private ActiveControlModes activeControlModes = null;
	private Integer limit = 0;
	private Integer maxChargePower = 0;

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
			case CHARGE_FROM_GRID -> {
				var limit = parsedResponse.activateControlModes().ess().chargePower();
				this.setActiveMode(RemoteControlMode.CHARGE_FROM_GRID, limit);
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
		if (this.config.readOnly()) {
			this.setDefaultMode();
			return;
		}

		switch (this.remoteControlMode) {
		case OFF, UNDEFINED -> {
			this.setDefaultMode();
		}
		case NO_DISCHARGE -> {
			if (!this.isNoDischargeAllowed()) {
				this.setDefaultMode();
			} else {
				setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.NO_DISCHARGE);
				this.setActiveTime(false, true, false);
				this.ess.setActivePowerEqualsWithPid(0);
			}
		}
		case CHARGE_FROM_GRID -> {
			if (!this.isEssChargeFromGridAllowed()) {
				this.setDefaultMode();
			} else {
				setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.CHARGE_FROM_GRID);
				this.setActiveTime(false, false, true);
				Integer absoluteLimitNegated = (Math.min(Math.abs(this.maxChargePower), Math.abs(this.limit)) * -1);
				this.ess.setActivePowerEquals(absoluteLimitNegated);
			}
		}
		}
	}

	private void setDefaultMode() {
		this.setActiveTime(true, false, false);
		setValue(this, ControllerCleverPv.ChannelId.REMOTE_CONTROL_MODE, RemoteControlMode.OFF);
	}

	private void setActiveTime(boolean inactive, boolean noDischarge, boolean ChargeFromGrid) {
		this.calculateCumulatedInactiveTime.update(inactive);
		this.calculateCumulatedNoDischargeTime.update(noDischarge);
		this.calculateCumulatedChargeFromGridTime.update(ChargeFromGrid);
	}

	protected SendData collectData() {
		final var currentData = CurrentData.fromComponentManager(this.componentManager);
		var currentlyActiveEssMode = new Ess(this.remoteControlMode, null, null);

		final AvailableControlModes availableControlModes;

		if (this.config.readOnly()) {
			return new SendData(currentData, null, this.activeControlModes, this.host, this.sum);
		}

		final var essModes = ImmutableList.<Ess>builder();
		if (this.isNoDischargeAllowed()) {
			essModes.add(new Ess(RemoteControlMode.NO_DISCHARGE, null, null));
		}

		if (this.isEssChargeFromGridAllowed()) {
			final int gridConnectionPointFuseLimit = this.meta.getGridConnectionPointFuseLimit(); // Ampere
			final int essMaxPower = this.ess.getPower().getMaxPower(this.ess, ALL, ACTIVE);
			final double safetyMargin = 0.1F; // 10% less than max for safety

			int maxGridFeedIn = Optional.of((int)Math.round(gridConnectionPointFuseLimit * (1 - safetyMargin)) * VOLT_PER_AMPERE * 3).orElse(0);
			int currentConsumption = Optional.ofNullable(this.sum.getConsumptionActivePower().get()).orElse(0);
			int currentProduction = Optional.ofNullable(this.sum.getProductionActivePower().get()).orElse(0);

			this.maxChargePower = Math.max(0,(maxGridFeedIn - currentConsumption + currentProduction));
			this.maxChargePower = (int)Math.round(Math.min(essMaxPower, this.maxChargePower) * (1 - safetyMargin));

			currentlyActiveEssMode = new Ess(this.remoteControlMode, null, Math.min(this.limit, this.maxChargePower));
			essModes.add(new Ess(RemoteControlMode.CHARGE_FROM_GRID, this.maxChargePower, null));
		}

		availableControlModes = new AvailableControlModes(essModes.build());
		this.activeControlModes = new ActiveControlModes(currentlyActiveEssMode);

		return new SendData(currentData, availableControlModes, this.activeControlModes, this.host, this.sum);
	}

	private boolean isNoDischargeAllowed() {
		return Optional.ofNullable(this.timeOfUseTariffController).map(TimeOfUseTariffController::getStateMachine)
				.map(s -> switch (s) {
					case BALANCING, DELAY_DISCHARGE -> true; // allow NO_DISCHARGE
					case CHARGE_GRID, DISCHARGE_GRID -> false; // no available modes in these cases
				}).orElse(true); // No ToU-Ctrl -> allow NO_DISCHARGE
	}

	private boolean isEssChargeFromGridAllowed() {
		if (!this.meta.getIsEssChargeFromGridAllowed()) {
			return false;
		}

		if (this.ess.getPower().getMaxPower(this.ess, ALL, ACTIVE) <= 0) {
			return false;
		}

		return true;
	}

	private void setActiveMode(RemoteControlMode mode, Integer limit) {
		this.remoteControlMode = mode;
		this.limit = 0;

		if (limit != null) {
			this.limit = limit;
		}

		if (mode == RemoteControlMode.NO_DISCHARGE) {
			this.limit = 0;
		}

		JsonUtils.JsonObjectBuilder builder = buildJsonObject().add("mode", JsonUtils.toJson(mode.toString()));

		builder.add("power", JsonUtils.toJson(this.limit));
	}

	private void resetControlMode() {
		this.remoteControlMode = RemoteControlMode.OFF;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
