package io.openems.edge.controller.ortsnetzauslastung;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.OpenemsConstants;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ortsnetzauslastung.Types.SendData;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ortsnetz-Auslastung", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({ "meter" })
public class ControllerOrtsnetzAuslastungImpl extends AbstractOpenemsComponent
		implements Controller, OpenemsComponent, ControllerOrtsnetzAuslastung {

	/** Send data every 5 minutes. */
	private static final int SEND_SECONDS = 5 * 60;
	private static final int INTEGRATION_VERSION_MAX_LENGTH = 32;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	protected Meta meta;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.meter_id})(enabled=true))")
	private ElectricityMeter meter;

	private Config config;

	private Coordinates coordinates = null;
	private HttpBridgeTimeService.TimeEndpoint<HttpResponse<String>> timeEndpoint = null;
	private volatile String lastHttpResult = "HTTP:WAITING";

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
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	@Override
	@Deactivate
	protected synchronized void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
			this.timeService = null;
			this.timeEndpoint = null;
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		final var coordinates = this.meta.getCoordinates();
		setValue(this, ControllerOrtsnetzAuslastung.ChannelId.NO_COORDINATES, coordinates == null);
		if (coordinates != null) {
			this.coordinates = coordinates;
		}

		if (coordinates == null || !this.hasPhaseVoltage()) {
			if (coordinates != null) {
				// coordinates are available, but no phase voltage to send
				setValue(this, ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND, true);
			}
			this.cancelSendData();
			return;
		}

		this.sendData();
	}

	/**
	 * Checks if at least one phase voltage is available; either from the phase
	 * specific channels or the symmetric voltage channel.
	 *
	 * @return true if a voltage is available
	 */
	private boolean hasPhaseVoltage() {
		return this.meter.getVoltageL1().get() != null //
				|| this.meter.getVoltage().get() != null;
	}

	/**
	 * Subscribes the periodic send task. The first request is sent immediately,
	 * all following every 5 minutes. Does nothing if the task is already
	 * subscribed.
	 */
	private void sendData() {
		if (this.timeEndpoint != null) {
			return;
		}

		final var delay = DelayTimeProvider.Delay.of(Duration.ofSeconds(SEND_SECONDS));
		final var delayTime = new DefaultDelayTimeProvider<HttpResponse<String>>(//
				DelayTimeProvider.Delay::immediate, //
				t -> delay, //
				t -> delay);

		this.timeEndpoint = this.timeService.subscribeJsonTime(delayTime, () -> {
			var data = this.collectData();
			return BridgeHttp.create(this.config.url()) //
					.setBodyJson(data.toJson()) //
					.build();
		}, (jsonHttpResponse, httpError) -> {
			this.lastHttpResult = switch (httpError) {
			case null -> "HTTP:" + jsonHttpResponse.status();
			case HttpError.ResponseError responseError -> "HTTP:" + responseError.status;
			case HttpError.UnknownError unknownError -> "HTTP:" + getRootCause(unknownError).getClass().getSimpleName();
			};
			setValue(this, ControllerOrtsnetzAuslastung.ChannelId.UNABLE_TO_SEND, httpError != null);
		});
	}

	/**
	 * Cancels the periodic send task. Does nothing if it is not subscribed.
	 */
	private void cancelSendData() {
		if (this.timeEndpoint == null) {
			return;
		}
		this.timeService.removeTimeEndpointIf(endpoint -> endpoint == this.timeEndpoint);
		this.timeEndpoint = null;
	}

	private static Throwable getRootCause(HttpError error) {
		var cause = (Throwable) error;
		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}
		return cause;
	}

	/**
	 * Collects the measurement data from the Meter, Meta and OEM components.
	 *
	 * <p>
	 * Note: coordinates must have been set before via {@link #run()}.
	 *
	 * @return the {@link SendData}
	 */
	protected SendData collectData() {
		final var l1V = this.getAsVolt(this.meter.getVoltageL1().get(), this.meter.getVoltage().get());
		final var l2V = this.getAsVolt(this.meter.getVoltageL2().get());
		final var l3V = this.getAsVolt(this.meter.getVoltageL3().get());
		final var frequency = this.getAsHertz(this.meter.getFrequency().get());

		return new SendData(//
				Instant.now().truncatedTo(ChronoUnit.SECONDS), //
				this.coordinates, //
				l1V, //
				l2V, //
				l3V, //
				frequency, //
				this.getIntegrationVersion());
	}

	/**
	 * Gets the L1 voltage in [V] from the given phase specific value in [mV]; or
	 * falls back to the symmetric voltage value if not available.
	 *
	 * @param voltageL1 the L1 voltage in [mV] or null
	 * @param voltage   the symmetric voltage in [mV] or null
	 * @return the voltage in [V] or null
	 */
	private static Double getAsVolt(Integer voltageL1, Integer voltage) {
		if (voltageL1 != null) {
			return voltageL1 / 1000.D;
		}
		if (voltage != null) {
			return voltage / 1000.D;
		}
		return null;
	}

	private static Double getAsVolt(Integer voltage) {
		return voltage == null ? null : voltage / 1000.D;
	}

	private static Double getAsHertz(Integer frequency) {
		return frequency == null ? null : frequency / 1000.D;
	}

	/**
	 * Builds the integration version identifier from the OEM model and the
	 * OpenEMS version; max 32 characters.
	 *
	 * @return the integration version
	 */
	private String getIntegrationVersion() {
		var version = this.oem.getManufacturerModel() + "-" + OpenemsConstants.VERSION;
		return StringUtils.toShortString(version, INTEGRATION_VERSION_MAX_LENGTH);
	}

	@Override
	public String debugLog() {
		if (this.config == null || !this.config.debugMode().isAtLeast(DebugMode.SIMPLE)) {
			return null;
		}
		return this.lastHttpResult;
	}
}
