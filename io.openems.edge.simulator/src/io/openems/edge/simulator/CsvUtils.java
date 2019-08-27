package io.openems.edge.simulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CsvUtils {

	/**
	 * Reads the CSV file.
	 * 
	 * @param clazz     a class in the same java package as the file
	 * @param filename  the name of the file in the java package
	 * @param csvFormat the CSV-Format
	 * @param factor    a multiplication factor to apply on the read number
	 * @throws IOException           on error
	 * @throws NumberFormatException on error
	 */
	public static DataContainer readCsvFileFromRessource(Class<?> clazz, String filename, CsvFormat csvFormat,
			float factor) throws NumberFormatException, IOException {
		DataContainer result = new DataContainer();
		boolean isTitleLine = true;
		String line = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(clazz.getResourceAsStream(filename)))) {
			while ((line = br.readLine()) != null) {
				if (isTitleLine) {
					// read titles
					result.setKeys(line.split(csvFormat.lineSeparator));
					isTitleLine = false;
					continue;
				}
				// start reading the values, parse them to doubles and add them to the result
				String[] values = line.split(csvFormat.lineSeparator);
				float[] floatValues = new float[values.length];
				for (int i = 0; i < values.length; i++) {
					String value = values[i];
					if (csvFormat.decimalSeparator != ".") {
						value = value.replace(csvFormat.decimalSeparator, ".");
					}
					floatValues[i] = Float.parseFloat(value) * factor;
				}
				result.addRecord(floatValues);
			}
		}
		return result;
	}
}
