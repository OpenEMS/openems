package io.openems.edge.predictor.lstm.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ReadModels {

	private static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> allModel = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();

	/**
	 * Reads a data file and parses its content into a nested ArrayList structure.
	 * This method reads the specified data file, where the data is organized into
	 * nested lists using empty lines as separators. Each non-empty line consists of
	 * space-separated values that are parsed into double-precision floating-point
	 * numbers. The data is structured as a list of lists, where each inner list
	 * represents a row of data, and each outer list groups multiple rows. The
	 * method returns the parsed data in the form of a nested ArrayList structure.
	 *
	 * @param filename The name of the file to read the data from.
	 * @return A nested ArrayList structure containing the parsed data.
	 * @throws FileNotFoundException if the specified file is not found or cannot be
	 *                               opened.
	 */
	public static ArrayList<ArrayList<ArrayList<Double>>> readDataFile(String filename) {
		ArrayList<ArrayList<ArrayList<Double>>> dataList = new ArrayList<>();
		
		

		try {
			Scanner scanner = new Scanner(new File(filename));
			ArrayList<ArrayList<Double>> outerList = new ArrayList<>();
			ArrayList<Double> innerList = new ArrayList<>();

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();

				if (line.isEmpty()) {
					if (!innerList.isEmpty()) {
						dataList.add(outerList);
						innerList = new ArrayList<>();
						outerList = new ArrayList<>();
					}

					if (!outerList.isEmpty()) {
						dataList.add(outerList);
						outerList = new ArrayList<>();
					}
				} else {
					String[] values = line.split(" ");
					for (String value : values) {
						innerList.add(Double.parseDouble(value));
					}
					outerList.add(innerList);
					innerList = new ArrayList<Double>();
				}
			}

			scanner.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return dataList;
	}

	/**
	 * Reshapes a three-dimensional ArrayList into a four-dimensional ArrayList
	 * structure. This method takes a three-dimensional ArrayList of data and
	 * reshapes it into a four-dimensional ArrayList structure. The reshaping is
	 * performed by dividing the original data into blocks of size 4x24. The
	 * resulting four-dimensional ArrayList contains these blocks.
	 *
	 * @param dataList        The three-dimensional ArrayList to be reshaped.
	 * @param hyperParameters is the object of class HyperPrameters.
	 * @return A four-dimensional ArrayList structure containing the reshaped data.
	 */

	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> reshape(
			ArrayList<ArrayList<ArrayList<Double>>> dataList, HyperParameters hyperParameters) {

		int m = 60 / hyperParameters.getInterval() * 24;
		int n = dataList.size() / m;
		int o = 0;
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> temp2 = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		for (int i = 0; i < n; i++) {
			ArrayList<ArrayList<ArrayList<Double>>> temp1 = new ArrayList<ArrayList<ArrayList<Double>>>();

			for (int j = 0; j < m; j++) {
				temp1.add(dataList.get(o));
				o = o + 1;

			}
			temp2.add(temp1);

		}
		return temp2;
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
		SaveModel.saveModels(finalWeight, fileName);

		// SaveModel.saveModels(finalWeight, "BestModels.txt");

	}

	/**
	 * Updates a model based on the provided index and saves it. This method updates
	 * a model by selecting a specific set of data from a four-dimensional ArrayList
	 * using the given index and saves it. The updated model is saved using the
	 * "SaveModel" utility class without specifying a file name.
	 *
	 * @param index The index specifying the data to be included in the updated
	 *              model.
	 */

	public void updateModel(Integer index) {
		ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> finalWeight = new ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>();
		ArrayList<ArrayList<ArrayList<Double>>> optimumWeight = new ArrayList<ArrayList<ArrayList<Double>>>();
		optimumWeight = allModel.get(index);
		finalWeight.add(optimumWeight);
		SaveModel.saveModels(finalWeight);

	}

	/**
	 * Retrieves a three-dimensional model for trend analysis from a data file. This
	 * method reads data from the specified file and returns it as a
	 * three-dimensional ArrayList structure. The data is expected to be organized
	 * in a specific format suitable for trend analysis.
	 *
	 * @param filePath The path to the data file containing the model data.
	 * @return A three-dimensional ArrayList structure representing the model data.
	 */
	public static ArrayList<ArrayList<ArrayList<Double>>> getModelForTrend(String filePath) {
		ArrayList<ArrayList<ArrayList<Double>>> dataList = readDataFile(filePath);

		return dataList;

	}

	/**
	 * Retrieves a four-dimensional model for seasonality analysis from a data file.
	 * This method reads data from the specified file, reshapes it into a
	 * four-dimensional ArrayList structure suitable for seasonality analysis, and
	 * returns the reshaped model.
	 *
	 * @param filePath       The path to the data file containing the model data.
	 * @param hyperParametes is the object of class HyperPrameters.
	 * @return A four-dimensional ArrayList structure representing the reshaped
	 *         model data.
	 */

	public static ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> getModelForSeasonality(String filename,
			HyperParameters hyperParametes) {
		
		
		ArrayList<ArrayList<ArrayList<Double>>> dataList = readDataFile(filename);
		
		allModel = reshape(dataList, hyperParametes);
		
		return allModel;

	}
}
