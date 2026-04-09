package io.openems.edge.core.meta;

import static io.openems.common.utils.IntUtils.minInt;
import static io.openems.common.utils.StringUtils.emptyToNull;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.jsonapi.EdgeGuards.roleIsAtleast;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.OpenemsConstants;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jscalendar.JSCalendar.Tasks;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Role;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.jsonapi.JSCalendarApi;
import io.openems.edge.common.jsonapi.JSCalendarApi.UpdateJsCalendarRecord;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.core.meta.geocoding.GeocodeJsonRpcEndpoint;
import io.openems.edge.core.meta.geocoding.OpenCageGeocodingService;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Meta.SINGLETON_SERVICE_PID, //
		immediate = true, //
		property = { //
				"enabled=true" //
		})
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class MetaImpl extends AbstractOpenemsComponent
		implements Meta, OpenemsComponent, ModbusSlave, ComponentJsonApi, EventHandler {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	private Config config;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private OpenCageGeocodingService geocodingService;
	private JSCalendar.Tasks<GridBuySoftLimit> gridBuySoftLimit = JSCalendar.Tasks.empty();

	public MetaImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Meta.ChannelId.values() //
		);
		this.channel(Meta.ChannelId.VERSION).setNextValue(OpenemsConstants.VERSION.toString());
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		// Update the Channel _meta/SystemTimeUtc after every second
		final var systemTimeUtcChannel = this.<LongReadChannel>channel(Meta.ChannelId.SYSTEM_TIME_UTC);
		this.executor.scheduleAtFixedRate(() -> {
			systemTimeUtcChannel.setNextValue(Instant.now().getEpochSecond());
		}, 0, 1000, TimeUnit.MILLISECONDS);

		this.httpBridge = this.httpBridgeFactory.get();
		this.geocodingService = new OpenCageGeocodingService(this.httpBridge, this.oem.getOpenCageApiKey());

		this.applyConfig(config);
		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, SINGLETON_COMPONENT_ID, Meta.SINGLETON_SERVICE_PID, true);

		this.applyConfig(config);
		if (OpenemsComponent.validateSingleton(this.cm, Meta.SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		shutdownAndAwaitTermination(this.executor, 0);
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	private void applyConfig(Config config) {
		this.config = config;
		setValue(this, Meta.ChannelId.CURRENCY, Currency.fromCurrencyConfig(config.currency()));
		setValue(this, Meta.ChannelId.IS_ESS_CHARGE_FROM_GRID_ALLOWED, config.isEssChargeFromGridAllowed());
		setValue(this, Meta.ChannelId.GRID_FEED_IN_LIMITATION_TYPE,
				config.gridFeedInLimitationType().getGridFeedInLimitationType());

		{
			// Post-Process Grid-Buy-Soft-Limit
			final var gridBuyHardLimit = this.getGridBuyHardLimit();
			final var raw = JSCalendar.Tasks.fromStringOrEmpty(this.componentManager.getClock(),
					config.gridBuySoftLimit(), GridBuySoftLimit.serializer());
			final var result = JSCalendar.Tasks.<GridBuySoftLimit>create() //
					.setClock(raw.clock);
			// Make sure each value is <= getGridBuyHardLimit()
			raw.tasks.stream() //
					.map(t -> {
						if (t.payload().power() > gridBuyHardLimit) {
							return JSCalendar.Task.createFrom(t) //
									.setPayload(new GridBuySoftLimit(gridBuyHardLimit)) //
									.build();
						} else {
							return t;
						}
					}) //
					.forEach(result::add);
			// Add fallback of getGridBuyHardLimit() to make sure each timestamp has a value
			result.add(t -> t //
					.setPayload(new GridBuySoftLimit(gridBuyHardLimit))); //
			this.gridBuySoftLimit = result.build();
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return Meta.getModbusSlaveTable(accessMode, this.oem);
	}

	@Override
	public int getGridConnectionPointFuseLimit() {
		return this.config.gridConnectionPointFuseLimit();
	}

	/**
	 * Gets the {@link #getGridConnectionPointFuseLimit()} as three-phase in [W].
	 * 
	 * <p>
	 * NOTE: this currently uses static values for voltage (400 V) and cos(φ) (1)
	 * internally.
	 * 
	 * @return the value
	 */
	private int getGridConnectionPointFuseLimitInWatt() {
		final var voltage = 400.0; // [V]
		final var cosPhi = 1; // cos(φ)
		final var current = this.getGridConnectionPointFuseLimit(); // [A]
		return Double.valueOf(Math.sqrt(3) * voltage * current * cosPhi).intValue();
	}

	@Override
	public SubdivisionCode getSubdivisionCode() {
		return this.config.subdivisionCode();
	}

	@Override
	public String getPlaceName() {
		return emptyToNull(this.config.placeName());
	}

	@Override
	public String getPostcode() {
		return emptyToNull(this.config.postcode());
	}

	@Override
	public Coordinates getCoordinates() {
		return Coordinates.of(this.config.latitude(), this.config.longitude());
	}

	@Override
	public ZoneId getTimezone() {
		var timeZoneString = this.config.timezone();
		return (timeZoneString == null || timeZoneString.isBlank()) //
				? null //
				: ZoneId.of(timeZoneString);
	}

	@Override
	public int getGridSellHardLimit() {
		final var powerFromMaximumGridFeedInLimit = switch (this.config.gridFeedInLimitationType()) {
		case DYNAMIC_LIMITATION -> this.config.maximumGridFeedInLimit() >= 0 //
				? this.config.maximumGridFeedInLimit() //
				: null;
		case NO_LIMITATION -> null;
		};
		final var powerFromFuseLimit = this.getGridConnectionPointFuseLimitInWatt();
		return minInt(powerFromFuseLimit, powerFromMaximumGridFeedInLimit);
	}

	@Override
	public int getGridBuyHardLimit() {
		final var powerFromFuseLimit = this.getGridConnectionPointFuseLimitInWatt();
		return powerFromFuseLimit;
	}

	@Override
	public Tasks<GridBuySoftLimit> getGridBuySoftLimit() {
		return this.gridBuySoftLimit;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, GridBuySoftLimit.serializer(), //
				() -> this.gridBuySoftLimit, //
				() -> new UpdateJsCalendarRecord(this.cm, this.componentManager, this.servicePid(),
						"gridBuySoftLimit"));

		builder.handleRequest(new GeocodeJsonRpcEndpoint(), endpoint -> {
			endpoint.setGuards(roleIsAtleast(Role.OWNER));
		}, call -> {
			return new GeocodeJsonRpcEndpoint.Response(//
					this.geocodingService.geocode(call.getRequest().query()).get());
		});
	}

	@Override
	public ThirdPartyUsageAcceptance getThirdPartyUsageAcceptance() {
		return this.config.thirdPartyUsageAcceptance();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			final Integer gridBuySoftLimit = Optional.ofNullable(this.gridBuySoftLimit.getActiveOneTask())
					.map(JSCalendar.Tasks.OneTask::payload) //
					.map(GridBuySoftLimit::power) //
					.orElse(null);
			setValue(this, Meta.ChannelId.GRID_BUY_SOFT_LIMIT, gridBuySoftLimit);
		}
		}
	}
}
