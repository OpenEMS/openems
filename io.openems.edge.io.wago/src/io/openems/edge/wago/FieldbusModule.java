package io.openems.edge.wago;

import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.common.channel.BooleanReadChannel;

public abstract class FieldbusModule {

	public static FieldbusModule of(Wago parent, String moduleArtikelnr, String moduleType,
			FieldbusModuleKanal[] kanals, int inputOffset, int outputOffset) {
		switch (moduleArtikelnr) {
		case "750-4xx":
			switch (moduleType) {
			case "DI":
				return new Fieldbus400DI2Ch(parent, inputOffset, outputOffset);
			}
			break;

		case "750-5xx":
			switch (moduleType) {
			case "DO/DIA":
				return new Fieldbus523RO1Ch(parent, inputOffset, outputOffset);
			case "DO":
				return new Fieldbus501DO2Ch(parent, inputOffset, outputOffset);
			}
			break;
		}
		throw new IllegalArgumentException("WAGO Fieldbus module is unknown! Article [" + moduleArtikelnr + "] Type ["
				+ moduleType + "] Channels [" + kanals.length + "]");
	}

	public abstract String getName();

	public abstract AbstractModbusElement<?>[] getInputElements();

	public abstract AbstractModbusElement<?>[] getOutputElements();

	public abstract int getOutputCoils();

	public abstract int getInputCoils();
	
	public abstract BooleanReadChannel[] getChannels();

}
