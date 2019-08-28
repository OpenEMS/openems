package io.openems.edge.simulator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CsvUtils {

	/**
	 * Reads a CSV file from a JAR file.
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
					readTitles(result, csvFormat, line);
					isTitleLine = false;
					continue;
				}
				// start reading the values, parse them to doubles and add them to the result
				readRecord(result, csvFormat, factor, line);
			}
		}
		return result;
	}

	/**
	 * Reads a CSV file.
	 * 
	 * @param path      the path + filename of the CSV file
	 * @param csvFormat the CSV-Format
	 * @param factor    a multiplication factor to apply on the read number
	 * @throws IOException           on error
	 * @throws NumberFormatException on error
	 */
	public static DataContainer readCsvFile(File path, CsvFormat csvFormat, float factor)
			throws NumberFormatException, IOException {
		DataContainer result = new DataContainer();
		boolean isTitleLine = true;
		String line = null;
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			while ((line = br.readLine()) != null) {
				if (isTitleLine) {
					// read titles
					readTitles(result, csvFormat, line);
					isTitleLine = false;
					continue;
				}
				// start reading the values, parse them to doubles and add them to the result
				readRecord(result, csvFormat, factor, line);
			}
		}
		return result;
	}

	private static void readTitles(DataContainer result, CsvFormat csvFormat, String line) {
		result.setKeys(line.split(csvFormat.lineSeparator));
	}

	private static void readRecord(DataContainer result, CsvFormat csvFormat, float factor, String line) {
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
