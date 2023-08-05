package io.openems.edge.bridge.modbus.sunspec;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.base.CaseFormat;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingFunction;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointCategory;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint.PointType;

/**
 * This tool converts SunSpec XML definitions to Java code suitable for the
 * OpenEMS SunSpec implementation.
 *
 * <p>
 * Download XML files from https://github.com/sunspec/models.
 */
public class SunSpecCodeGenerator {

	/**
	 * Path to the SunSpec model XML files; download them from
	 * https://github.com/sunspec/models.
	 */
	private static final String SUNSPEC_JSON_PATH = System.getProperty("user.home") + "\\git\\models\\json\\";
	/**
	 * Path to the generated output file.
	 */
	private static final String OUT_FILE_PATH = System.getProperty("user.home") + "\\git\\models\\SunSpecModel.java";

	/**
	 * XML files that should be ignored; mainly because certain features are not
	 * implemented yet.
	 */
	private static final Set<String> IGNORE_FILES = new HashSet<>(Arrays.asList(//
			"model_3.json", //
			"model_4.json", //
			"model_5.json", //
			"model_6.json", //
			"model_7.json", //
			"model_8.json", //
			"model_9.json", //
			"model_10.json", //
			"model_11.json", //
			"model_12.json", //
			"model_13.json", //
			"model_14.json", //
			"model_16.json", //
			"model_17.json", //
			"model_19.json", //
			"model_126.json", //
			"model_129.json", //
			"model_130.json", //
			"model_131.json", //
			"model_132.json", //
			"model_133.json", //
			"model_134.json", //
			"model_135.json", //
			"model_136.json", //
			"model_137.json", //
			"model_138.json", //
			"model_139.json", //
			"model_140.json", //
			"model_141.json", //
			"model_142.json", //
			"model_143.json", //
			"model_144.json", //
			"model_160.json", //
			"model_211.json", //
			"model_212.json", //
			"model_213.json", //
			"model_214.json", //
			"model_220.json", //
			"model_401.json", //
			"model_402.json", //
			"model_403.json", //
			"model_404.json", //
			"model_501.json", //
			"model_502.json", //
			"model_601.json", //
			"model_803.json", //
			"model_804.json", //
			"model_805.json", //
			"model_806.json", //
			"model_807.json", //
			"model_808.json", //
			"model_809.json", //
			"model_63001.json", //
			"model_63002.json", //
			"model_64020.json" //
	));

	/**
	 * Run this method to start the code generator.
	 *
	 * @param args not supported
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		System.out.println(SUNSPEC_JSON_PATH);
		var generator = new SunSpecCodeGenerator();
		var models = generator.parseSunSpecFiles();
		generator.writeSunSpecModelJavaFile(models);
	}

	/**
	 * Parses all SunSpec XML files in a directory.
	 *
	 * @return a list of Models
	 * @throws Exception on error
	 */
	private List<Model> parseSunSpecFiles() throws Exception {
		List<Model> result = new ArrayList<>();
		for (File file : new File(SUNSPEC_JSON_PATH).listFiles(file -> //
		file.getName().startsWith("model") //
				&& file.getName().endsWith(".json") //
				&& !IGNORE_FILES.contains(file.getName()))) {
			try {
				var model = this.parseSunSpecFile(Files.readString(file.toPath()));
				result.add(model);

			} catch (Exception e) {
				throw new Exception("Error while reading from " + file, e);
			}
		}
		return result;
	}

	/**
	 * Parses a SunSpec XML file.
	 *
	 * @param file the SunSpec XML file handler
	 * @return the Model
	 * @throws Exception on error
	 */
	private Model parseSunSpecFile(String file) throws Exception {
		var json = new JSONObject(file);

		var generator = new SunSpecCodeGenerator();
		return generator.parseSunSpecModels(json);
	}

	/**
	 * Parses the element sunSpecModels.
	 *
	 * <pre>
	 *   &lt;sunSpecModels v="1"&gt;
	 * </pre>
	 *
	 * <ul>
	 * <li>xs:attribute name="v" type="xs:string" default="1"
	 * </ul>
	 *
	 * @param sunSpecModels the 'sunSpecModels' json
	 * @return the Model
	 * @throws OpenemsNamedException on error
	 * @throws JSONException on json error
	 */
	private Model parseSunSpecModels(JSONObject sunSpecModels) throws OpenemsNamedException, JSONException {
		return new Model(sunSpecModels);
	}

