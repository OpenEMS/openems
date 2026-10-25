package io.openems.edge.sonnen.core;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sonnen.ess.EssSonnen;
import io.openems.edge.sonnen.meter.MeterSonnen;
import io.openems.edge.sonnen.pv.PvSonnen;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Sonnen.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
public class SonnenCoreImpl extends AbstractOpenemsComponent
		implements SonnenCore, OpenemsComponent, EventHandler {

	private static final String STATUS_URL_PART = "/api/v2/status";
	private static final String POWERMETER_URL_PART = "/api/v2/powermeter";
	private static final String CONFIGURATIONS_URL_PART = "/api/v2/configurations";
	private static final String SETPOINT_URL_PART = "/api/v2/setpoint/";

	private final Logger log = LoggerFactory.getLogger(SonnenCoreImpl.class);

	private final AtomicReference<EssSonnen> ess = new AtomicReference<>();
	private final AtomicReference<MeterSonnen> meter = new AtomicReference<>();
	private final AtomicReference<PvSonnen> pv = new AtomicReference<>();

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private String baseUrl;
	private String apiToken;
	private int readInterval;
	private long lastReadTime = 0L;

	// Values of the last successfully executed write-commands
	private OperatingMode lastOperatingMode = null;
	private Integer lastChargeSetpoint = null;
	private Integer lastDischargeSetpoint = null;

	public SonnenCoreImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				CoreChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!config.enabled()) {
			return;
		}
		this.baseUrl = "http://" + config.ipAddress() + ":" + config.port();
		this.apiToken = config.apiToken();
		this.readInterval = config.readInterval();
		this.httpBridge = this.httpBridgeFactory.get();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || this.httpBridge == null) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.pollData();
		}
	}

	private void pollData() {
		var now = System.currentTimeMillis();
		if (now - this.lastReadTime < this.readInterval * 1000L) {
			return;
		}
		this.lastReadTime = now;

		this.readStatus();
		if (this.getMeter().isPresent() || this.getPv().isPresent()) {
			this.readPowermeter();
		}
	}

	/**
	 * Reads the '/status' endpoint and distributes the values to the registered
	 * components.
	 */
	private void readStatus() {
		final var endpoint = BridgeHttp.create(this.baseUrl + STATUS_URL_PART) //
				.onlyIf(!this.apiToken.isEmpty(), b -> b.setHeader("Auth-Token", this.apiToken)) //
				.build();

		this.httpBridge.requestJson(endpoint).whenComplete((response, error) -> {
			if (error != null) {
				this.log.warn("Error reading status from sonnenBatterie: " + error.getMessage());
				this._setSlaveCommunicationFailed(true);
				return;
			}

			final var status = SonnenStatus.from(response.data());
			this._setSlaveCommunicationFailed(false);

			this.getEss().ifPresent(ess -> ess.updateFromStatus(status));
			this.getMeter().ifPresent(meter -> meter.updateFromStatus(status));
			this.getPv().ifPresent(pv -> pv.updateFromStatus(status));
		});
	}

	/**
	 * Reads the '/powermeter' endpoint and distributes the values to the
	 * registered meter and pv components.
	 */
	private void readPowermeter() {
		final var endpoint = BridgeHttp.create(this.baseUrl + POWERMETER_URL_PART) //
				.onlyIf(!this.apiToken.isEmpty(), b -> b.setHeader("Auth-Token", this.apiToken)) //
				.build();

		this.httpBridge.requestJson(endpoint).whenComplete((response, error) -> {
			if (error != null) {
				this.log.warn("Error reading powermeter from sonnenBatterie: " + error.getMessage());
				this._setSlaveCommunicationFailed(true);
				return;
			}

			final var data = response.data();
			if (data == null || !data.isJsonArray()) {
				return;
			}

			JsonArray array = data.getAsJsonArray();
			SonnenPowermeter consumption = null;
			SonnenPowermeter production = null;
			for (var element : array) {
				final var entry = SonnenPowermeter.from(element);
				if (entry.isConsumption()) {
					consumption = entry;
				} else {
					production = entry;
				}
			}

			final var finalConsumption = consumption;
			final var finalProduction = production;
			this.getMeter().ifPresent(meter -> meter.updateFromPowermeter(finalConsumption));
			this.getPv().ifPresent(pv -> pv.updateFromPowermeter(finalProduction));
		});
	}

	@Override
	public synchronized CompletableFuture<Void> setOperatingMode(OperatingMode mode) {
		if (mode == this.lastOperatingMode) {
			return CompletableFuture.completedFuture(null);
		}
		final var future = this.executeWriteCommand(//
				BridgeHttp.create(this.baseUrl + CONFIGURATIONS_URL_PART) //
						.setMethod(HttpMethod.PUT) //
						.setHeader("Content-Type", "application/x-www-form-urlencoded") //
						.setBody("EM_OperatingMode=" + mode.getValue()) //
						.build());
		return future.thenAccept(v -> this.lastOperatingMode = mode);
	}

	@Override
	public synchronized CompletableFuture<Void> setChargeSetpoint(int watts) {
		if (Integer.valueOf(watts).equals(this.lastChargeSetpoint)) {
			return CompletableFuture.completedFuture(null);
		}
		final var future = this.executeWriteCommand(//
				BridgeHttp.create(this.baseUrl + SETPOINT_URL_PART + "charge/" + watts) //
						.setMethod(HttpMethod.POST) //
						.build());
		return future.thenAccept(v -> this.lastChargeSetpoint = watts);
	}

	@Override
	public synchronized CompletableFuture<Void> setDischargeSetpoint(int watts) {
		if (Integer.valueOf(watts).equals(this.lastDischargeSetpoint)) {
			return CompletableFuture.completedFuture(null);
		}
		final var future = this.executeWriteCommand(//
				BridgeHttp.create(this.baseUrl + SETPOINT_URL_PART + "discharge/" + watts) //
						.setMethod(HttpMethod.POST) //
						.build());
		return future.thenAccept(v -> this.lastDischargeSetpoint = watts);
	}

	/**
	 * Executes a write-command with the 'Auth-Token' header; sets the
	 * {@link CoreChannelId#WRITE_COMMAND_FAILED} channel on error and clears the
	 * dedup-value, so that the command is re-tried on the next call.
	 *
	 * @param endpoint the {@link BridgeHttp.Endpoint}
	 * @return the future
	 */
	private CompletableFuture<Void> executeWriteCommand(BridgeHttp.Endpoint endpoint) {
		if (this.apiToken.isEmpty()) {
			this.log.warn("Unable to execute write-command on sonnenBatterie: API token is not set.");
			this._setWriteCommandFailed(true);
			return CompletableFuture.completedFuture(null);
		}
		endpoint = endpoint.toBuilder() //
				.setHeader("Auth-Token", this.apiToken) //
				.build();

		return this.httpBridge.request(endpoint).handle((response, error) -> {
			if (error != null || response.status().isError()) {
				this.log.warn("Write-command on sonnenBatterie failed: "
						+ (error != null ? error.getMessage() : response.status().toString()));
				this._setWriteCommandFailed(true);
				this.lastOperatingMode = null;
				this.lastChargeSetpoint = null;
				this.lastDischargeSetpoint = null;
			} else {
				this._setWriteCommandFailed(false);
			}
			return null;
		});
	}

	@Override
	public void setEss(EssSonnen ess) {
		this.ess.set(ess);
	}

	@Override
	public Optional<EssSonnen> getEss() {
		return Optional.ofNullable(this.ess.get());
	}

	@Override
	public void setMeter(MeterSonnen meter) {
		this.meter.set(meter);
	}

	@Override
	public Optional<MeterSonnen> getMeter() {
		return Optional.ofNullable(this.meter.get());
	}

	@Override
	public void setPv(PvSonnen pv) {
		this.pv.set(pv);
	}

	@Override
	public Optional<PvSonnen> getPv() {
		return Optional.ofNullable(this.pv.get());
	}
}
