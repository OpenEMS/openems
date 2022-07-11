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
	 * @return a {@link DataContainer}
	 * @throws IOException           on error
	 * @throws NumberFormatException on error
	 */
	public static DataContainer readCsvFileFromResource(Class<?> clazz, String filename, CsvFormat csvFormat,
			float factor) throws NumberFormatException, IOException {
		var result = new DataContainer();
		var isTitleLine = true;
		String line = null;
		try (var br = new BufferedReader(new InputStreamReader(clazz.getResourceAsStream(filename)))) {
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
	 * @return a {@link DataContainer}
	 * @throws IOException           on error
	 * @throws NumberFormatException on error
	 */
	public static DataContainer readCsvFile(File path, CsvFormat csvFormat, float factor)
			throws NumberFormatException, IOException {
		var result = new DataContainer();
		var isTitleLine = true;
		String line = null;
		try (var br = new BufferedReader(new FileReader(path))) {
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
	 * @param csv       the CSV content
	 * @param csvFormat the CSV-Format
	 * @param factor    a multiplication factor to apply on the read number
	 * @return a {@link DataContainer}
	 * @throws IOException           on error
	 * @throws NumberFormatException on error
	 */
	public static DataContainer parseCsv(String csv, CsvFormat csvFormat, float factor)
			throws NumberFormatException, IOException {
		var result = new DataContainer();
		var isTitleLine = true;
		var lines = csv.split("\\r?\\n");
		for (String line : lines) {
			if (isTitleLine) {
				isTitleLine = false;
				if (!isNumeric(line)) {
					// read titles
					readTitles(result, csvFormat, line);
					continue;
				}
			}
			// start reading the values, parse them to doubles and add them to the result
			readRecord(result, csvFormat, factor, line);
		}
		return result;
	}

	private static void readTitles(DataContainer result, CsvFormat csvFormat, String line) {
		result.setKeys(line.split(csvFormat.lineSeparator));
	}

	private static void readRecord(DataContainer result, CsvFormat csvFormat, float factor, String line) {
		var values = line.split(csvFormat.lineSeparator);
		var floatValues = new Float[values.length];
		for (var i = 0; i < values.length; i++) {
			var value = values[i];
			if (value == null || value.isEmpty()) {
				value = null;
			} else {
				if (csvFormat.decimalSeparator != ".") {
					value = value.replace(csvFormat.decimalSeparator, ".");
				}
				floatValues[i] = Float.parseFloat(value) * factor;
			}
		}
		result.addRecord(floatValues);
	}

	/**
	 * Returns true if the given value is a number.
	 *
	 * @param strNum a value to be evaluated
	 * @return true for numbers
	 */
	private static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			Double.parseDouble(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
}
