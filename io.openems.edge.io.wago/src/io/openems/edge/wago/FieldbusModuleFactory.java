package io.openems.edge.wago;

public class FieldbusModuleFactory {

	private int moduleCount400DI = 0;
	private int moduleCount523RO1Ch = 0;
	private int moduleCount501DO2Ch = 0;

	public FieldbusModule of(Wago parent, String moduleArtikelnr, String moduleType, FieldbusModuleKanal[] kanals,
			int inputOffset, int outputOffset) {
		switch (moduleArtikelnr) {
		case "750-4xx":
			switch (moduleType) {
			case "DI":
				return new Fieldbus400DI(parent, ++this.moduleCount400DI, inputOffset, outputOffset, kanals.length);
			}
			break;

		case "750-5xx":
			switch (moduleType) {
			case "DO/DIA":
				return new Fieldbus523RO1Ch(parent, ++this.moduleCount523RO1Ch, inputOffset, outputOffset);
			case "DO":
				return new Fieldbus501DO2Ch(parent, ++this.moduleCount501DO2Ch, inputOffset, outputOffset);
			}
			break;
		}
		throw new IllegalArgumentException("WAGO Fieldbus module is unknown! Article [" + moduleArtikelnr + "] Type ["
				+ moduleType + "] Channels [" + kanals.length + "]");
	}

}
