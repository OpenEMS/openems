package io.openems.edge.wago;

/**
 * Represents one XML "Kanal" element inside the WAGO ea-config.xml file.
 */
public class FieldbusModuleKanal {
	public final String name;
	public final String type;

	public FieldbusModuleKanal(String name, String type) {
		this.name = name;
		this.type = type;
	}
}
