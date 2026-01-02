package io.openems.edge.evcc.loadpoint;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleService;
import io.openems.edge.bridge.http.cycle.HttpBridgeCycleServiceDefinition;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

/**
 * Abstract base class for EVCC loadpoint meters.
 *
 * <p>
 * Provides common functionality for:
 * <ul>
 * <li>Loadpoint identification using title/index-based fallback strategy</li>
 * <li>HTTP lifecycle management</li>
 * <li>Per-phase energy calculation</li>
 * </ul>
 */
public abstract class AbstractLoadpointMeterEvcc extends AbstractOpenemsComponent
		implements TimedataProvider, EventHandler {

	// Note: @Reference annotations must be in concrete implementations, not abstract classes
	// Use abstract getters instead

	protected BridgeHttp httpBridge;
	protected HttpBridgeCycleService cycleService;

	// Loadpoint reference configuration
	private String configuredTitle;
	private int configuredIndex;
	private boolean fallbackWarningLogged = false;

	/**
	 * Energy calculators for both positive and negative power values.
	 *
	 * <p>
	 * EVCC loadpoint is a consumption meter that typically reports positive
	 * ActivePower values (consumption). According to ElectricityMeter API:
	 * <ul>
	 * <li>ACTIVE_PRODUCTION_ENERGY = integral over positive ACTIVE_POWER values
	 * <li>ACTIVE_CONSUMPTION_ENERGY = integral over negative ACTIVE_POWER values
	 * </ul>
	 * Both are provided for completeness, though for a wallbox/charging station,
	 * ACTIVE_PRODUCTION_ENERGY is the primary channel as it accumulates positive
	 * power (consumption). This ensures compatibility with UI history charts which
	 * expect consumption meters to use ActiveProductionEnergy.
	 */
	protected final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	protected final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	/**
	 * Energy calculators for each phase (L1, L2, L3).
	 */
	protected final CalculateEnergyFromPower calculateProductionEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1);
	protected final CalculateEnergyFromPower calculateProductionEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2);
	protected final CalculateEnergyFromPower calculateProductionEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3);

	protected AbstractLoadpointMeterEvcc(//
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds, //
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds //
	) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Activates the HTTP subscription for loadpoint data.
	 *
	 * @param context         the component context
	 * @param id              the component ID
	 * @param alias           the component alias
	 * @param enabled         whether the component is enabled
	 * @param apiUrl          the EVCC API URL
	 * @param loadpointTitle  the loadpoint title for matching
	 * @param loadpointIndex  the loadpoint index as fallback
	 * @param log             the logger instance
	 */
	protected void activateHttpSubscription(ComponentContext context, String id, String alias, boolean enabled,
			String apiUrl, String loadpointTitle, int loadpointIndex, Logger log) {
		super.activate(context, id, alias, enabled);
		this.initializeLoadpointReference(loadpointTitle, loadpointIndex);

		var factory = this.getHttpBridgeFactory();
		if (enabled && factory != null) {
			this.httpBridge = factory.get();
			this.cycleService = this.httpBridge.createService(this.getHttpBridgeCycleServiceDefinition());

			var jqFilter = this.buildLoadpointFilter(loadpointTitle, loadpointIndex);
			var url = UrlBuilder.parse(apiUrl) //
					.withQueryParam("jq", jqFilter) //
					.toEncodedString();
			this.logInfo(log, "Subscribing to loadpoint with filter: " + jqFilter);
			this.cycleService.subscribeJsonEveryCycle(url, this::processHttpResult);
		}
	}

	/**
	 * Deactivates the HTTP subscription.
	 */
	protected void deactivateHttpSubscription() {
		if (this.httpBridge != null) {
			var factory = this.getHttpBridgeFactory();
			if (factory != null) {
				factory.unget(this.httpBridge);
			}
			this.httpBridge = null;
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			this.calculateEnergyPerPhase();
			break;
		}
	}

	/**
	 * Calculate energy from power values, splitting positive and negative values.
	 *
	 * <p>
	 * Positive power values (consumption) are accumulated in ACTIVE_PRODUCTION_ENERGY,
	 * negative power values (production) are accumulated in ACTIVE_CONSUMPTION_ENERGY.
	 * For a wallbox/charging station, positive values are expected (consumption only),
	 * so ACTIVE_PRODUCTION_ENERGY is the primary channel used by UI history charts.
	 */
	protected void calculateEnergy() {
		final var activePower = this.getActivePowerValue();
		if (activePower == null) {
			this.calculateProductionEnergy.update(null);
			this.calculateConsumptionEnergy.update(null);
		} else if (activePower > 0) {
			// Positive power = consumption -> accumulate in ACTIVE_PRODUCTION_ENERGY
			this.calculateProductionEnergy.update(activePower);
			this.calculateConsumptionEnergy.update(0);
		} else {
			// Negative power = production -> accumulate in ACTIVE_CONSUMPTION_ENERGY
			this.calculateProductionEnergy.update(0);
			this.calculateConsumptionEnergy.update(Math.abs(activePower));
		}
	}

	/**
	 * Processes the HTTP result from EVCC API.
	 * Subclasses must implement phase-specific handling.
	 *
	 * @param result the HTTP response
	 * @param error  the HTTP error, if any
	 */
	protected abstract void processHttpResult(HttpResponse<JsonElement> result, HttpError error);

	/**
	 * Returns the logger for this component.
	 *
	 * @return the logger
	 */
	protected abstract Logger getLogger();

	/**
	 * Returns the active power channel value.
	 *
	 * @return the active power in W, or null
	 */
	protected abstract Integer getActivePowerValue();

	/**
	 * Returns the active power value for phase L1.
	 *
	 * @return the active power L1 in W, or null
	 */
	protected abstract Integer getActivePowerL1Value();

	/**
	 * Returns the active power value for phase L2.
	 *
	 * @return the active power L2 in W, or null
	 */
	protected abstract Integer getActivePowerL2Value();

	/**
	 * Returns the active power value for phase L3.
	 *
	 * @return the active power L3 in W, or null
	 */
	protected abstract Integer getActivePowerL3Value();

	/**
	 * Returns the BridgeHttpFactory.
	 * Note: @Reference annotation must be in concrete implementations.
	 *
	 * @return the BridgeHttpFactory
	 */
	protected abstract BridgeHttpFactory getHttpBridgeFactory();

	/**
	 * Returns the HttpBridgeCycleServiceDefinition.
	 * Note: @Reference annotation must be in concrete implementations.
	 *
	 * @return the HttpBridgeCycleServiceDefinition
	 */
	protected abstract HttpBridgeCycleServiceDefinition getHttpBridgeCycleServiceDefinition();

	/**
	 * Initializes the loadpoint reference configuration.
	 *
	 * @param title the configured loadpoint title (can be empty)
	 * @param index the configured loadpoint index
	 */
	protected void initializeLoadpointReference(String title, int index) {
		this.configuredTitle = title;
		this.configuredIndex = index;
	}

	/**
	 * Builds a JQ filter to select the loadpoint.
	 *
	 * @param title the configured loadpoint title
	 * @param index the configured loadpoint index
	 * @return JQ filter expression
	 */
	protected String buildLoadpointFilter(String title, int index) {
		if (title != null && !title.trim().isEmpty()) {
			var escapedTitle = title.trim().replace("\\", "\\\\").replace("\"", "\\\"");
			return "(.loadpoints[] | select(.title == \"" + escapedTitle + "\")) // .loadpoints[" + index + "]";
		}
		return ".loadpoints[" + index + "]";
	}

	/**
	 * Checks if the received loadpoint matches the configured title.
	 *
	 * @param lp     the loadpoint JSON object
	 * @param logger the logger to use for warnings
	 */
	protected void checkLoadpointMatch(JsonObject lp, Logger logger) {
		if (this.configuredTitle == null || this.configuredTitle.trim().isEmpty()) {
			return;
		}
		if (!lp.has("title")) {
			return;
		}
		var actualTitle = lp.get("title").getAsString();
		if (!this.configuredTitle.trim().equals(actualTitle) && !this.fallbackWarningLogged) {
			logger.warn(
					"Loadpoint title mismatch! Configured title='{}' + index=[{}], but received title='{}'. "
							+ "Using fallback to index.",
					this.configuredTitle, this.configuredIndex, actualTitle);
			this.fallbackWarningLogged = true;
		}
	}


	/**
	 * Calculate production energy per phase from phase-specific power values.
	 */
	protected void calculateEnergyPerPhase() {
		// L1
		this.calculateProductionEnergyL1.update(this.getActivePowerL1Value());

		// L2
		this.calculateProductionEnergyL2.update(this.getActivePowerL2Value());

		// L3
		this.calculateProductionEnergyL3.update(this.getActivePowerL3Value());
	}

	@Override
	public abstract Timedata getTimedata();

	@Override
	public String debugLog() {
		return "L:" + this.getActivePowerValue();
	}
}
