package io.openems.edge.controller.cleverpv;

import static io.openems.common.utils.FunctionUtils.doNothing;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Clever-PV", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerCleverPvImpl extends AbstractOpenemsComponent
		implements ControllerCleverPv, Controller, OpenemsComponent {

	private static final int SEND_SECONDS = 15;
	private static final ChannelAddress GRID = new ChannelAddress("_sum", "GridActivePower");
	private static final ChannelAddress PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");
	private static final ChannelAddress ESS_SOC = new ChannelAddress("_sum", "EssSoc");
	private static final ChannelAddress ESS_POWER = new ChannelAddress("_sum", "EssActivePower");

	protected static enum PowerStorageState {
		IDLE(0), CHARGING(1), DISABLED(2), DISCHARGING(3);

		public final int value;

		private PowerStorageState(int value) {
			this.value = value;
		}
	}

	private final Logger log = LoggerFactory.getLogger(ControllerCleverPvImpl.class);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private ComponentManager componentManager;

	private String url;
	private Instant lastSend = Instant.MIN;
	private LogVerbosity logVerbosity = LogVerbosity.NONE;

	public ControllerCleverPvImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerCleverPv.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.url = config.url();
		this.logVerbosity = config.logVerbosity();
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
			var data = this.collectData();
			this.sendData(data);
			this.lastSend = now;
		}
	}

	protected JsonObject collectData() {
		final var essPower = this.getAsInt(ESS_POWER);
		return JsonUtils.buildJsonObject() //
				.add("watt", this.getAsJson(GRID)) //
				.add("producingWatt", this.getAsJson(PRODUCTION)) //
				.add("soc", this.getAsJson(ESS_SOC)) //
				.add("powerStorageState", //
						new JsonPrimitive(essPower.map(p -> {
							if (p > 0) {
								return PowerStorageState.DISCHARGING;
							} else if (p < 0) {
								return PowerStorageState.CHARGING;
							} else {
								return PowerStorageState.IDLE;
							}
						}).orElse(PowerStorageState.DISABLED).value)) //
				.add("chargingPower", essPower.<JsonElement>map(p -> new JsonPrimitive(Math.abs(p))) //
						.orElse(JsonNull.INSTANCE)) //
				.build();
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
				});
	}

	private void log(JsonObject data, HttpResponse<JsonElement> msg, Throwable ex) {
		switch (this.logVerbosity) {
		case NONE, DEBUG_LOG -> doNothing();
		case FULL_JSON_ON_SEND -> {
			var b = new StringBuilder("");
			b.append("Data:").append(data.toString());
			if (ex != null) {
				b.append("|").append("Error:").append(ex.getClass().getSimpleName()).append(":").append(ex.toString());
			}
			if (msg != null) {
				b.append("|").append("Response:").append(msg.toString());
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
}
