package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.common.utils.JsonUtils.getAsOptionalBoolean;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonObject;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS_L1;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS_L2;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.GRID_STATUS_L3;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.LOCATION_BLOCKED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_COORDINATES;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_GRID_METER;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.NO_VOLTAGE;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.REDISPATCH_ACTIVE;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.REQUEST_REJECTED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.STORAGE_RECOMMENDATION;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.THIRD_PARTY_USAGE_NOT_ACCEPTED;
import static io.openems.edge.controller.ortsnetzauslastung.ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.types.DebugMode;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ortsnetzauslastung.Types.GridStatus;
import io.openems.edge.controller.ortsnetzauslastung.Types.SendData;
import io.openems.edge.controller.ortsnetzauslastung.Types.StaticData;
import io.openems.edge.controller.ortsnetzauslastung.Types.StorageRecommendation;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ortsnetz-Auslastung", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerOrtsnetzAuslastungImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ControllerOrtsnetzAuslastung {

	/** URL of the measurements API. */
	private static final String URL = "https://www.ortsnetz-auslastung.de/v1/measurements";

	/** Send interval as defined by the API. */
	protected static final Duration SEND_INTERVAL = Duration.ofMinutes(5);

	private static final int INTEGRATION_VERSION_MAX_LENGTH = 32;
	private static final int SMARTMETER_MODEL_MAX_LENGTH = 120;

	/* Valid ranges as defined by the API; in OpenEMS channel units */
	private static final int MAX_VOLTAGE = 500_000; // [mV]
	private static final int MIN_FREQUENCY = 45_000; // [mHz]
	private static final int MAX_FREQUENCY = 55_000; // [mHz]

	private static final int HTTP_FORBIDDEN = 403;
	private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

	private final Logger log = LoggerFactory.getLogger(ControllerOrtsnetzAuslastungImpl.class);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private Meta meta;

	@Reference
	private Sum sum;

	@Reference
	private ComponentManager componentManager;

	private Config config;

	/** Result of the last request; set by the HTTP thread, applied in run(). */
	private final AtomicReference<SendResult> pendingResult = new AtomicReference<>();
	private final AtomicBoolean requestRunning = new AtomicBoolean(false);

	private final IntervalStatistics l1VStatistics = new IntervalStatistics();
	private final IntervalStatistics l2VStatistics = new IntervalStatistics();
	private final IntervalStatistics l3VStatistics = new IntervalStatistics();
	private final IntervalStatistics frequencyStatistics = new IntervalStatistics();

	private Instant nextSend = null;
	private StaticData lastSentStaticData = null;
	private String smartmeterModelFactoryPid = null;
	private String smartmeterModel = null;
	private String lastHttpResult = "HTTP:WAITING";

	private record SendResult(HttpResponse<String> response, Throwable error, StaticData staticData) {
	}

	public ControllerOrtsnetzAuslastungImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerOrtsnetzAuslastung.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.isEnabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.setDebugMode(config.debugMode());
	}

	@Override
	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.applyResult(this.pendingResult.getAndSet(null));

		final var coordinates = this.meta.getCoordinates();
		final var isAccepted = this.meta.getThirdPartyUsageAcceptance() == ThirdPartyUsageAcceptance.ACCEPTED;
		final var meter = this.findGridMeter();
		setValue(this, NO_COORDINATES, coordinates == null);
		setValue(this, THIRD_PARTY_USAGE_NOT_ACCEPTED, !isAccepted);
		setValue(this, NO_GRID_METER, meter == null);

		final var hasVoltage = meter != null && this.addSamples(meter);
		setValue(this, NO_VOLTAGE, meter != null && !hasVoltage);

		if (coordinates == null || !isAccepted || !hasVoltage) {
			// Nothing is sent; a previous send error is no longer relevant
			setValue(this, UNABLE_TO_SEND, false);
			return;
		}

		final var now = Instant.now(this.componentManager.getClock());
		if (this.requestRunning.get() || this.nextSend != null && now.isBefore(this.nextSend)) {
			return;
		}
		this.nextSend = now.plus(SEND_INTERVAL);

		final SendData data;
		try {
			data = this.collectData(now, coordinates, meter);
		} catch (RuntimeException e) {
			this.logWarn(this.log, "Unable to collect data: " + e.getMessage());
			setValue(this, UNABLE_TO_SEND, true);
			return;
		} finally {
			this.resetStatistics();
		}
		this.send(data);
	}

	/**
	 * Finds the Meter at the grid connection point.
	 *
	 * @return the first enabled {@link ElectricityMeter} of type GRID (sorted by
	 *         Component-ID); null if none exists
	 */
	private ElectricityMeter findGridMeter() {
		final var meterId = this.config.meter_id();
		return this.componentManager.getEnabledComponentsOfType(ElectricityMeter.class).stream() //
				.filter(meterId.isBlank() //
						? m -> m.getMeterType() == MeterType.GRID //
						: m -> m.id().equals(meterId)) //
				.min(Comparator.comparing(OpenemsComponent::id)) //
				.orElse(null);
	}

	/**
	 * Adds the current voltages and the frequency to the interval statistics.
	 *
	 * @param meter the grid meter
	 * @return true if at least one phase voltage is valid
	 */
	private boolean addSamples(ElectricityMeter meter) {
		final var l1V = validVoltage(meter.getVoltageL1().get());
		final var l2V = validVoltage(meter.getVoltageL2().get());
		final var l3V = validVoltage(meter.getVoltageL3().get());
		this.l1VStatistics.add(l1V);
		this.l2VStatistics.add(l2V);
		this.l3VStatistics.add(l3V);
		this.frequencyStatistics.add(validFrequency(meter.getFrequency().get()));
		return l1V != null || l2V != null || l3V != null;
	}

	private void resetStatistics() {
		this.l1VStatistics.reset();
		this.l2VStatistics.reset();
		this.l3VStatistics.reset();
		this.frequencyStatistics.reset();
	}

	/**
	 * Collects the measurement data from the grid meter, Sum, Meta and OEM.
	 *
	 * @param now         the current time
	 * @param coordinates the location
	 * @param meter       the grid meter
	 * @return the {@link SendData}
	 */
	protected SendData collectData(Instant now, Coordinates coordinates, ElectricityMeter meter) {
		final var l1V = validVoltage(meter.getVoltageL1().get());
		final var l2V = validVoltage(meter.getVoltageL2().get());
		final var l3V = validVoltage(meter.getVoltageL3().get());
		final var staticData = this.getStaticData(meter);

		return new SendData(//
				now.truncatedTo(ChronoUnit.SECONDS), //
				coordinates, //
				divide(l1V, 1000), //
				divide(l2V, 1000), //
				divide(l3V, 1000), //
				this.l1VStatistics.get(1000), //
				this.l2VStatistics.get(1000), //
				this.l3VStatistics.get(1000), //
				divide(validFrequency(meter.getFrequency().get()), 1000), //
				this.frequencyStatistics.get(1000), //
				meter.getActivePower().get(), //
				l1V == null ? null : currentInAmpere(meter.getCurrentL1().get()), //
				l2V == null ? null : currentInAmpere(meter.getCurrentL2().get()), //
				l3V == null ? null : currentInAmpere(meter.getCurrentL3().get()), //
				validSoc(this.sum.getEssSoc().get()), //
				Objects.equals(staticData, this.lastSentStaticData) ? null : staticData, //
				getIntegrationVersion(this.oem.getManufacturerModel(), OpenemsConstants.VERSION.toString()));
	}

	/**
	 * Gets the static data of the location.
	 *
	 * @param meter the grid meter
	 * @return the {@link StaticData}
	 */
	private StaticData getStaticData(ElectricityMeter meter) {
		final var batteryCapacity = this.sum.getEssCapacity().get();
		return new StaticData(//
				batteryCapacity != null && batteryCapacity > 0 ? batteryCapacity / 1000. : null, //
				this.getSmartmeterModel(meter));
	}

	/**
	 * Gets the model of the grid meter from the name of its Factory.
	 *
	 * @param meter the grid meter
	 * @return the model or null
	 */
	private String getSmartmeterModel(ElectricityMeter meter) {
		final var factoryPid = meter.serviceFactoryPid();
		if (factoryPid == null) {
			return null;
		}
		if (!factoryPid.equals(this.smartmeterModelFactoryPid)) {
			// EdgeConfig is expensive; read only if the meter changed
			final var factory = this.componentManager.getEdgeConfig().getFactories().get(factoryPid);
			final var name = factory != null && !factory.getName().isBlank() ? factory.getName() : factoryPid;
			this.smartmeterModel = truncate(name, SMARTMETER_MODEL_MAX_LENGTH);
			this.smartmeterModelFactoryPid = factoryPid;
		}
		return this.smartmeterModel;
	}

	/**
	 * Sends the data asynchronously. The result is applied in the next
	 * {@link #run()}.
	 *
	 * @param data the {@link SendData}
	 */
	private void send(SendData data) {
		final var endpoint = BridgeHttp.create(URL) //
				.setBodyJson(data.toJson()) //
				.build();
		this.requestRunning.set(true);
		this.httpBridge.request(endpoint).whenComplete((response, error) -> {
			this.pendingResult.set(new SendResult(response, error, data.staticData()));
			this.requestRunning.set(false);
		});
	}

	/**
	 * Applies the result of a request to the Channels.
	 *
	 * @param result the {@link SendResult}; null if there is none
	 */
	private void applyResult(SendResult result) {
		if (result == null) {
			return;
		}
		var error = result.error();
		if (error instanceof CompletionException && error.getCause() != null) {
			error = error.getCause();
		}

		switch (error) {
		case null -> {
			this.lastHttpResult = "HTTP:" + result.response().status();
			if (result.staticData() != null) {
				this.lastSentStaticData = result.staticData();
			}
			this.setRequestStates(false, false, false);
			this.applyResponse(result.response().data());
		}
		case HttpError.ResponseError e -> {
			this.lastHttpResult = "HTTP:" + e.status;
			final var code = e.status.code();
			if (code == HTTP_UNPROCESSABLE_ENTITY) {
				this.logWarn(this.log, "Measurement rejected: " + e.body);
			}
			this.setRequestStates(code != HTTP_FORBIDDEN && code != HTTP_UNPROCESSABLE_ENTITY,
					code == HTTP_FORBIDDEN, code == HTTP_UNPROCESSABLE_ENTITY);
			this.applyResponse(null);
		}
		default -> {
			this.lastHttpResult = "HTTP:" + getRootCause(error).getClass().getSimpleName();
			this.setRequestStates(true, false, false);
			this.applyResponse(null);
		}
		}
	}

	private void setRequestStates(boolean unableToSend, boolean locationBlocked, boolean requestRejected) {
		setValue(this, UNABLE_TO_SEND, unableToSend);
		setValue(this, LOCATION_BLOCKED, locationBlocked);
		setValue(this, REQUEST_REJECTED, requestRejected);
	}

	/**
	 * Applies the API response body to the Channels. Unknown or missing values set
	 * the Channels to undefined.
	 *
	 * @param body the response body; may be null or not JSON
	 */
	private void applyResponse(String body) {
		var status = GridStatus.UNDEFINED;
		var statusL1 = GridStatus.UNDEFINED;
		var statusL2 = GridStatus.UNDEFINED;
		var statusL3 = GridStatus.UNDEFINED;
		var recommendation = StorageRecommendation.UNDEFINED;
		Boolean redispatchActive = null;

		if (body != null && !body.isBlank()) {
			try {
				final var json = JsonUtils.parseToJsonObject(body);
				final var statusJson = getAsOptionalJsonObject(json, "status");
				status = GridStatus.fromApi(statusJson.flatMap(s -> getAsOptionalString(s, "overall")).orElse(null));
				statusL1 = GridStatus.fromApi(statusJson.flatMap(s -> getAsOptionalString(s, "l1")).orElse(null));
				statusL2 = GridStatus.fromApi(statusJson.flatMap(s -> getAsOptionalString(s, "l2")).orElse(null));
				statusL3 = GridStatus.fromApi(statusJson.flatMap(s -> getAsOptionalString(s, "l3")).orElse(null));
				recommendation = StorageRecommendation
						.fromApi(getAsOptionalString(json, "storage_recommendation").orElse(null));
				redispatchActive = getAsOptionalJsonObject(json, "redispatch") //
						.flatMap(r -> getAsOptionalBoolean(r, "active")) //
						.orElse(null);
			} catch (OpenemsNamedException | RuntimeException e) {
				// The measurement was accepted; only the optional response is unreadable
				this.logWarn(this.log, "Unable to parse response: " + e.getMessage());
			}
		}

		setValue(this, GRID_STATUS, status);
		setValue(this, GRID_STATUS_L1, statusL1);
		setValue(this, GRID_STATUS_L2, statusL2);
		setValue(this, GRID_STATUS_L3, statusL3);
		setValue(this, STORAGE_RECOMMENDATION, recommendation);
		setValue(this, REDISPATCH_ACTIVE, redispatchActive);
	}

	private static Throwable getRootCause(Throwable error) {
		var cause = error;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}

	/**
	 * Builds the integration version from the OEM model and the OpenEMS version;
	 * max 32 characters. If too long, the model is shortened, so that the version
	 * is kept.
	 *
	 * @param model   the OEM model
	 * @param version the OpenEMS version
	 * @return the integration version
	 */
	protected static String getIntegrationVersion(String model, String version) {
		final var suffix = "-" + version;
		final var maxModelLength = INTEGRATION_VERSION_MAX_LENGTH - suffix.length();
		if (model == null || model.isBlank() || maxModelLength <= 0) {
			return truncate(version, INTEGRATION_VERSION_MAX_LENGTH);
		}
		return truncate(model, maxModelLength) + suffix;
	}

	private static String truncate(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	/**
	 * Validates a voltage.
	 *
	 * @param voltage the voltage in [mV]
	 * @return the voltage in [mV] if it is &gt; 0 and &lt;= 500 V; otherwise null
	 */
	protected static Integer validVoltage(Integer voltage) {
		return voltage != null && voltage > 0 && voltage <= MAX_VOLTAGE ? voltage : null;
	}

	/**
	 * Validates a frequency.
	 *
	 * @param frequency the frequency in [mHz]
	 * @return the frequency in [mHz] if it is within 45 to 55 Hz; otherwise null
	 */
	protected static Integer validFrequency(Integer frequency) {
		return frequency != null && frequency >= MIN_FREQUENCY && frequency <= MAX_FREQUENCY ? frequency : null;
	}

	private static Integer validSoc(Integer soc) {
		return soc != null && soc >= 0 && soc <= 100 ? soc : null;
	}

	private static Double currentInAmpere(Integer current) {
		return current == null ? null : Math.abs(current) / 1000.;
	}

	private static Double divide(Integer value, double divisor) {
		return value == null ? null : value / divisor;
	}

	@Override
	public String debugLog() {
		if (this.config == null || !this.config.debugMode().isAtLeast(DebugMode.SIMPLE)) {
			return null;
		}
		return this.lastHttpResult;
	}
}
