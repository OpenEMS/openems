package io.openems.edge.predictor.lstmmodel.model;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class SaveModel {

	public ArrayList<ArrayList<Double>> finalWeight = new ArrayList<ArrayList<Double>>();

	public void saveModel(ArrayList<ArrayList<Double>> finalWeight, String fileName) {

		try {
			String relativePath = "\\testResults\\" + fileName;
			String actualPath = new File(".").getCanonicalPath()  + relativePath ;
			FileWriter fw = new FileWriter(actualPath);
			BufferedWriter bw = new BufferedWriter(fw);

			for (ArrayList<Double> innerList : finalWeight) {
				for (Double value : innerList) {
					bw.write(value.toString() + " ");
				}
				bw.newLine();
			}
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