	/**
	 * Writes the SunSpecModel.java file.
	 *
	 * @param models a list of Models
	 * @throws IOException on error
	 */
	private void writeSunSpecModelJavaFile(List<Model> models) throws IOException {
		try (var w = Files.newBufferedWriter(Paths.get(OUT_FILE_PATH))) {
			w.write("package io.openems.edge.bridge.modbus.sunspec;");
			w.newLine();
			w.newLine();
			w.write("import io.openems.common.channel.AccessMode;");
			w.newLine();
			w.write("import io.openems.common.channel.Unit;");
			w.newLine();
			w.write("import io.openems.common.types.OptionsEnum;");
			w.newLine();
			w.newLine();
			w.write("/**");
			w.newLine();
			w.write(" * Do not touch this file. It is auto-generated by SunSpecCodeGenerator.");
			w.newLine();
			w.write(" */");
			w.newLine();
			w.write("public enum SunSpecModel {");
			w.newLine();

			/*
			 * Write main Model enum
			 */
			for (var i = 0; i < models.size(); i++) {
				var model = models.get(i);
				w.write("	S_" + model.id + "(//");
				w.newLine();
				w.write("			\"" + esc(model.label) + "\", //");
				w.newLine();
				w.write("			\"" + esc(model.description) + "\", //");
				w.newLine();
				w.write("			\"" + esc(model.notes) + "\", //");
				w.newLine();
				w.write("			" + model.len + ", //");
				w.newLine();
				w.write("			SunSpecModel.S" + model.id + ".values(), //");
				w.newLine();
				w.write("			SunSpecModelType." + model.modelType + " //");
				w.newLine();
				w.write("	)");
				if (i == models.size() - 1) {
					w.write("; //");
				} else {
					w.write(", //");
				}
				w.newLine();
			}
			w.newLine();

			/*
			 * For each Model write enum with SunSpecPoints
			 */
			for (Model model : models) {
				w.write("	public static enum S" + model.id + " implements SunSpecPoint {");
				w.newLine();
				for (var i = 0; i < model.points.size(); i++) {
					var point = model.points.get(i);
					var pointUpperId = toUpperUnderscore(point.id);
					w.write("		" + pointUpperId + "(new PointImpl(//");
					w.newLine();
					w.write("				\"S" + model.id + "_" + pointUpperId + "\", //");
					w.newLine();
					w.write("				\"" + esc(point.label) + "\", //");
					w.newLine();
					w.write("				\"" + esc(point.description) + "\", //");
					w.newLine();
					w.write("				\"" + esc(point.notes) + "\", //");
					w.newLine();
					w.write("				PointType." + point.type.name() + ", //");
					w.newLine();
					w.write("				" + point.mandatory + ", //");
					w.newLine();
					w.write("				AccessMode." + point.accessMode.name() + ", //");
					w.newLine();
					w.write("				Unit." + point.unit.name() + ", //");
					w.newLine();
					w.write("				"
							+ (point.scaleFactor.isPresent() ? "\"" + point.scaleFactor.get() + "\"" : null) + ", //");
					w.newLine();
					if (point.symbols.length == 0) {
						w.write("				new OptionsEnum[0]))");
					} else {
						w.write("				S" + model.id + "_" + point.id + ".values()))");
					}

					if (i == model.points.size() - 1) {
						w.write("; //");
					} else {
						w.write(", //");
					}
					w.newLine();
				}
				w.newLine();
				w.write("		protected final PointImpl impl;");
				w.newLine();
				w.newLine();
				w.write("		private S" + model.id + "(PointImpl impl) {");
				w.newLine();
				w.write("			this.impl = impl;");
				w.newLine();
				w.write("		}");
				w.newLine();
				w.newLine();
				w.write("		@Override");
				w.newLine();
				w.write("		public PointImpl get() {");
				w.newLine();
				w.write("			return this.impl;");
				w.newLine();
				w.write("		}");
				w.newLine();
				w.write("	}");
				w.newLine();
				w.newLine();

				/*
				 * For SunSpecPoints with Symbols write OpenEMS OptionsEnum
				 */
				for (Point point : model.points) {
					if (point.symbols.length == 0) {
						continue;
					}

					w.write("	public static enum S" + model.id + "_" + point.id + " implements OptionsEnum {");
					w.newLine();
					w.write("		UNDEFINED(-1, \"Undefined\"), //");
					w.newLine();
					for (var i = 0; i < point.symbols.length; i++) {
						var symbol = point.symbols[i];
						var symbolId = symbol.id;
						symbolId = toUpperUnderscore(symbolId);

						switch (symbolId) {
						case "RESERVED":
							symbolId = symbolId + "_" + symbol.value; // avoid duplicated "RESERVED" ids.
							break;
						}

						w.write("		" + symbolId + "(" + symbol.value + ", \"" + symbolId + "\")");
						if (i == point.symbols.length - 1) {
							w.write("; //");
						} else {
							w.write(", //");
						}
						w.newLine();
					}
					w.newLine();
					w.write("		private final int value;");
					w.newLine();
					w.write("		private final String name;");
					w.newLine();
					w.newLine();
					w.write("		private S" + model.id + "_" + point.id + "(int value, String name) {");
					w.newLine();
					w.write("			this.value = value;");
					w.newLine();
					w.write("			this.name = name;");
					w.newLine();
					w.write("		}");
					w.newLine();
					w.newLine();
					w.write("		@Override");
					w.newLine();
					w.write("		public int getValue() {");
					w.newLine();
					w.write("			return value;");
					w.newLine();
					w.write("		}");
					w.newLine();
					w.newLine();
					w.write("		@Override");
					w.newLine();
					w.write("		public String getName() {");
					w.newLine();
					w.write("			return name;");
					w.newLine();
					w.write("		}");
					w.newLine();
					w.newLine();
					w.write("		@Override");
					w.newLine();
					w.write("		public OptionsEnum getUndefined() {");
					w.newLine();
					w.write("			return UNDEFINED;");
					w.newLine();
					w.write("		}");
					w.newLine();
					w.write("	}");
					w.newLine();
					w.newLine();
				}
			}

			w.write("	public final String label;");
			w.newLine();
			w.write("	public final String description;");
			w.newLine();
			w.write("	public final String notes;");
			w.newLine();
			w.write("	public final int length;");
			w.newLine();
			w.write("	public final SunSpecPoint[] points;");
			w.newLine();
			w.write("	public final SunSpecModelType modelType;");
			w.newLine();
			w.newLine();
			w.write("	private SunSpecModel(String label, String description, String notes, int length, SunSpecPoint[] points,");
			w.newLine();
			w.write("			SunSpecModelType modelType) {");
			w.newLine();
			w.write("		this.label = label;");
			w.newLine();
			w.write("		this.description = description;");
			w.newLine();
			w.write("		this.notes = notes;");
			w.newLine();
			w.write("		this.length = length;");
			w.newLine();
			w.write("		this.points = points;");
			w.newLine();
			w.write("		this.modelType = modelType;");
			w.newLine();
			w.write("	}");
			w.newLine();
			w.write("}");
			w.newLine();
		}
	}

