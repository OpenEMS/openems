package io.openems.edge.predictor.lstmmodel.predictor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class ReadCsv {

	public static final String FILENAME = "\\testResults\\loadValues.csv";

	public ArrayList<Double> data = new ArrayList<Double>();
	public ArrayList<OffsetDateTime> dates = new ArrayList<OffsetDateTime>();

	public ReadCsv() {
		getDataFromCSV();
	}

	public void getDataFromCSV() {

		try {
			String filename = "\\testResults\\time_series_15min_singleindex_filtered.csv";
			String path = new File(".").getCanonicalPath() + filename;

			BufferedReader reader = new BufferedReader(new FileReader(path));

			String line = reader.readLine();

			while (line != null) {
				String[] parts = line.split(",");
				OffsetDateTime date = OffsetDateTime.parse(parts[0]);
				// OffsetDateTime temp1;
				Double temp2 = 0.0;

				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals("")) {
						temp2 = Double.NaN;
					} else {
						temp2 = (Double.parseDouble(parts[i]));
					}
				}

				dates.add(date);
				data.add(temp2);

				line = reader.readLine();
			}

			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
