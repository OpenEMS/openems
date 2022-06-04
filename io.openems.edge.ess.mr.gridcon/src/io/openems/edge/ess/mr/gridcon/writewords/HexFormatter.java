package io.openems.edge.ess.mr.gridcon.writewords;

public class HexFormatter {

	// CHECKSTYLE:OFF
	public static String format(Float f, boolean switchQuarters) {
		return toTwoQuarterGroup(getFloatToHexString(f), switchQuarters);
	}

	public static String format(Integer i, boolean switchQuarters) {
		return toTwoQuarterGroup(getIntToHexString(i), switchQuarters);
	}

	public static String formatShort(Short i, boolean switchQuarters) {
		return toOneQuarterGroup(getShortToHexString(i));
	}
	// CHECKSTYLE:ON

	static String toOneQuarterGroup(String toFormat) {
		toFormat = addLeadingZeros(toFormat, 4);
		String newString = "";
		for (int i = 0; i < toFormat.length(); i++) {
			if (i % 4 == 0) {
				newString = newString + " ";
			}
			newString = newString + toFormat.charAt(i);

		}
		newString = newString.trim().toUpperCase();

		return newString;
	}

	static String toTwoQuarterGroup(String toFormat, boolean switchQuarters) {
		toFormat = addLeadingZeros(toFormat, 8);

		String newString = "";
		for (int i = 0; i < toFormat.length(); i++) {
			if (i % 4 == 0) {
				newString = newString + " ";
			}
			newString = newString + toFormat.charAt(i);

		}
		newString = newString.trim().toUpperCase();

		if (switchQuarters) {
			newString = switchQuarters(newString);
		}

		return newString;
	}

	static String switchQuarters(String s) {
		// "1234 5678" --> "5678 1234"; "1111 2222 3333 4444" --> "2222 1111 4444 3333"
		s = s.trim();

		int length = s.length();
		int groupLength = 8 + 1;
		int groupsToSwitch = length % groupLength + 1;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < groupsToSwitch; i++) {
			String subGroup = s.substring(groupLength * i, groupLength * i + groupLength);
			String g1 = subGroup.substring(0, 4);
			String g2 = subGroup.substring(5, 9);
			sb.append(g2);
			sb.append(" ");
			sb.append(g1);
			sb.append(" ");
		}
		return sb.toString().trim();
	}

	static String addLeadingZeros(String toFormat, int digits) {
		int length = toFormat.length();

		int remainder = length % digits;

		int toAdd = 0;
		if (remainder != 0 || length == 0) {
			toAdd = digits - remainder;
			;
		}

		for (int i = 0; i < toAdd; i++) {
			toFormat = "0" + toFormat;
		}
		return toFormat;
	}

	static String getFloatToHexString(Float input) {
		return Integer.toHexString(Float.floatToIntBits(input));
	}

	static String getIntToHexString(Integer input) {
		return Integer.toHexString(input);
	}

	static String getShortToHexString(Short input) {
		return Integer.toHexString(input);
	}
}
