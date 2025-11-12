package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsJsonObject;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
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
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
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
	private static final ChannelAddress GRID = new ChannelAddress("_sum", "GridActivePower");
	private static final ChannelAddress PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress("_sum", "EssSoc");
	private static final ChannelAddress ESS_POWER = new ChannelAddress("_sum", "EssDischargePower");

	protected ControlMode mode = ControlMode.OFF;
	protected JsonObject activeMode;

	private final Logger log = LoggerFactory.getLogger(ControllerCleverPvImpl.class);

	private final CalculateActiveTime calculateCumulatedNoDischargeTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_NO_DISCHARGE_TIME);

	private final CalculateActiveTime calculateCumulatedInactiveTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_INACTIVE_TIME);

	private final CalculateActiveTime calculateCumulatedForceChargeTime = new CalculateActiveTime(this,
			ControllerCleverPv.ChannelId.CUMULATED_FORCE_CHARGE_TIME);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
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

	// FORCE_CHARGE disabled till next release, but needed for testing
	// private int forceChargePower = 0;

	private String url;
	private Instant lastSend = Instant.MIN;
	private LogVerbosity logVerbosity = LogVerbosity.NONE;

	private Config config;

	public ControllerCleverPvImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerCleverPv.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.url = config.url();
		this.logVerbosity = config.logVerbosity();
		this.config = config;
		this.activeMode = JsonUtils.parseToJsonObject("{}");
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
		var now = Instant.now();

		if (Duration.between(this.lastSend, now).getSeconds() >= SEND_SECONDS) {

			JsonObject data = this.collectData();
			this.sendData(data);
			this.lastSend = now;
		}

		if (!this.config.readOnly() && this.config.mode() == Mode.REMOTE_CONTROL) {
			this.calculateCumulatedForceChargeTime.update(false);
			this.calculateCumulatedNoDischargeTime.update(false);
			this.calculateCumulatedInactiveTime.update(false);

			switch (this.mode) {
			case ControlMode.OFF, ControlMode.UNDEFINED -> {
				this.calculateCumulatedInactiveTime.update(true);
				this.setControlMode(ControlMode.OFF);
			}
			case ControlMode.NO_DISCHARGE -> {
				this.setMode(ControlMode.NO_DISCHARGE, 0);
				this.calculateCumulatedNoDischargeTime.update(true);
			}
			}
		}

		if (this.mode == ControlMode.OFF) {
			this.calculateCumulatedInactiveTime.update(true);
			this.setControlMode(ControlMode.OFF);
		}
	}

	private void setMode(ControlMode controlMode, int power) throws OpenemsNamedException {
		this.ess.setActivePowerEqualsWithPid(power);
		this.setControlMode(controlMode);
	}

	protected JsonObject collectData() {
		final var essPower = this.getAsInt(ESS_POWER);
		System.out.println(essPower);

		JsonObject data = buildJsonObject() //
				.add("watt", this.getAsJson(GRID)) //
				.add("producingWatt", this.getAsJson(PRODUCTION)).add("soc", this.getAsJson(ESS_SOC)) //
				.add("powerStorageState", new JsonPrimitive(essPower.map(PowerStorageState::fromPower).orElse(PowerStorageState.DISABLED).value))
				.add("chargingPower",
						essPower.<JsonElement>map(p -> new JsonPrimitive(Math.abs(p))).orElse(JsonNull.INSTANCE))
				.build();

		JsonObject currentData = buildJsonObject() //
				.add("_sum/GridActivePower", this.getAsJson(GRID)) //
				.add("_sum/ProductionActivePower", this.getAsJson(PRODUCTION)) //
				.add("_sum/EssSoc", this.getAsJson(ESS_SOC)) //
				.add("_sum/EssDcDischargePower", data.get("powerStorageState")).build(); //

		JsonObject essNoDischarge = buildJsonObject().add("mode", JsonUtils.toJson(ControlMode.NO_DISCHARGE.toString()))
				.build();

		JsonArray essArray = new JsonArray();

		if (this.timeOfUseTariffController != null) {

			switch (this.timeOfUseTariffController.getStateMachine()) {
			case StateMachine.BALANCING, StateMachine.DELAY_DISCHARGE -> {
				essArray.add(essNoDischarge);
			}
			case StateMachine.CHARGE_GRID, StateMachine.DISCHARGE_GRID -> {
				// currently no avaivable modes in these cases
			}
			}
		} else {
			essArray.add(essNoDischarge);
			;
		}

		JsonObject availableControlModes = buildJsonObject().add("ess", essArray).build();

		data.add("currentData", currentData);

		if (!this.config.readOnly()) {
			data.add("availableControlModes", availableControlModes);
			data.add("activeControlModes", this.activeMode);
		}

		return data;
	}

	private JsonElement getAsJson(ChannelAddress address) {
		try {
			var channel = this.componentManager.getChannel(address);
			return channel.value().asJson();
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			// ignore
			return JsonNull.INSTANCE;
		}
	}

	@SuppressWarnings("unchecked")
	private Optional<Integer> getAsInt(ChannelAddress address) {
		try {
			var channel = this.componentManager.getChannel(address);
			return (Optional<Integer>) channel.value().asOptional();
		} catch (IllegalArgumentException | OpenemsNamedException e) {
			// ignore
			return Optional.empty();
		}
	}

	private void sendData(JsonObject data) {
		this.httpBridge.postJson(this.url, data) //
				.whenComplete((msg, ex) -> {
					this.log(data, msg, ex);
					this.channel(ControllerCleverPv.ChannelId.UNABLE_TO_SEND).setNextValue(ex != null);

					if (ex != null || this.config.mode() != Mode.REMOTE_CONTROL) {
						this.resetControlMode();
						return;
					}

					JsonObject activateControlModes = null;

					try {
						activateControlModes = getAsJsonObject(msg.data(), "activateControlModes");
					} catch (OpenemsNamedException e) {
						throw new RuntimeException(e);
					}

					if (activateControlModes == null || !activateControlModes.has("ess")) {
						this.resetControlMode();
						return;
					}

					if (activateControlModes.get("ess").isJsonNull()) {
						this.resetControlMode();
						return;
					}

					JsonObject ess = activateControlModes.getAsJsonObject("ess");

					String mode = ess.has("mode") ? ess.get("mode").getAsString() : null;

					if (ControlMode.NO_DISCHARGE.toString().equals(mode)) {
						this.setActiveMode(ControlMode.NO_DISCHARGE, null);
					} else {
						this.resetControlMode();
					}
				});
	}

	private void setActiveMode(ControlMode mode, Integer limit) {
		this.mode = mode;
		int setLimit = 0;
		if (limit != null) {
			setLimit = limit;
		}

		if (mode == ControlMode.NO_DISCHARGE) {
			setLimit = 0;
		}

		JsonUtils.JsonObjectBuilder builder = buildJsonObject().add("mode", JsonUtils.toJson(mode.toString()));

		builder.add("power", JsonUtils.toJson(setLimit));

		this.activeMode.add("ess", builder.build());
	}

	private void resetControlMode() {
		this.activeMode.remove("ess");
		this.mode = ControlMode.OFF;
	}

	private void log(JsonObject data, HttpResponse<JsonElement> msg, Throwable ex) {
		switch (this.logVerbosity) {
		case NONE, DEBUG_LOG -> doNothing();
		case FULL_JSON_ON_SEND -> {
			var b = new StringBuilder();
			b.append("Data:").append(data.toString());
			if (ex != null) {
				b.append("|").append("Error:").append(ex.getClass().getSimpleName()).append(":").append(ex);
			}
			if (msg != null) {
				b.append("|").append("Response:").append(msg);
			}
			this.logInfo(this.log, b.toString());
		}
		}
	}

	@Override
	public String debugLog() {
		return switch (this.logVerbosity) {
		case NONE -> null;
		case DEBUG_LOG, FULL_JSON_ON_SEND -> {
			var b = new StringBuilder("Sent:");
			if (this.lastSend == Instant.MIN) {
				b.append("NEVER");
			} else {
				b.append(Duration.between(this.lastSend, Instant.now()).getSeconds()).append("s ago");
			}
			yield b.toString();
		}
		};
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
