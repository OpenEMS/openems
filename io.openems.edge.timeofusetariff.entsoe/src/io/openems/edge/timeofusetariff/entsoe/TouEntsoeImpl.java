package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.common.utils.StringUtils.definedOrElse;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.TouManualHelper.EMPTY_TOU_MANUAL_HELPER;
import static io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi.getExchangeRateOrElse;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseCurrency;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parsePrices;
import static io.openems.edge.timeofusetariff.entsoe.Utils.parseToSchedule;
import static io.openems.edge.timeofusetariff.entsoe.Utils.processPrices;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

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
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.api.TouManualHelper;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.ENTSO-E", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TouEntsoeImpl extends AbstractOpenemsComponent implements TouEntsoe, OpenemsComponent, TimeOfUseTariff {

	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(TouEntsoeImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private HttpBridgeTimeService timeService;

	private Config config = null;
	private String securityToken = null;
	private TouManualHelper helper = TouManualHelper.EMPTY_TOU_MANUAL_HELPER;

	public TouEntsoeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TouEntsoe.ChannelId.values() //
		);
	}

	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChange = (a, b) -> {
		this.timeService.removeAllTimeEndpoints();
		this.timeService.subscribeTime(new EntsoeDelayTimeProvider(this.componentManager.getClock()), //
				this::createEntsoeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	};

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.applyConfig(config);
		this.httpBridge = this.httpBridgeFactory.get();

		// React on updates to Currency.
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChange);
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		this.timeService.subscribeTime(new EntsoeDelayTimeProvider(this.componentManager.getClock()), //
				this::createEntsoeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	/**
	 * Creates the ENTSO-E API endpoint for querying day-ahead prices.
	 *
	 * @return the configured {@link Endpoint}
	 */
	private Endpoint createEntsoeEndpoint() {
		final var fromDate = ZonedDateTime.now().truncatedTo(HOURS);
		final var toDate = fromDate.plusDays(1);
		final var biddingZone = this.config.biddingZone();
		return EntsoeApi.createEndPoint(biddingZone, this.securityToken, fromDate, toDate);
	}

	/**
	 * Handles successful response from ENTSO-E API.
	 *
	 * @param response the HTTP response
	 * @throws OpenemsNamedException        if processing fails
	 * @throws IOException                  on Error
	 * @throws SAXException                 on Error
	 * @throws ParserConfigurationException on Error
	 */
	private void handleEndpointResponse(HttpResponse<String> response)
			throws OpenemsNamedException, ParserConfigurationException, SAXException, IOException {
		setValue(this, TouEntsoe.ChannelId.HTTP_STATUS_CODE, response.status().code());
		setValue(this, TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, false);

		final var result = response.data();
		final var entsoeCurrency = parseCurrency(result);
		final var globalCurrency = this.meta.getCurrency();
		final double exchangeRate = getExchangeRateOrElse(entsoeCurrency, globalCurrency, 1.);
		final var gridFees = this.helper.getPrices();

		// Parse the response for the prices
		final var parsedPrices = parsePrices(result, this.config.resolution(), this.config.biddingZone());
		final var processedPrices = processPrices(this.componentManager.getClock(), parsedPrices, exchangeRate,
				gridFees);

		this.prices.set(processedPrices);
	}

	/**
	 * Handles errors from ENTSO-E API.
	 *
	 * @param error the HTTP error
	 */
	private void handleEndpointError(HttpError error) {
		final var httpStatusCode = switch (error) {
		case HttpError.ResponseError re -> re.status.code();
		default -> INTERNAL_ERROR;
		};

		setValue(this, TouEntsoe.ChannelId.HTTP_STATUS_CODE, httpStatusCode);
		setValue(this, TouEntsoe.ChannelId.UNABLE_TO_UPDATE_PRICES, true);

		this.logWarn(this.log, "Unable to Update Entsoe Time-Of-Use Price: " + error.getMessage());
	}

	/**
	 * Delay time provider for ENTSO-E API requests.
	 */
	public static class EntsoeDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public EntsoeDelayTimeProvider(Clock clock) {
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			// On error, retry after 1 minute with some randomness
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(10)) //
					.plusRandomDelay(30, SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			try {
				return Utils.calculateDelay(this.clock, result.data());

			} catch (ParserConfigurationException | SAXException | IOException e) {
				// Wait 30 minutes before retry
				return Delay.of(Duration.ofMinutes(30));
			}
		}
	}

	private void applyConfig(Config config) {
		this.securityToken = definedOrElse(config.securityToken(), this.oem.getEntsoeToken());
		if (this.securityToken == null) {
			this.logError(this.log, "Please configure Security Token to access ENTSO-E");
			return;
		}

		this.config = config;
		final var clock = this.componentManager.getClock();

		try {
			final var schedule = parseToSchedule(clock, config.biddingZone(), config.ancillaryCosts(),
					msg -> this.logWarn(this.log, msg));
			this.helper = new TouManualHelper(clock, schedule, 0.0);

		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Unable to parse Schedule: " + e.getMessage());
			this.helper = EMPTY_TOU_MANUAL_HELPER;
		}
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.componentManager.getClock()), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
