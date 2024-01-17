package io.openems.edge.predictor.lstm.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.common.OpenemsConstants;

public class ReadCsv {

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

//			String openemsDirectory = OpenemsConstants.getOpenemsDataDir();
//			// String filename = "\\TestFolder\\" + fileName;
//			File file = new File(openemsDirectory + "/models/" + fileName);
//			String path = file.getAbsolutePath();

			File file = Paths.get(OpenemsConstants.getOpenemsDataDir()).toFile();
			String path = file.getAbsolutePath() + File.separator + "models" + File.separator + fileName;

			System.out.println(path);

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