	/**
	 * Helper method to escape a string.
	 *
	 * @param string original string
	 * @return escaped string
	 */
	private static final String esc(String string) {
		if (string == null) {
			return "";
		}
		return string //
				.replaceAll("[^\\x00-\\x7F]", "") // non-ascii chars
				.replace("\"", "\\\"") // escape backslash
				.trim();
	}


	/**
	 * POJO container for a SunSpec Model.
	 */
	public static class Model {
		protected final int id;
		protected final int len;
		protected final String name;
		protected final List<Point> points;
		protected final SunSpecModelType modelType;

		protected String label = "";
		protected String description = "";
		protected String notes = "";


		public Model(JSONObject model) throws JSONException, OpenemsNamedException {
			this.id = model.getInt("id");
			var group = model.getJSONObject("group");
			this.name = group.getString("name");
			this.label = group.optString("label");
			this.description = group.optString("desc");
			var points = group.getJSONArray("points");

	        ArrayList<Point> list = new ArrayList<Point>();
	        var offset = 0;
			for (int i = 0; i < points.length(); i++) {
				var p = new Point(points.getJSONObject(i), offset);
				list.add(p);
				offset += p.len;
			}
			this.points = list;
			this.len = this.points.stream().map(p -> p.len).reduce(0, (t, p) -> t + p);
			this.modelType = SunSpecModelType.getModelType(this.id);
		}

		/**
		 * Gets the Point with the given Id.
		 *
		 * @param id the Point-ID
		 * @return the Point
		 * @throws OpenemsException on error
		 */
		public Point getPoint(String id) throws OpenemsException {
			for (Point point : this.points) {
				if (point.id.equals(id)) {
					return point;
				}
			}
			throw new OpenemsException("Unable to find Point with ID " + id);
		}

