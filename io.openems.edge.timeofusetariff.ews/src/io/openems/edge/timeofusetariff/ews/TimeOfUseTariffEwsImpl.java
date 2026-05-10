package io.openems.edge.timeofusetariff.ews;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.TimeOfUsePrices.EMPTY_PRICES;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.ParserConfigurationException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.HttpStatus;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.ews.Utils.EwsDelayTimeProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Ews", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffEwsImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffEws {

	private static final String EWS_API_URL = "https://api.ews-schoenau.de/v1/dynamicprices/EWS-OEKO-DYN";
	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffEwsImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;

	private LogVerbosity logVerbosity;
	private Endpoint endpoint;

	public TimeOfUseTariffEwsImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffEws.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.logVerbosity = config.logVerbosity();
		this.endpoint = BridgeHttp.create(EWS_API_URL) //
				.setHeader("X-API-KEY", config.accessToken()) //
				.build();

		if (!config.enabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		this.timeService.subscribeTime(//
				new EwsDelayTimeProvider(this.componentManager.getClock()), //
				this.endpoint, //
				this::handleResponse, //
				this::handleError);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}

	protected void handleResponse(HttpResponse<String> response)
			throws OpenemsNamedException, ParserConfigurationException, SAXException, IOException {
		switch (this.logVerbosity) {
		case NONE -> doNothing();
		case TRACE -> this.logInfo(this.log, response.toString());
		}

		setValue(this, TimeOfUseTariffEws.ChannelId.HTTP_STATUS_CODE, response.status().code());
		setValue(this, TimeOfUseTariffEws.ChannelId.STATUS_AUTHENTICATION_FAILED, false);
		setValue(this, TimeOfUseTariffEws.ChannelId.STATUS_SERVER_ERROR, false);

		// Parse the response for the prices
		this.prices.set(Utils.parsePrices(response.data()));
	}

	private void handleError(HttpError error) {
		switch (this.logVerbosity) {
		case NONE -> doNothing();
		case TRACE -> this.logInfo(this.log, error.toString());
		}

		final var httpStatusCode = switch (error) {
		case HttpError.ResponseError re -> re.status.code();
		default -> INTERNAL_ERROR;
		};

		setValue(this, TimeOfUseTariffEws.ChannelId.HTTP_STATUS_CODE, httpStatusCode);
		final var authenticationFailed = httpStatusCode == HttpStatus.UNAUTHORIZED.code();
		setValue(this, TimeOfUseTariffEws.ChannelId.STATUS_AUTHENTICATION_FAILED, authenticationFailed);
		setValue(this, TimeOfUseTariffEws.ChannelId.STATUS_SERVER_ERROR, !authenticationFailed);

		this.logWarn(this.log, "Unable to Update Entsoe Time-Of-Use Price: " + error.getMessage());
	}
}
