package io.openems.edge.predictor.lstm.common;

import static io.openems.edge.predictor.lstm.common.ReadAndSaveModels.MODEL_FOLDER;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//import org.junit.Test;

import io.openems.common.OpenemsConstants;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadAndSaveModels;

/**
 * This class contains test methods for saving and reading objects using Gson.
 * Uncomment the @Test annotations to run the tests locally. These tests use the
 * HyperParameters class and involve saving objects to JSON files and reading
 * them back for validation.
 */
public class ReadAndSaveObjectTest {

	/**
	 * Test method for saving an object to a file using Gson serialization.
	 * Uncomment the @Test annotation to run the test locally.
	 */
	// @Test
	public void saveObjectGsonTest() {
		HyperParameters hyperParameters = new HyperParameters();
		hyperParameters.setModelName("testGson");
		hyperParameters.setCount(30);
		hyperParameters.setRmsErrorTrend(0.1234);
		hyperParameters.setRmsErrorTrend(0.4567);
		ReadAndSaveModels.save(hyperParameters);
	}

	/**
	 * Test method for reading a JSON object from a file. Uncomment the @Test
	 * annotation to run the test locally.
	 */
	// @Test
	public void readObjectGson() {
		HyperParameters hyperParameters = new HyperParameters();
		hyperParameters.setCount(30);
		hyperParameters.setModelName("Consumption");

		HyperParameters hyper = ReadAndSaveModels.read(hyperParameters.getModelName());
		assertEquals(hyper.getCount(), hyperParameters.getCount());

		// deleting the hyperparametes
		try {
			Files.delete(Paths.get(getModelPath(hyperParameters.getModelName() + "fenHp.edge")));
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Gets the absolute path for a model file based on a given suffix. The path is
	 * constructed within the OpenEMS data directory under the "lstm" subdirectory.
	 *
	 * @param suffix The suffix to be appended to the model file path.
	 * @return The absolute path for the model file.
	 */
	public static String getModelPath(String suffix) {
		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
		return file.getAbsolutePath() + MODEL_FOLDER + suffix;
	}
}