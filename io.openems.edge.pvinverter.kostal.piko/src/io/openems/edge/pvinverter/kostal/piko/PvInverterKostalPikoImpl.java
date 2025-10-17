package io.openems.edge.pvinverter.kostal.piko;

import java.util.Base64;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "PV-Inverter.Kostal.Piko", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				"type=PRODUCTION" //
		} //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class PvInverterKostalPikoImpl extends AbstractOpenemsComponent
		implements PvInverterKostalPiko, ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(PvInverterKostalPikoImpl.class);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;

	private BridgeHttp httpBridge;
	private String baseUrl;
	private Map<String, String> headers;

	public PvInverterKostalPikoImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				PvInverterKostalPiko.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.baseUrl = config.url();
		this.httpBridge = this.httpBridgeFactory.get();

		// Setup Basic Authentication headers
		String auth = config.username() + ":" + config.password();
		String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
		this.headers = Map.of("Authorization", "Basic " + encodedAuth, "Accept", "text/html");

		// Set PV inverter limits
		this._setMaxApparentPower(null); // Not limited
		this._setActivePowerLimit(null); // Not limited

		if (this.isEnabled()) {
			this.logInfo(this.log, "Subscribing to KOSTAL PIKO at " + this.baseUrl);

			// Subscribe for updates every cycle
			this.httpBridge.subscribeCycle(1,
					() -> new BridgeHttp.Endpoint(this.baseUrl, HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT,
							BridgeHttp.DEFAULT_READ_TIMEOUT, null, this.headers),
					this::handleSuccessfulResult, this::handleError);
		}
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
	public void handleEvent(Event event) {
		// Not needed
	}

	private void handleSuccessfulResult(HttpResponse<String> result) {
		if (result == null || result.data() == null) {
			this.logError(this.log, "Received null response");
			return;
		}

		try {
			this.parseHtmlResponse(result.data());
			// Clear fault on successful communication
			this._setSlaveCommunicationFailed(false);
		} catch (Exception e) {
			this.logError(this.log, "Failed to parse HTML response: " + e.getMessage());
			this.channel(PvInverterKostalPiko.ChannelId.DEBUG_HTML)
					.setNextValue(result.data().substring(0, Math.min(result.data().length(), 1000)));
		}
	}

	private void handleError(HttpError error) {
		this.logError(this.log, "HTTP request failed: " + error.getMessage());
		this._setSlaveCommunicationFailed(true);
		this._setActivePower(null);
		this._setActivePowerL1(null);
		this._setActivePowerL2(null);
		this._setActivePowerL3(null);
		this._setVoltageL1(null);
		this._setVoltageL2(null);
		this._setVoltageL3(null);
		this._setCurrentL1(null);
		this._setCurrentL2(null);
		this._setCurrentL3(null);
	}

	private void parseHtmlResponse(String html) throws OpenemsException {

		Document doc = Jsoup.parse(html);

		// Store raw HTML for debugging
		this.channel(PvInverterKostalPiko.ChannelId.DEBUG_HTML)
				.setNextValue(html.substring(0, Math.min(html.length(), 1000)));

		// Parse based on structure: look for cells containing numeric data that are
		// followed by unit cells
		// This is more robust than relying on bgcolor="#FFFFFF" styling
		var allRows = doc.select("table tr");
		var valueCells = new Elements();

		for (Element row : allRows) {
			Elements cells = row.select("td");

			// Look for data pattern: [Label] [Value] [Unit]
			for (int i = 1; i < cells.size() - 1; i++) {
				Element valueCell = cells.get(i);
				Element prevCell = cells.get(i - 1);
				Element nextCell = cells.get(i + 1);

				String valueText = valueCell.text().trim();
				String prevText = prevCell.text().trim();
				String nextText = nextCell.text().trim();

				// Check if this matches the pattern: previous cell is a label (text),
				// current cell has numeric value or "x x x", next cell is a unit (text)
				boolean hasNumericOrNoData = valueText.matches(".*\\d+.*") || valueText.matches("x\\s*x\\s*x")
						|| valueText.equals("x");
				boolean prevIsLabel = prevText.matches(".*[a-zA-ZäöüÄÖÜß].*") && !prevText.matches("^[LMVWA]+\\d*$"); // Exclude pure unit labels
				boolean nextIsUnit = nextText.matches("\\s*[kMGmµ]?[WhVA]+\\s*"); // Match units like W, kWh, V, A, etc.

				if (hasNumericOrNoData && prevIsLabel && nextIsUnit) {
					valueCells.add(valueCell);
				}
			}
		}

		if (valueCells.isEmpty()) {
			throw new OpenemsException("No data cells found in HTML response");
		}

		var index = 0;

		// AC Power (total) - first value
		var acPower = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			acPower = value != null ? value : 0;
		}

		// Total Energy - second value
		Long totalYield = null;
		if (index < valueCells.size()) {
			totalYield = this.parseLongValue(valueCells.get(index++).text());
		}

		// Day Energy - third value
		Long dayYield = null;
		if (index < valueCells.size()) {
			dayYield = this.parseLongValue(valueCells.get(index++).text());
		}

		// String 1 Voltage - fourth value (Cell 3)
		var dcString1Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			dcString1Voltage = value != null ? value : 0;
		}

		// L1 Voltage - fifth value (Cell 4)
		var l1Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l1Voltage = value != null ? value : 0;
		}

		// String 1 Current - sixth value (Cell 5)
		var dcString1Current = 0;
		if (index < valueCells.size()) {
			var value = this.parseFloatAsMilliamps(valueCells.get(index++).text());
			dcString1Current = value != null ? value : 0;
		}

		// L1 Power - seventh value (Cell 6)
		var l1Power = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l1Power = value != null ? value : 0;
		}

		// String 2 Voltage - eighth value (Cell 7)
		var dcString2Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			dcString2Voltage = value != null ? value : 0;
		}

		// L2 Voltage - ninth value (Cell 8)
		var l2Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l2Voltage = value != null ? value : 0;
		}

		// String 2 Current - tenth value (Cell 9)
		var dcString2Current = 0;
		if (index < valueCells.size()) {
			var value = this.parseFloatAsMilliamps(valueCells.get(index++).text());
			dcString2Current = value != null ? value : 0;
		}

		// L2 Power - eleventh value (Cell 10)
		var l2Power = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l2Power = value != null ? value : 0;
		}

		// String 3 Voltage - twelfth value (Cell 11)
		var dcString3Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			dcString3Voltage = value != null ? value : 0;
		}

		// L3 Voltage - thirteenth value (Cell 12)
		var l3Voltage = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l3Voltage = value != null ? value : 0;
		}

		// String 3 Current - fourteenth value (Cell 13)
		var dcString3Current = 0;
		if (index < valueCells.size()) {
			var value = this.parseFloatAsMilliamps(valueCells.get(index++).text());
			dcString3Current = value != null ? value : 0;
		}

		// L3 Power - fifteenth value (Cell 14)
		var l3Power = 0;
		if (index < valueCells.size()) {
			var value = this.parseIntegerValue(valueCells.get(index++).text());
			l3Power = value != null ? value : 0;
		}

		// Parse Status
		String status = null;
		Elements statusElements = doc.select("td:contains(Status)");
		if (!statusElements.isEmpty()) {
			Element statusRow = statusElements.first().parent();
			Elements statusCells = statusRow.select("td");
			if (statusCells.size() >= 2) {
				status = statusCells.get(1).text().trim();
			}
		}

		// Calculate phase currents from power and voltage
		final Integer l1Current = this.calculateAcCurrent(l1Power, l1Voltage);
		final Integer l2Current = this.calculateAcCurrent(l2Power, l2Voltage);
		final Integer l3Current = this.calculateAcCurrent(l3Power, l3Voltage);

		// Calculate DC string powers
		final Integer dcString1Power = this.calculateDcPower(dcString1Voltage, dcString1Current);
		final Integer dcString2Power = this.calculateDcPower(dcString2Voltage, dcString2Current);
		final Integer dcString3Power = this.calculateDcPower(dcString3Voltage, dcString3Current);

		// Total AC Power
		this._setActivePower(acPower);

		// Energy yields
		this.channel(PvInverterKostalPiko.ChannelId.DAY_YIELD).setNextValue(dayYield);
		this._setActiveProductionEnergy(totalYield);

		// DC String 1
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING1_VOLTAGE).setNextValue(dcString1Voltage);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING1_CURRENT).setNextValue(dcString1Current);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING1_POWER).setNextValue(dcString1Power);

		// DC String 2
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING2_VOLTAGE).setNextValue(dcString2Voltage);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING2_CURRENT).setNextValue(dcString2Current);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING2_POWER).setNextValue(dcString2Power);

		// DC String 3
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING3_VOLTAGE).setNextValue(dcString3Voltage);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING3_CURRENT).setNextValue(dcString3Current);
		this.channel(PvInverterKostalPiko.ChannelId.DC_STRING3_POWER).setNextValue(dcString3Power);

		// AC Phase L1
		this._setVoltageL1(l1Voltage * 1000); // Convert V to mV
		this._setActivePowerL1(l1Power);
		this._setCurrentL1(l1Current);

		// AC Phase L2
		this._setVoltageL2(l2Voltage * 1000); // Convert V to mV
		this._setActivePowerL2(l2Power);
		this._setCurrentL2(l2Current);

		// AC Phase L3
		this._setVoltageL3(l3Voltage * 1000); // Convert V to mV
		this._setActivePowerL3(l3Power);
		this._setCurrentL3(l3Current);

		// Status
		if (status != null) {
			this.channel(PvInverterKostalPiko.ChannelId.STATUS).setNextValue(status);
		}
	}

	/**
	 * Calculate current from power and voltage.
	 *
	 * @param power   Power in Watts
	 * @param voltage Voltage in Volts
	 * @return Current in milliamperes
	 */
	private int calculateAcCurrent(Integer power, Integer voltage) {
		if (power == null || voltage == null || voltage == 0) {
			return 0;
		}
		// Current = Power / Voltage, convert to milliamperes
		return (int) ((power * 1000.0) / voltage);
	}

	/**
	 * Calculate power from voltage and current.
	 *
	 * @param voltage   Voltage in Volts
	 * @param currentMa Current in milliamperes
	 * @return Power in Watts
	 */
	private int calculateDcPower(Integer voltage, Integer currentMa) {
		if (voltage == null || currentMa == null) {
			return 0;
		}
		// Power = Voltage * Current (convert mA to A)
		return (int) (voltage * (currentMa / 1000.0));
	}

	/**
	 * Clean and prepare text for numeric parsing.
	 *
	 * @param text The raw text to clean
	 * @return Cleaned text ready for parsing, or null if text is invalid
	 */
	private String cleanTextForParsing(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}

		// Remove all whitespace characters (spaces, nbsp, tabs, etc.)
		String cleaned = text.replaceAll("\\s+", "");

		// Check for "xxx" or "x" pattern which indicates no data
		if (cleaned.contains("xxx") || cleaned.equals("x")) {
			return null;
		}

		// Remove any non-numeric characters except minus and decimal point
		cleaned = cleaned.replaceAll("[^0-9\\-\\.]", "");

		return cleaned.isEmpty() ? null : cleaned;
	}

	private Integer parseIntegerValue(String text) {
		String cleaned = this.cleanTextForParsing(text);
		if (cleaned == null) {
			return null;
		}

		try {
			// If it contains a decimal point, parse as float and convert to int
			if (cleaned.contains(".")) {
				return (int) Float.parseFloat(cleaned);
			}

			return Integer.parseInt(cleaned);
		} catch (NumberFormatException e) {
			this.logDebug(this.log, "Failed to parse integer value: " + text);
			return null;
		}
	}

	private Long parseLongValue(String text) {
		Integer intValue = this.parseIntegerValue(text);
		return intValue != null ? intValue.longValue() : null;
	}

	private Integer parseFloatAsMilliamps(String text) {
		String cleaned = this.cleanTextForParsing(text);
		if (cleaned == null) {
			return null;
		}

		try {
			// Parse as float and convert to milliamps
			float amps = Float.parseFloat(cleaned);
			return (int) (amps * 1000);
		} catch (NumberFormatException e) {
			this.logDebug(this.log, "Failed to parse float value: " + text);
			return null;
		}
	}

	@Override
	public String debugLog() {
		return "ActivePower:" + this.getActivePower().asString();
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}
}