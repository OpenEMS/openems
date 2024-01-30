package io.openems.edge.predictor.lstm.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;

import io.openems.common.OpenemsConstants;

public class SaveModel {

	/**
	 * Save a 4D ArrayList of Double values to a text file. This method takes a 4D
	 * ArrayList of Double values and a file name, and it saves the data to a text
	 * file with the specified file name. Each value in the ArrayList is written to
	 * the file, separated by spaces, with newlines between levels of the ArrayList.
	 *
	 * @param weightMatrix The 4D ArrayList of Double values to be saved.
	 * @param fileName     The name of the file to which the data will be saved.
	 */

	public static void saveModels(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix, String fileName) {

		try {
			File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
			String path = file.getAbsolutePath() + File.separator + "models" + File.separator + fileName;
			//System.out.println("Path  is : " + path);

			FileWriter fw = new FileWriter(path);
			BufferedWriter bw = new BufferedWriter(fw);

			for (ArrayList<ArrayList<ArrayList<Double>>> level1 : weightMatrix) {
				for (ArrayList<ArrayList<Double>> level2 : level1) {
					for (ArrayList<Double> level3 : level2) {
						for (Double value : level3) {
							bw.write(value.toString() + " ");
						}
						bw.newLine();
					}
					bw.newLine();
				}
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * Save a 4D ArrayList of Double values to a default text file. This method
	 * takes a 4D ArrayList of Double values and saves the data to a default text
	 * file named "SavedModel.txt" in the "TestFolder" directory. Each value in the
	 * ArrayList is written to the file, separated by spaces, with newlines between
	 * levels of the ArrayList.
	 *
	 * @param weightMatrix The 4D ArrayList of Double values to be saved.
	 */

	public static void saveModels(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix) {
		try {
			String filename = "\\TestFolder\\SavedModel.txt";
			String path = new File(".").getCanonicalPath() + filename;
			FileWriter fw = new FileWriter(path);
			BufferedWriter bw = new BufferedWriter(fw);

			for (ArrayList<ArrayList<ArrayList<Double>>> level1 : weightMatrix) {
				for (ArrayList<ArrayList<Double>> level2 : level1) {
					for (ArrayList<Double> level3 : level2) {
						for (Double value : level3) {
							bw.write(value.toString() + " ");
						}
						bw.newLine();
					}
					bw.newLine();
				}
				bw.newLine();
			}

			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
