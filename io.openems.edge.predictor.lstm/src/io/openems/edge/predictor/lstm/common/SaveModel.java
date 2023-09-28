package io.openems.edge.predictor.lstm.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SaveModel {
	

	public static void saveModels(ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> weightMatrix, String fileName) {

		try {
			String relativePath = "\\TestFolder\\" + fileName;
			String path = new File(".").getCanonicalPath() + relativePath;
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
