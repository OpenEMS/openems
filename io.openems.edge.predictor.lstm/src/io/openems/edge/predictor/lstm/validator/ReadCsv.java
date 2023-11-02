package io.openems.edge.predictor.lstm.validator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

public class ReadCsv {

	public static final String FILENAME = "\\testResults\\Consumption_data_Fems_10005.csv";

	private ArrayList<Double> data = new ArrayList<Double>();
	private ArrayList<OffsetDateTime> dates = new ArrayList<OffsetDateTime>();

	public ReadCsv() {
		this.getDataFromcsv();
	}

	/**
	 * Reads data from a CSV file and populates date and data lists. This method
	 * reads data from a CSV file, where each line represents a timestamp and data
	 * values separated by commas. It populates two lists: one for timestamps
	 * (OffsetDateTime) and one for data values (Double). If a data value is missing
	 * in the CSV (indicated by an empty string), it's set to Double.NaN. Note:
	 * Ensure that the file path for the CSV is correctly specified before calling
	 * this method.
	 */

	public void getDataFromcsv() {

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

				this.dates.add(date);
				this.data.add(temp2);

				line = reader.readLine();
			}
			reader.close();
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
