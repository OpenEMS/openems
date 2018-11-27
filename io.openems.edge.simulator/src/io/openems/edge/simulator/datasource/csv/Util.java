package io.openems.edge.simulator.datasource.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

	private final static Logger log = LoggerFactory.getLogger(Util.class);

	private final static String SEPARATOR = ",";

	private static String getFileName(Source source) {
		if (source == null) {
			return null;
		}
		switch (source) {
		case H0_HOUSEHOLD_SUMMER_WEEKDAY_STANDARD_LOAD_PROFILE:
			return "h0-summer-weekday-standard-load-profile.csv";
		case H0_HOUSEHOLD_SUMMER_WEEKDAY_PV_PRODUCTION:
			return "h0-summer-weekday-pv-production.csv";
		case H0_HOUSEHOLD_SUMMER_WEEKDAY_NON_REGULATED_CONSUMPTION:
			return "h0-summer-weekday-non-regulated-consumption.csv";
		}
		return null;
	}

	protected static DataContainer getValues(Source source, float multiplier) {
		DataContainer result = new DataContainer();
		String fileName = Util.getFileName(source);
		// return null on error
		if (fileName == null) {
			return result;
		}
		// read file
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		String line = null;
		try {
			is = Util.class.getResourceAsStream(fileName);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			boolean isTitleLine = true;
			while ((line = br.readLine()) != null) {
				if (isTitleLine) {
					// read titles
					result.setKeys(line.split(SEPARATOR));
					isTitleLine = false;
					continue;
				}
				// start reading the values, parse them to doubles and add them to the result
				String[] values = line.split(SEPARATOR);
				float[] doubleValues = new float[values.length];
				for (int i = 0; i < values.length; i++) {
					doubleValues[i] = Float.parseFloat(values[i]) * multiplier;
				}
				result.addRecord(doubleValues);
			}
		} catch (NumberFormatException | IOException e) {
			log.error("Unable to parse file. " + e.getClass().getSimpleName() + ": " + e.getMessage());
		} finally {
			try {
				br.close();
				isr.close();
				is.close();
			} catch (IOException e) {
			}
		}
		return result;
	}
}
