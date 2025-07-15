package io.openems.edge.bridge.modbus.sunspec.generator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * This tool converts SunSpec Json definitions to Java code suitable for the
 * OpenEMS SunSpec implementation.
 *
 * <p>
 * Download Json files from https://github.com/sunspec/models.
 * </p>
 */
public class SunSpecCodeGenerator extends AbstractSunSpecCodeGenerator {

	private static final Logger log = LoggerFactory.getLogger(SunSpecCodeGenerator.class);

	@Override
	protected String getOutFilePath() {
		return System.getProperty("user.dir")
				+ "\\src\\io\\openems\\edge\\bridge\\modbus\\sunspec\\DefaultSunSpecModel.java";
	}

	@Override
	protected String getSunSpecJsonPath() {
		return System.getProperty("user.home") + "\\git\\sunspec\\json\\";
	}

	@Override
	protected Set<String> getIgnoreFiles() {
		return new HashSet<>(Arrays.asList(//
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
	}

	@Override
	protected Model parseSunSpecModels(JsonObject sunSpecModels) throws OpenemsNamedException {
		return Model.fromJson(sunSpecModels);
	}

	/**
	 * Run this method to start the code generator.
	 *
	 * @param args not supported
	 * @throws Exception on error
	 */
	public static void main(String[] args) throws Exception {
		var generator = new SunSpecCodeGenerator();
		log.info("Parsing SunSpec files from " + generator.getSunSpecJsonPath());
		var models = generator.parseSunSpecFiles();
		generator.writeSunSpecModelJavaFile(models);
	}
}