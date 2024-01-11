import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.Model;
import io.openems.edge.predictor.lstm.common.ReadModels;

public class ReadandSaveModleBinTest {
	/**
	 * Saves an object to a file using binary serialization.
	 *
	 * @param obj  The object to be serialized and saved.
	 * @param path The path to the file where the object will be saved.
	 * @throws IOException If an I/O error occurs during serialization.
	 */
	public static void smb(Model obj, String path) {
		//Model mod = new Model();
		Model.save(obj, path);
	}

	/**
	 * Reads and deserializes an object from a file using binary deserialization.
	 * The deserialized object is printed to the console.
	 *
	 * @param path The path to the file from which the object will be read and
	 *             deserialized.
	 * @return val is a modle
	 * @throws IOException            If an I/O error occurs during deserialization.
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found.
	 */

	public static Object rmb(String path) throws ClassNotFoundException, IOException {

		//Model mod = new Model();
		return Model.read(path);

	}

	@Test
	public void test() throws ClassNotFoundException, IOException {
		String pathTrend = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\trend.txt";
		String pathSeasonality = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\SavedModel.txt";
		final String pathBin = "C:\\Users\\bishal.ghimire\\git\\Lstmforecasting\\io.openems.edge.predictor.lstm\\TestFolder\\fems.fe";
		HyperParameters hyperParameters = new HyperParameters();
		var modleTrend = ReadModels.getModelForSeasonality(pathTrend, hyperParameters);
		var modleSeasonality = ReadModels.getModelForSeasonality(pathSeasonality, hyperParameters);
		Model modle = new Model();
  //		modle.setSeasonalityModel(modleSeasonality);
  //		modle.setTrendModle(modleTrend);
  //		modle.setBatchCount(25);
  //		modle.setAccuracySeasonality(3.55);
  //		ReadandSaveModleBinTest.smb(modle, pathBin);
  //		System.out.println(ReadandSaveModleBinTest.rmb(pathBin));
		Model modle1 = (Model) ReadandSaveModleBinTest.rmb(pathBin);
		System.out.println(modle1.getSeasonalityModle().size());
		fail("Not yet implemented");
	}

}
