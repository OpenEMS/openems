package io.openems.edge.bridge.modbus.sunspec.generator;

import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.getAsOptionalJsonArray;
import static io.openems.common.utils.JsonUtils.getAsOptionalPrimitive;
import static io.openems.common.utils.JsonUtils.getAsOptionalString;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.getAsStringOrElse;

import java.util.Arrays;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPointCategory;

/**
 * POJO container for a SunSpec Point.
 */
public record Point(String id, int len, int offset, String type, Optional<String> scaleFactor, Unit unit,
		AccessMode accessMode, boolean mandatory, SunSpecPointCategory category, Point.Symbol[] symbols, String label,
		String description) {

	/**
	 * Builds a {@link Point} from a {@link JsonObject}.
	 * 
	 * @param point  the {@link JsonObject}
	 * @param offset the address offset
	 * @return a {@link Point}
	 * @throws OpenemsNamedException on error
	 */
	public static Point fromJson(JsonObject point, int offset) throws OpenemsNamedException {
		var id = getAsString(point, "name");
		var len = getAsInt(point, "size");
		var label = getAsStringOrElse(point, "label", "");
		var description = getAsStringOrElse(point, "desc", "");
		final String type;
		var t = getAsString(point, "type");
		if (t.equals("string")) {
			type = "STRING" + len;
		} else {
			type = t.toUpperCase();
		}
		var scaleFactor = getAsOptionalPrimitive(point, "sf").map(sf -> sf.getAsString());
		var unit = toUnit(getAsStringOrElse(point, "units", ""));
		var accessMode = switch (getAsStringOrElse(point, "access", "r").toLowerCase()) {
		case "wo" //
			-> AccessMode.WRITE_ONLY;
		case "rw" //
			-> AccessMode.READ_WRITE;
		default // "r", "ro"
			-> AccessMode.READ_ONLY;
		};
		var mandatory = getAsOptionalString(point, "mandatory").isPresent();
		var category = SunSpecPointCategory.MEASUREMENT;

		var symbolsJsonOpt = getAsOptionalJsonArray(point, "symbols");
		final Point.Symbol[] symbols;
		if (symbolsJsonOpt.isPresent()) {
			var symbolsJson = symbolsJsonOpt.get();
			symbols = new Point.Symbol[symbolsJson.size()];
			for (var i = 0; i < symbolsJson.size(); i++) {
				var symbol = symbolsJson.get(i);
				symbols[i] = Symbol.fromJson(symbol);
			}
		} else {
			symbols = new Point.Symbol[0];
		}
		return new Point(id, len, offset, type, scaleFactor, unit, accessMode, mandatory, category, symbols, label,
				description);
	}

	private static Unit toUnit(String unit) throws OpenemsNamedException {
		final ThrowingFunction<String, Unit, OpenemsNamedException> toUnit = s -> {
			s = s.trim();
			if (s.contains(" ")) {
				s = s.substring(0, s.indexOf(" "));
			}
			return switch (s) {
			case "", "%ARtg/%dV", "bps", "cos()", "deg", "Degrees", "hhmmss", "hhmmss.sssZ", "HPa", "kO", "Mbps",
					"meters", "mm", "mps", "m/s", "ohms", "Pct", "PF", "SF", "text", "Tmd", "Tmh", "Tms", "Various",
					"Vm", "W/m2", "YYYYMMDD", "S", "%Max/Sec" ->
				Unit.NONE;
			case "%", "%WHRtg" //
				-> Unit.PERCENT;
			case "A" //
				-> Unit.AMPERE;
			case "Ah", "AH" //
				-> Unit.AMPERE_HOURS;
			case "C" //
				-> Unit.DEGREE_CELSIUS;
			case "Hz" //
				-> Unit.HERTZ;
			case "kAH" //
				-> Unit.KILOAMPERE_HOURS;
			case "kWh" //
				-> Unit.KILOWATT_HOURS;
			case "mSecs" //
				-> Unit.MILLISECONDS;
			case "Secs" //
				-> Unit.SECONDS;
			case "V" //
				-> Unit.VOLT;
			case "VA" //
				-> Unit.VOLT_AMPERE;
			case "VAh" //
				-> Unit.VOLT_AMPERE_HOURS;
			case "var", "Var" //
				-> Unit.VOLT_AMPERE_REACTIVE;
			case "varh", "Varh" //
				-> Unit.VOLT_AMPERE_REACTIVE_HOURS;
			case "W" //
				-> Unit.WATT;
			case "Wh", "WH" // Validate manually: OpenEMS distinguishes CUMULATED and DISCRETE Watt-Hours.
				-> Unit.CUMULATED_WATT_HOURS;
			default //
				-> throw new OpenemsException("Unhandled unit [" + s + "]");
			};
		};
		return toUnit.apply(unit);
	}

	/**
	 * Gets the Symbol with the given Id.
	 *
	 * @param id the Symbol-Id
	 * @return the Symbol
	 * @throws OpenemsException on error
	 */
	public Point.Symbol getSymbol(String id) throws OpenemsException {
		for (var symbol : this.symbols) {
			if (symbol.id.equals(id)) {
				return symbol;
			}
		}
		throw new OpenemsException("Unable to find Symbol with ID " + id);
	}

	@Override
	public String toString() {
		return "Point [id=" + this.id + ", len=" + this.len + ", offset=" + this.offset + ", type=" + this.type
				+ ", scaleFactor=" + this.scaleFactor.orElse("") + ", unit=" + this.unit + ", access=" + this.accessMode
				+ ", mandatory=" + this.mandatory + ", category=" + this.category + ", symbols="
				+ Arrays.toString(this.symbols) + ", label=" + this.label + ", description=" + this.description + "]";
	}

	/**
	 * POJO container for a SunSpec Point Symbol.
	 */
	public static record Symbol(String id, int value, String label) {

		/**
		 * Builds a {@link Symbol} from a {@link JsonElement}.
		 * 
		 * @param symbol the {@link JsonElement}
		 * @return a {@link Symbol}
		 * @throws OpenemsNamedException on error
		 */
		public static Symbol fromJson(JsonElement symbol) throws OpenemsNamedException {
			var id = cleanId(getAsString(symbol, "name"));
			var value = getAsInt(symbol, "value");
			var label = getAsStringOrElse(symbol, "label", "");
			return new Symbol(id, value, label);
		}

		private static String cleanId(String id) {
			return switch (id) {
			// Special handling for ID 111 point "Operating State"
			case "ggOFF", "ggSLEEPING", "ggSTARTING", "ggTHROTTLED", "ggSHUTTING_DOWN", "ggFAULT", "ggSTANDBY" //
				-> id.substring(2);

			// Special handling for ID 202 point "Events"
			case "M_EVENT_Power_Failure", "M_EVENT_Under_Voltage", "M_EVENT_Low_PF", "M_EVENT_Over_Current",
					"M_EVENT_Over_Voltage", "M_EVENT_Missing_Sensor", "M_EVENT_Reserved1", "M_EVENT_Reserved2",
					"M_EVENT_Reserved3", "M_EVENT_Reserved4", "M_EVENT_Reserved5", "M_EVENT_Reserved6",
					"M_EVENT_Reserved7", "M_EVENT_Reserved8", "M_EVENT_OEM01", "M_EVENT_OEM02", "M_EVENT_OEM03",
					"M_EVENT_OEM04", "M_EVENT_OEM05", "M_EVENT_OEM06", "M_EVENT_OEM07", "M_EVENT_OEM08",
					"M_EVENT_OEM09", "M_EVENT_OEM10", "M_EVENT_OEM11", "M_EVENT_OEM12", "M_EVENT_OEM13",
					"M_EVENT_OEM14", "M_EVENT_OEM15" //
				-> id.substring(8);

			default //
				-> id;
			};
		}
	}
}