		@Override
		public String toString() {
			return "Model [id=" + this.id + ", name=" + this.name + ", points=" + this.points
					+ ", label=" + this.label + ", description=" + this.description + ", notes=" + this.notes + "]";
		}

	}

	/**
	 * POJO container for a SunSpec Point.
	 */
	public static class Point {

		protected final String id;
		protected final int len;
		protected final int offset;
		protected final PointType type;
		protected final Optional<String> scaleFactor;
		protected final Unit unit;
		protected final AccessMode accessMode;
		protected final boolean mandatory;
		protected final PointCategory category;
		protected final Symbol[] symbols;

		protected String label;
		protected String description;
		protected String notes;

		public Point(String id, int len, int offset, PointType type, String scaleFactor, Unit unit,
				AccessMode accessMode, boolean mandatory, PointCategory category, Symbol[] symbols) {
			this.id = id;
			this.len = len;
			this.offset = offset;
			this.type = type;
			this.scaleFactor = Optional.ofNullable(scaleFactor);
			this.unit = unit;
			this.accessMode = accessMode;
			this.mandatory = mandatory;
			this.category = category;
			this.symbols = symbols;
		}
		
		public Point(JSONObject point, int offset) throws JSONException, OpenemsNamedException {
			this.id = point.getString("name");
			this.len = point.getInt("size");
			this.label = point.optString("label");
			this.description = point.optString("desc");
			this.offset = offset;
			var t = point.getString("type");
			if (t.equals("string")) {
				this.type = PointType.valueOf("STRING" + this.len);
			} else {
				this.type = PointType.valueOf(t.toUpperCase());
			}
			var sf = point.optString("sf", "");
			this.scaleFactor = Optional.of(sf);
			this.unit = toUnit(point.optString("units", ""));
			var access = point.optString("access", "r");
			switch (access.toLowerCase()) {
			case "wo":
				this.accessMode = AccessMode.WRITE_ONLY;
				break;
			case "rw":
				this.accessMode = AccessMode.READ_WRITE;
				break;
			case "r":
			case "ro":
			default:
				this.accessMode = AccessMode.READ_ONLY;
				break;
			}
			this.mandatory = point.optString("mandatory", "N").equals("M");
			this.category = PointCategory.MEASUREMENT;
			
			var symbolsJson = point.optJSONArray("symbols");
			Symbol[] symbols;
			if (symbolsJson != null) {
				symbols = new Symbol[symbolsJson.length()];
				for (var i = 0; i < symbolsJson.length(); i++) {
					var symbol = symbolsJson.getJSONObject(i);
					symbols[i] = new Symbol(symbol);
				}
			} else {
				symbols = new Symbol[0];
			}
			this.symbols = symbols;

		}
		
		
		static Unit toUnit(String unit) throws OpenemsNamedException {
			final ThrowingFunction<String, Unit, OpenemsNamedException> toUnit = s -> {
				s = s.trim();
				if (s.contains(" ")) {
					s = s.substring(0, s.indexOf(" "));
				}
				switch (s) {
				case "":
				case "%ARtg/%dV":
				case "bps": // not available in OpenEMS
				case "cos()": // not available in OpenEMS
				case "deg": // not available in OpenEMS
				case "Degrees": // not available in OpenEMS
				case "hhmmss": // not available in OpenEMS
				case "hhmmss.sssZ": // not available in OpenEMS
				case "HPa": // not available in OpenEMS
				case "kO": // not available in OpenEMS
				case "Mbps": // not available in OpenEMS
				case "meters": // not available in OpenEMS
				case "mm": // not available in OpenEMS
				case "mps": // not available in OpenEMS
				case "m/s": // not available in OpenEMS
				case "ohms": // not available in OpenEMS
				case "Pct": // not available in OpenEMS
				case "PF": // not available in OpenEMS
				case "SF": // not available in OpenEMS
				case "text": // not available in OpenEMS
				case "Tmd": // not available in OpenEMS
				case "Tmh": // not available in OpenEMS
				case "Tms": // not available in OpenEMS
				case "Various": // not available in OpenEMS
				case "Vm": // not available in OpenEMS
				case "W/m2": // not available in OpenEMS
				case "YYYYMMDD": // not available in OpenEMS
				case "S": // not available in OpenEMS
				case "%Max/Sec": // not available in OpenEMS
					return Unit.NONE;
				case "%":
				case "%WHRtg":
					return Unit.PERCENT;
				case "A":
					return Unit.AMPERE;
				case "Ah":
				case "AH":
					return Unit.AMPERE_HOURS;
				case "C":
					return Unit.DEGREE_CELSIUS;
				case "Hz":
					return Unit.HERTZ;
				case "kAH":
					return Unit.KILOAMPERE_HOURS;
				case "kWh":
					return Unit.KILOWATT_HOURS;
				case "mSecs":
					return Unit.MILLISECONDS;
				case "Secs":
					return Unit.SECONDS;
				case "V":
					return Unit.VOLT;
				case "VA":
					return Unit.VOLT_AMPERE;
				case "VAh":
					return Unit.VOLT_AMPERE_HOURS;
				case "var":
				case "Var":
					return Unit.VOLT_AMPERE_REACTIVE;
				case "varh":
				case "Varh":
					return Unit.VOLT_AMPERE_REACTIVE_HOURS;
				case "W":
					return Unit.WATT;
				case "Wh":
				case "WH":
					// Validate manually: OpenEMS distinguishes CUMULATED and DISCRETE Watt-Hours.
					return Unit.CUMULATED_WATT_HOURS;
				}
				throw new OpenemsException("Unhandled unit [" + s + "]");
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
		public Symbol getSymbol(String id) throws OpenemsException {
			for (Symbol symbol : this.symbols) {
				if (symbol.id.equals(id)) {
					return symbol;
				}
			}
			throw new OpenemsException("Unable to find Symbol with ID " + id);
		}

		@Override
		public String toString() {
			return "Point [id=" + this.id + ", len=" + this.len + ", offset=" + this.offset + ", type=" + this.type
					+ ", scaleFactor=" + this.scaleFactor.orElse("") + ", unit=" + this.unit + ", access="
					+ this.accessMode + ", mandatory=" + this.mandatory + ", category=" + this.category + ", symbols="
					+ Arrays.toString(this.symbols) + ", label=" + this.label + ", description=" + this.description
					+ ", notes=" + this.notes + "]";
		}

		/**
		 * POJO container for a SunSpec Point Symbol.
		 */
		public static class Symbol {
			protected final String id;
			protected final int value;

			protected String label;
			protected String description;
			protected String notes;

			private static Function<String, String> idCleaner = id -> {
				switch (id) {
				case "ggOFF":
				case "ggSLEEPING":
				case "ggSTARTING":
				case "ggTHROTTLED":
				case "ggSHUTTING_DOWN":
				case "ggFAULT":
				case "ggSTANDBY":
					// Special handling for ID 111 point "Operating State"
					// TODO: create pull-request to fix XML file upstream
					return id.substring(2);
				case "M_EVENT_Power_Failure":
				case "M_EVENT_Under_Voltage":
				case "M_EVENT_Low_PF":
				case "M_EVENT_Over_Current":
				case "M_EVENT_Over_Voltage":
				case "M_EVENT_Missing_Sensor":
				case "M_EVENT_Reserved1":
				case "M_EVENT_Reserved2":
				case "M_EVENT_Reserved3":
				case "M_EVENT_Reserved4":
				case "M_EVENT_Reserved5":
				case "M_EVENT_Reserved6":
				case "M_EVENT_Reserved7":
				case "M_EVENT_Reserved8":
				case "M_EVENT_OEM01":
				case "M_EVENT_OEM02":
				case "M_EVENT_OEM03":
				case "M_EVENT_OEM04":
				case "M_EVENT_OEM05":
				case "M_EVENT_OEM06":
				case "M_EVENT_OEM07":
				case "M_EVENT_OEM08":
				case "M_EVENT_OEM09":
				case "M_EVENT_OEM10":
				case "M_EVENT_OEM11":
				case "M_EVENT_OEM12":
				case "M_EVENT_OEM13":
				case "M_EVENT_OEM14":
				case "M_EVENT_OEM15":
					// Special handling for ID 202 point "Events"
					return id.substring(8);
				default:
					return id;
				}
			};

			protected Symbol(String id, int value) {
				this.id = idCleaner.apply(id);
				this.value = value;
			}

			public Symbol(JSONObject symbol) throws JSONException {
				this.id = idCleaner.apply(symbol.getString("name"));
				this.value = symbol.getInt("value");
				this.label = symbol.optString("label");
			}
		}
	}

	protected static String toUpperUnderscore(String string) {
		string = string //
				.replace("-", "_") //
				.replace(" ", "_");
		if (!string.toUpperCase().equals(string)) {
			string = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, string);
		}
		return string.replace("__", "_");
	}

}
