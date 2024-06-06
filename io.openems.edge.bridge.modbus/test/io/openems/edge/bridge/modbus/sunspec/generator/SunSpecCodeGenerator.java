package io.openems.edge.bridge.modbus.sunspec.generator;

import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.bridge.modbus.sunspec.Utils.toLabel;
import static io.openems.edge.bridge.modbus.sunspec.Utils.toUpperUnderscore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.StateChannel;

/**
 * This tool converts SunSpec Json definitions to Java code suitable for the
 * OpenEMS SunSpec implementation.
 *
 * <p>
 * Download Json files from https://github.com/sunspec/models.
 */
public class SunSpecCodeGenerator {

	/**
	 * Path to the SunSpec model Json files; download them from
	 * https://github.com/sunspec/models.
	 */
	private static final String SUNSPEC_JSON_PATH = System.getProperty("user.home") + "\\git\\sunspec\\json\\";
	/**
	 * Path to the generated output file.
	 */
	private static final String OUT_FILE_PATH = System.getProperty("user.dir")
			+ "\\src\\io\\openems\\edge\\bridge\\modbus\\sunspec\\DefaultSunSpecModel.java";

	/**
	 * Json files that should be ignored; mainly because certain features are not
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
			"model_302.json", //
			"model_303.json", //
			"model_304.json", //
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
		System.out.println("Parsing SunSpec files from " + SUNSPEC_JSON_PATH);
		var generator = new SunSpecCodeGenerator();
		var models = generator.parseSunSpecFiles();
		generator.writeSunSpecModelJavaFile(models);
	}

	/**
	 * Parses all SunSpec Json files in a directory.
	 *
	 * @return a list of Models
	 * @throws Exception on error
	 */
	private List<Model> parseSunSpecFiles() throws Exception {
		List<Model> result = new ArrayList<>();
		for (var file : new File(SUNSPEC_JSON_PATH).listFiles(file -> //
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
		// Sort by model ids to have an ordered output
		result.sort(new Comparator<Model>() {
			@Override
			public int compare(Model m1, Model m2) {
				return m1.id() - m2.id();
			}
		});

		return result;
	}

	/**
	 * Parses a SunSpec Json file.
	 *
	 * @param file the SunSpec Json file handler
	 * @return the Model
	 * @throws Exception on error
	 */
	private Model parseSunSpecFile(String file) throws Exception {
		var json = parseToJsonObject(file);

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
	 * @throws JSONException         on json error
	 */
	private Model parseSunSpecModels(JsonObject sunSpecModels) throws OpenemsNamedException {
		return Model.fromJson(sunSpecModels);
	}

	private static enum PointType {
		VALUE("ValuePoint", "ValuePoint.Type"), //
		SCALED_VALUE("ScaledValuePoint", "ValuePoint.Type"), //
		SCALE_FACTOR("ScaleFactorPoint", "ScaleFactorPoint.Type"), //
		ENUM("EnumPoint", "EnumPoint.Type"), //
		BITFIELD("BitFieldPoint", "BitFieldPoint.Type");

		public final String clazz;
		public final String typeClazz;

		private PointType(String clazz, String type) {
			this.clazz = clazz;
			this.typeClazz = type;
		}

		public static PointType evaluate(Point point) {
			if (point.scaleFactor().isPresent()) {
				return PointType.SCALED_VALUE;
			}
			switch (point.type()) {
			case "ENUM16", "ENUM32":
				return PointType.ENUM;
			case "SUNSSF":
				return PointType.SCALE_FACTOR;
			case "BITFIELD16", "BITFIELD32":
				return PointType.BITFIELD;
			}
			return PointType.VALUE;
		}
	}

	/**
	 * Writes the SunSpecModel.java file.
	 *
	 * @param models a list of Models
	 * @throws IOException on error
	 */
	private void writeSunSpecModelJavaFile(List<Model> models) throws IOException {
		try (var w = FluentWriter.to(OUT_FILE_PATH)) {
			w //
					.writeln("// CHECKSTYLE:OFF") //
					.blank() //
					.writeln("package io.openems.edge.bridge.modbus.sunspec;") //
					.blank() //
					.writeln("import io.openems.common.channel.AccessMode;") //
					.writeln("import io.openems.common.channel.Level;") //
					.writeln("import io.openems.common.channel.Unit;") //
					.writeln("import io.openems.common.types.OptionsEnum;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.BitFieldPoint.SunSpecBitPoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.BitPoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.EnumPoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.ScaleFactorPoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.ScaledValuePoint;") //
					.writeln("import io.openems.edge.bridge.modbus.sunspec.Point.ValuePoint;") //
					.blank() //
					.writeln("/**") //
					.writeln(" * Do not touch this file. It is auto-generated by SunSpecCodeGenerator.") //
					.writeln(" */") //
					.writeln("public enum DefaultSunSpecModel implements SunSpecModel {");

			/*
			 * Write main Model enum
			 */
			for (var i = 0; i < models.size(); i++) {
				var model = models.get(i);
				w //
						.writeln("	S_" + model.id() + "(" //
								+ "\"" + esc(model.label()) + "\", //") //
						.writeln("			\"" + esc(model.description()) + "\", //") //
						.write("			" + model.len() + " /* length */, " //
								+ "DefaultSunSpecModel.S" + model.id() + ".values(), " //
								+ "SunSpecModelType." + model.modelType()) //
						.writelnIf(i == models.size() - 1, //
								"); //", //
								"), //");
			}
			w.blank();

			/*
			 * For each Model write enum with SunSpecPoints
			 */
			for (var model : models) {
				w.writeln("	public static enum S" + model.id() + " implements SunSpecPoint {");
				for (var i = 0; i < model.points().size(); i++) {
					final var point = model.points().get(i);
					final var pointUpperId = toUpperUnderscore(point.id());
					final var pointType = PointType.evaluate(point);
					w //
							.write("		" + pointUpperId + "(new " + pointType.clazz + "(" //
									+ "\"S" + model.id() + "_" + pointUpperId + "\", " //
									+ "\"" + esc(point.label()) + "\", ");
					if (!point.description().isEmpty()) {
						w.writeln("//").write("				");
					}
					w.write("\"" + esc(point.description()) + "\""); //

					if (pointType != PointType.SCALE_FACTOR) {
						w //
								.writeln(", //") //
								.write("				" + pointType.typeClazz + "." + point.type() + ", " //
										+ point.mandatory() + " /* mandatory? */" //
										+ ", AccessMode." + point.accessMode().name());
						if (pointType == PointType.VALUE || pointType == PointType.SCALED_VALUE) {
							w //
									.write(", Unit." + point.unit().name());
						}
						if (pointType == PointType.SCALED_VALUE) {
							w //
									.write(", \"" + point.scaleFactor().get() + "\"");
						} else if (pointType == PointType.ENUM) {
							w //
									.write(", ") //
									.writeIf(point.symbols().length == 0, //
											"new OptionsEnum[0]", //
											"S" + model.id() + "_" + point.id() + ".values()");
						} else if (pointType == PointType.BITFIELD) {
							w //
									.write(", ") //
									.writeIf(point.symbols().length == 0, //
											"new SunSpecBitPoint[0]", //
											"S" + model.id() + "_" + point.id() + ".values()");
						}
					}

					w.writelnIf(i == model.points().size() - 1, //
							"));", //
							")), //");
				}
				w //
						.blank() //
						.writeln("		private final Point point;") //
						.blank() //
						.writeln("		private S" + model.id() + "(Point point) {") //
						.writeln("			this.point = point;") //
						.writeln("		}") //
						.blank() //
						.writeln("		@Override") //
						.writeln("		public Point get() {") //
						.writeln("			return this.point;") //
						.writeln("		}") //
						.writeln("	}") //
						.blank();

				// Handle SunSpecPoints with Symbols
				for (var point : model.points()) {
					if (point.symbols().length == 0) {
						continue;
					}
					final var pointType = PointType.evaluate(point);
					/*
					 * Handle Enum points
					 */
					if (pointType == PointType.ENUM) {
						w //
								.writeln("	public static enum S" + model.id() + "_" + point.id()
										+ " implements OptionsEnum {") //
								.writeln("		UNDEFINED(-1, \"Undefined\"), //");
						for (var i = 0; i < point.symbols().length; i++) {
							final var symbol = point.symbols()[i];
							var symbolId = toUpperUnderscore(symbol.id());

							switch (symbolId) {
							case "RESERVED":
								symbolId = symbolId + "_" + symbol.value(); // avoid duplicated "RESERVED" ids.
								break;
							}

							w //
									.write("		" + symbolId + "(" + symbol.value() + ", \"" + symbolId + "\")") //
									.writelnIf(i == point.symbols().length - 1, //
											";", //
											", //");
						}
						w.blank() //
								.writeln("		private final int value;") //
								.writeln("		private final String name;") //
								.blank() //
								.writeln("		private S" + model.id() + "_" + point.id()
										+ "(int value, String name) {") //
								.writeln("			this.value = value;") //
								.writeln("			this.name = name;") //
								.writeln("		}") //
								.blank() //
								.writeln("		@Override") //
								.writeln("		public int getValue() {") //
								.writeln("			return this.value;") //
								.writeln("		}") //
								.blank() //
								.writeln("		@Override") //
								.writeln("		public String getName() {") //
								.writeln("			return this.name;") //
								.writeln("		}") //
								.blank() //
								.writeln("		@Override") //
								.writeln("		public OptionsEnum getUndefined() {") //
								.writeln("			return UNDEFINED;") //
								.writeln("		}") //
								.writeln("	}") //
								.blank();

					} else if (pointType == PointType.BITFIELD) {
						/*
						 * Handle BitField points
						 */
						final var name = "S" + model.id() + "_" + point.id();
						w.writeln("	public static enum " + name + " implements SunSpecBitPoint {");
						for (var i = 0; i < point.symbols().length; i++) {
							final var symbol = point.symbols()[i];
							var symbolId = toUpperUnderscore(symbol.id());
							var channelId = toUpperUnderscore(name) + "_" + symbolId;
							var label = toLabel(symbol.id());
							var level = getBitLevel(channelId);
							w //
									.write("		" + symbolId + "(new BitPoint(" + symbol.value() + ", \""
											+ channelId + "\", \"" + label + "\"") //
									.writeIf(level != null, () -> ", Level." + level.name()) //
									.write("))") //
									.writelnIf(i == point.symbols().length - 1, //
											";", //
											", //");
						}
						w //
								.blank() //
								.writeln("		private final BitPoint point;") //
								.blank() //
								.writeln("		private " + name + "(BitPoint point) {") //
								.writeln("			this.point = point;") //
								.writeln("		}") //
								.blank() //
								.writeln("		@Override") //
								.writeln("		public BitPoint get() {") //
								.writeln("			return this.point;") //
								.writeln("		}") //
								.writeln("	}") //
								.blank();

					}
				}
			}

			w //
					.writeln("	public final String label;") //
					.writeln("	public final String description;") //
					.writeln("	public final int length;") //
					.writeln("	public final SunSpecPoint[] points;") //
					.writeln("	public final SunSpecModelType modelType;") //
					.blank() //
					.writeln(
							"	private DefaultSunSpecModel(String label, String description, int length, SunSpecPoint[] points,") //
					.writeln("			SunSpecModelType modelType) {") //
					.writeln("		this.label = label;") //
					.writeln("		this.description = description;") //
					.writeln("		this.length = length;") //
					.writeln("		this.points = points;") //
					.writeln("		this.modelType = modelType;") //
					.writeln("	}") //
					.blank() //
					.writeln("	@Override") //
					.writeln("	public SunSpecPoint[] points() {") //
					.writeln("		return this.points;") //
					.writeln("	}") //
					.blank() //
					.writeln("	@Override") //
					.writeln("	public String label() {") //
					.writeln("		return this.label;") //
					.writeln("	}") //
					.writeln("}") //
					.writeln("// CHECKSTYLE:ON");
		}

	}

	/**
	 * Gets the {@link StateChannel}-Level per Channel-ID.
	 * 
	 * @param channelId the upper-case Channel-ID, e.g. "S2_EVT_GROUND_FAULT"
	 * @return {@link Level}, defaults to {@link Level#OK}; or null to create a
	 *         {@link BooleanReadChannel}
	 */
	private static Level getBitLevel(String channelId) {
		// RESERVED
		if (channelId.contains("RESERVED")) {
			return null;
		}

		// Level.WARNING Points
		for (var prefix : new String[] { "S2_EVT_", "S101_EVT1_", "S102_EVT1_", "S103_EVT1_", "S111_EVT1_",
				"S112_EVT1_", "S113_EVT1_" }) {
			if (channelId.startsWith(prefix)) {
				// TODO: those should be Level.WARNING
				return Level.INFO;
			}

		}

		return switch (channelId) {
		// This is the position for Channel-ID specific Levels
		default -> null;
		};
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

}
