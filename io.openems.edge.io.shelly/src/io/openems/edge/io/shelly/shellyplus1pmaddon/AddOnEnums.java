package io.openems.edge.io.shelly.shellyplus1pmaddon;

public class AddOnInputType {
	
	public enum InputType {
		TEMPERATURE,
		TEMPERATURE_AND_HUMIDITY,
		VOLTAGE,
		DIGITAL_INPUT,
		ANALOG_INPUT,
		NONE;
	}


	public enum InputIndex {
		Index100 (100),
		Index101 (101),
		Index102 (102),
		Index103 (103),
		Index104 (104);

		int index;
		InputIndex(int index) {
			this.index = index;
		}
		


	}
	
}

