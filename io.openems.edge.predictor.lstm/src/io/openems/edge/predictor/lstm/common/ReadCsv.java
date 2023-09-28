package io.openems.edge.predictor.lstm.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class ReadCsv {

	public static final String FILENAME = "\\TestFolder\\Consumption_data_Fems_10005.csv";

	public ArrayList<Double> data = new ArrayList<Double>();
	public ArrayList<OffsetDateTime> dates = new ArrayList<OffsetDateTime>();

	public ReadCsv(String path) {
		
		getDataFromCSV(path);
	}

	public void getDataFromCSV(String fileName) {

		try {
			String filename = "\\TestFolder\\"+fileName;
			String path = new File(".").getCanonicalPath() + filename;

			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line = reader.readLine();

			while (line != null) {
				String[] parts = line.split(",");
				OffsetDateTime date = OffsetDateTime.parse(parts[0]);
				// OffsetDateTime temp1;
				Double temp2 = 0.0;

				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals("") || parts[i].equals("nan")) {
						temp2 = Double.NaN;
					} else {
						temp2 = (Double.parseDouble(parts[i]));
					}
				}

				dates.add(date);
				data.add(temp2);
				

				line = reader.readLine();
			}

			reader.close();
			System.out.println(Collections.min(data));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}