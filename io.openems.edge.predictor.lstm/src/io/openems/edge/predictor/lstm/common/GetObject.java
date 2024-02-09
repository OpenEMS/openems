package io.openems.edge.predictor.lstm.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;

import io.openems.common.OpenemsConstants;

public class GetObject {

	/**
	 * Deserializes an object from a file and prints its value.
	 *
	 * @return temp the modle
	 * @throws IOException            If an I/O error occurs during deserialization.
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found.
	 */

	public static Object get(String fileName) throws ClassNotFoundException, IOException {

		File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();

		String path = file.getAbsolutePath() + File.separator + "models" + File.separator + fileName + "FenHp.fems";
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {

			return ois.readObject();

		}
	}

	/**
	 * Updates a model based on selected indices and saves it to a file. This method
	 * updates a model by selecting specific data from a four-dimensional ArrayList
	 * and saving the updated model to a file. It takes a list of indices to specify
	 * which data should be included in the updated model and saves it using the
	 * provided file name.
	 *
	 * @param allModel The original four-dimensional ArrayList containing the model
	 *                 data.
	 * @param index    A list of indices specifying the data to be included in the
	 *                 updated model.
	 * @param fileName The name of the file to save the updated model.
	 */

	// public static void
	// updateModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel,
	// List<List<Integer>> index, String fileName) {
	// ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new
	// ArrayList<ArrayList<ArrayList<Double>>>();
	// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new
	// ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
	//
	// for (int i = 0; i < index.size(); i++) {
	//
	// ArrayList<ArrayList<Double>> temp1 =
	// allModel.get(index.get(i).get(0)).get(index.get(i).get(1));
	// optimumWeight.add(temp1);
	//
	// }
	// finalWeight.add(optimumWeight);
	// SaveModelBin.saveModels(finalWeight, fileName);
	//
	// }

}
