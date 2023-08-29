package io.openems.edge.predictor.lstm.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ReadModel {

	public ArrayList<ArrayList<Double>> finalWeight = new ArrayList<ArrayList<Double>>();

	public ArrayList<ArrayList<Double>> getFinalWeight() {
		try {
			String relativePath ="\\testResults\\model.txt";
			String actualPath = new File(".").getCanonicalPath() + relativePath;
			BufferedReader br = new BufferedReader(new FileReader(actualPath));

			String st;

			while ((st = br.readLine()) != null) {
				finalWeight.add(convert(st.split(" ")));
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return finalWeight;
	}

	public static ArrayList<Double> convert(String[] toBeConverted) {

		return (ArrayList<Double>) Stream.of(toBeConverted)//
				.map(Double::valueOf)//
				.collect(Collectors.toList());
	}

}
