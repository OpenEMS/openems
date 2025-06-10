package io.openems.edge.bridge.modbus.sunspec;

import static java.util.stream.Collectors.joining;

import java.util.stream.Stream;

import com.google.common.base.CaseFormat;

public class Utils {

	private Utils() {
	}

	/**
	 * Converts a String ID to UPPER_UNDERSCORE.
	 * 
	 * @param string the source string
	 * @return the converted string
	 */
	public static String toUpperUnderscore(String string) {
		string = string //
				.replace("-", "_") //
				.replace(" ", "_");
		if (!string.toUpperCase().equals(string)) {
			string = CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, string);
		}
		return string.replace("__", "_");
	}

	/**
	 * Converts a String ID to a human readable label.
	 * 
	 * @param string the source string
	 * @return the converted string
	 */
	public static String toLabel(String string) {
		if (string.toUpperCase() != string || !string.contains("_")) {
			// Directly return non-uppercase strings
			return string;
		}
		// Taken from https://stackoverflow.com/a/28560959/4137113
		return Stream.of(string.trim().split("\\s|_")) //
				.filter(word -> word.length() > 0) //
				.map(word -> switch (word) {
				case "AC", "DC", "HW" -> word;
				default -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase(); //
				}) //
				.collect(joining(" "));
	}
}
