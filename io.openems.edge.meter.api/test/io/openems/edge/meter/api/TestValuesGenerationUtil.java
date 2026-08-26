package io.openems.edge.meter.api;

import java.util.LinkedHashMap;
import java.util.Map;

import io.openems.edge.meter.test.InvertTest;

/**
 * Class for generating the values needed for a {@link InvertTest}.
 */
public class TestValuesGenerationUtil {

	public enum WordOrder {
		MSW_LSW, // Most Significant Word first (standard Modbus)
		LSW_MSW // Least Significant Word first (alternative format)
	}

	// Converts a float value to a pair of LSW/MSW 16-bit hex values, ready for
	// Modbus
	private static String[] floatToHexWords(float value) {
		int bits = Float.floatToIntBits(value);
		String msw = String.format("0x%04X", (bits >>> 16) & 0xFFFF); // Upper 16 bits
		String lsw = String.format("0x%04X", bits & 0xFFFF); // Lower 16 bits

		final var order = WordOrder.LSW_MSW;

		if (order == WordOrder.MSW_LSW) {
			return new String[] { msw, lsw }; // Swap for LSW-MSW
		} else {
			return new String[] { lsw, msw }; // Normal MSW-LSW
		}
	}

	/**
	 * Main method.
	 * 
	 * @param args args
	 */
	public static void main(String[] args) {
		// Set these scale factors
		var voltageScale = 0.001f;
		var currentScale = 0.001f;
		var activePowerScale = 1f;
		var reactivePowerScale = 1;
		var frequencyScale = 0.001f;
		var energyScale = 1f;
		// Example input values - replace with your real test values if needed
		float[] voltages = { 1000f, 1000f, 1000f, 1000f };
		float[] currents = { 1000f, 1000f, 1000f, 3000f };
		float[] activePowers = { 10000f, 10000f, 10000f, 13000f };
		float[] reactivePowers = { 7000f, 7000f, 7000f, 7000f };
		float frequency = 5000f;

		// Map to store the results (description -> hex words)
		Map<String, ValueDecimalAndHex> registerValues = new LinkedHashMap<>();

		// Populate the map with ValueDecimalAndHex
		registerValues.put("VOLTAGE_L1",
				new ValueDecimalAndHex(voltages[0], floatToHexWords(voltages[0] * voltageScale)));

		registerValues.put("VOLTAGE_L2",
				new ValueDecimalAndHex(voltages[1], floatToHexWords(voltages[1] * voltageScale)));

		registerValues.put("VOLTAGE_L3",
				new ValueDecimalAndHex(voltages[2], floatToHexWords(voltages[2] * voltageScale)));

		registerValues.put("VOLTAGE", new ValueDecimalAndHex(voltages[3], floatToHexWords(voltages[3] * voltageScale)));

		registerValues.put("CURRENT_L1",
				new ValueDecimalAndHex(currents[0], floatToHexWords(currents[0] * currentScale)));

		registerValues.put("CURRENT_L2",
				new ValueDecimalAndHex(currents[1], floatToHexWords(currents[1] * currentScale)));

		registerValues.put("CURRENT_L3",
				new ValueDecimalAndHex(currents[2], floatToHexWords(currents[2] * currentScale)));

		registerValues.put("CURRENT", new ValueDecimalAndHex(currents[3], floatToHexWords(currents[3] * currentScale)));

		registerValues.put("ACTIVE_POWER_L1",
				new ValueDecimalAndHex(activePowers[0], floatToHexWords(activePowers[0] * activePowerScale)));

		registerValues.put("ACTIVE_POWER_L2",
				new ValueDecimalAndHex(activePowers[1], floatToHexWords(activePowers[1] * activePowerScale)));

		registerValues.put("ACTIVE_POWER_L3",
				new ValueDecimalAndHex(activePowers[2], floatToHexWords(activePowers[2] * activePowerScale)));

		registerValues.put("ACTIVE_POWER",
				new ValueDecimalAndHex(activePowers[3], floatToHexWords(activePowers[3] * activePowerScale)));

		registerValues.put("REACTIVE_POWER_L1",
				new ValueDecimalAndHex(reactivePowers[0], floatToHexWords(reactivePowers[0] * reactivePowerScale)));

		registerValues.put("REACTIVE_POWER_L2",
				new ValueDecimalAndHex(reactivePowers[1], floatToHexWords(reactivePowers[1] * reactivePowerScale)));

		registerValues.put("REACTIVE_POWER_L3",
				new ValueDecimalAndHex(reactivePowers[2], floatToHexWords(reactivePowers[2] * reactivePowerScale)));

		registerValues.put("REACTIVE_POWER",
				new ValueDecimalAndHex(reactivePowers[3], floatToHexWords(reactivePowers[3] * reactivePowerScale)));

		registerValues.put("FREQUENCY", new ValueDecimalAndHex(frequency, floatToHexWords(frequency * frequencyScale)));

		registerValues.put("ACTIVE_PRODUCTION_ENERGY",
				new ValueDecimalAndHex(activePowers[3], floatToHexWords(activePowers[3] * energyScale)));

		registerValues.put("ACTIVE_CONSUMPTION_ENERGY", //
				new ValueDecimalAndHex(0f, floatToHexWords(0f * energyScale)));

		// Print the map content
		System.out.println("Generated Modbus Registers (with labels):");
		for (var entry : registerValues.entrySet()) {
			String[] hexWords = entry.getValue().words();
			System.out.println("//" + entry.getKey() + ": " + entry.getValue().value);
			System.out.printf("%s, %s\n", hexWords[0], hexWords[1]);
		}

	}

	public record ValueDecimalAndHex(Float value, String[] words) {

	}
}
