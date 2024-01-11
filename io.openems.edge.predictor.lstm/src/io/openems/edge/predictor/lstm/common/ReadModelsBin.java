package io.openems.edge.predictor.lstm.common;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class ReadModelsBin {

	/**
	 * Deserializes an object from a file and prints its value.
	 *
	 * @param filename The name of the file from which the object will be
	 *                 deserialized.
	 * @return temp the modle
	 * @throws IOException            If an I/O error occurs during deserialization.
	 * @throws ClassNotFoundException If the class of the serialized object cannot
	 *                                be found.
	 */

	
	public static Object getModel(String filename)
			throws ClassNotFoundException, IOException {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
			
			
			return  ois.readObject();

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

	public static void updateModel(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel,
			List<List<Integer>> index, String fileName) {
		ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new ArrayList<ArrayList<ArrayList<Double>>>();
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

		for (int i = 0; i < index.size(); i++) {

			ArrayList<ArrayList<Double>> temp1 = allModel.get(index.get(i).get(0)).get(index.get(i).get(1));
			optimumWeight.add(temp1);

		}
		finalWeight.add(optimumWeight);
		SaveModelBin.saveModels(finalWeight, fileName);		

	}

	
}
