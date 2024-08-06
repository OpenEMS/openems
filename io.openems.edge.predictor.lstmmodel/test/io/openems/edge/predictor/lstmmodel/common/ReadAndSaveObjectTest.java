
package io.openems.edge.predictor.lstmmodel.common;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//import org.junit.Test;

import io.openems.common.OpenemsConstants;

public class ReadAndSaveObjectTest {

	/**
	 * Save object and testing.
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
	 * REading json object test.
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
			Files.delete(Paths.get(this.getModelPath(hyperParameters.getModelName() + "fenHp.fems")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Gets the absolute path for a model file based on a given suffix. The path is
	 * constructed within the OpenEMS data directory under the "models"
	 * subdirectory.
	 *
	 * @param suffix The suffix to be appended to the model file path.
	 * @return The absolute path for the model file.
	 */

	public String getModelPath(String suffix) {
		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
		return file.getAbsolutePath() + File.separator + "models" + File.separator + suffix;
	}

}
