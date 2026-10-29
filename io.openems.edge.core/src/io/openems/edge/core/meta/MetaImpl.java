package io.openems.edge.core.meta;

import static io.openems.common.utils.StringUtils.emptyToNull;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.common.jsonapi.EdgeGuards.roleIsAtleast;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import org.slf4j.Logger;

import io.openems.common.OpenemsConstants;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.jscalendar.JSCalendar;
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
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.meta.ThirdPartyUsageAcceptance;
import io.openems.edge.common.meta.types.Coordinates;
import io.openems.edge.common.meta.types.Environment;
import io.openems.edge.common.meta.types.EnvironmentStatus;
import io.openems.edge.common.meta.types.GridLimit;
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

	private final Logger log = OpenemsComponent.getComponentLogger(this);

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

	private final List<OpenemsComponent> betaComponents = new CopyOnWriteArrayList<>();

	@Reference(policy = DYNAMIC, //
			policyOption = GREEDY, //
			cardinality = MULTIPLE, //
			target = "(&(enabled=true)(|" //
					+ "(objectClass=io.openems.edge.evse.api.chargepoint.EvseChargePoint)" //
					+ "(objectClass=io.openems.edge.heat.askoma.HeatAskoma)" //
					+ "(objectClass=io.openems.edge.heat.mypv.HeatMyPv)))")
	@SuppressWarnings("unused")
	void bindBetaComponent(OpenemsComponent component) {
		this.betaComponents.add(component);
		this.updateEnvironmentStatus();
	}

	@SuppressWarnings("unused")
	void unbindBetaComponent(OpenemsComponent component) {
		this.betaComponents.remove(component);
		this.updateEnvironmentStatus();
	}

	private final GridLimit.Mutable gridLimit = new GridLimit.Mutable();

	private BridgeHttp httpBridge;
	private OpenCageGeocodingService geocodingService;
	private JSCalendar.Tasks<GridLimit.Power> configuredGridBuySoftLimit = JSCalendar.Tasks.empty();

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
		setValue(this, Meta.ChannelId.IS_ESS_DISCHARGE_TO_GRID_ALLOWED, config.isEssDischargeToGridAllowed());
		setValue(this, Meta.ChannelId.GRID_FEED_IN_LIMITATION_TYPE,
				config.gridFeedInLimitationType().getGridFeedInLimitationType());

		this.configuredGridBuySoftLimit = JSCalendar.Tasks.fromStringOrEmpty(//
				this.componentManager.getClock(), //
				config.gridBuySoftLimit(), //
				GridLimit.Power.serializer());

		this.gridLimit //
				.setFuseCurrent(config.gridConnectionPointFuseLimit()) //
				.setGridBuySoftLimit(this.configuredGridBuySoftLimit) //
				.setGridSellLimit(//
						config.gridFeedInLimitationType().getGridFeedInLimitationType(), //
						// -1 for no fixed limit; 0 for zero-feed-in //
						config.maximumGridFeedInLimit() < 0 //
								? null //
								: config.maximumGridFeedInLimit()) //
				.setEssChargeFromGridAllowed(config.isEssChargeFromGridAllowed()) //
				.setEssDischargeToGridAllowed(config.isEssDischargeToGridAllowed());

		this.updateEnvironmentStatus();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return Meta.getModbusSlaveTable(accessMode, this.oem);
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
	public GridLimit.Immutable getGridLimit() {
		return this.gridLimit.toImmutable();
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		JSCalendarApi.buildJsonApiRoutes(builder, GridLimit.Power.serializer(), //
				() -> this.configuredGridBuySoftLimit, //
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
	public Environment getEnvironment() {
		return switch (this.resolveEnvironmentStatus()) {
		case STABLE -> Environment.STABLE;
		case BETA_MANUAL, BETA_ENFORCED -> Environment.BETA;
		};
	}

	private EnvironmentStatus resolveEnvironmentStatus() {
		if (!this.betaComponents.isEmpty()) {
			return EnvironmentStatus.BETA_ENFORCED;
		}
		return this.config.environment() == Environment.BETA //
				? EnvironmentStatus.BETA_MANUAL //
				: EnvironmentStatus.STABLE;
	}

	private void updateEnvironmentStatus() {
		if (this.config == null) {
			return;
		}

		final var environmentStatus = this.resolveEnvironmentStatus();
		try {
			final var configuration = this.cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?");
			final var properties = configuration.getProperties();
			if (properties != null && environmentStatus.name().equals(properties.get("environmentStatus"))) {
				return;
			}

			var newProperties = properties;
			if (newProperties == null) {
				newProperties = new Hashtable<>();
			}
			newProperties.put("environmentStatus", environmentStatus.name());
			configuration.update(newProperties);
		} catch (IOException e) {
			this.log.error("Failed to update the environment status in the Meta configuration.", e);
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			setValue(this, Meta.ChannelId.GRID_BUY_SOFT_LIMIT, this.gridLimit.activeGridBuySoftLimit());
		}
		}
	}
}
