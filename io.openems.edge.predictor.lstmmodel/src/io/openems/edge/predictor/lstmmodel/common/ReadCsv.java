package io.openems.edge.predictor.lstmmodel.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.common.OpenemsConstants;

public class ReadCsv {

	private static final String MODEL_DIRECTORY = Paths.get(OpenemsConstants.getOpenemsDataDir())//
			.toFile().getAbsolutePath();
	private static final String MODEL_FOLDER = File.separator + "models" + File.separator;

	private ArrayList<Double> data = new ArrayList<Double>();
	private ArrayList<OffsetDateTime> dates = new ArrayList<OffsetDateTime>();

	public ReadCsv(String path) {
		this.getDataFromCsv(path);
	}

	/**
	 * Reads data from a CSV file and populates class fields with the data. This
	 * method reads data from a CSV file specified by the provided file name. Each
	 * line in the CSV file is expected to contain timestamped data points, where
	 * the first column represents timestamps in the ISO-8601 format and subsequent
	 * columns represent numeric data. The data is parsed, and the timestamps and
	 * numeric values are stored in class fields for further processing.
	 *
	 * @param fileName The name of the CSV file to read data from.
	 * @throws IOException if there are issues reading the file.
	 */
	public void getDataFromCsv(String fileName) {

		try {
			var path = Paths.get(MODEL_DIRECTORY, MODEL_FOLDER, fileName)//
					.toString();

			var reader = new BufferedReader(new FileReader(path));
			var line = reader.readLine();

			while (line != null) {
				var parts = line.split(",");
				var date = OffsetDateTime.parse(parts[0]);
				var temp2 = 0.0;

				for (int i = 1; i < parts.length; i++) {
					if (parts[i].equals("") || parts[i].equals("nan")) {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<Double> getData() {
		return this.data;
	}

	public ArrayList<OffsetDateTime> getDates() {
		return this.dates;
	}
}
