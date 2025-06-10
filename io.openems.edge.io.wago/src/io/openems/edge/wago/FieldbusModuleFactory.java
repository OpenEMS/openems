package io.openems.edge.wago;

public class FieldbusModuleFactory {

	private int moduleCount4xxDI = 0;
	private int moduleCount523RO1Ch = 0;
	private int moduleCount5xxDO = 0;

	/**
	 * Builds a WAGO {@link FieldbusModule} instance, defined by the XML
	 * specification.
	 *
	 * @param parent          the {@link IoWagoImpl} parent component
	 * @param moduleArtikelnr the XML 'Artikelnr'
	 * @param moduleType      the XML 'Type'
	 * @param kanals          the XML 'Kanals'
	 * @param coilOffset0     the current offset for coils starting from 0
	 * @param coilOffset512   the current offset for coils starting from 512
	 * @return the {@link FieldbusModule}
	 */
	public FieldbusModule of(IoWagoImpl parent, String moduleArtikelnr, String moduleType, FieldbusModuleKanal[] kanals,
			int coilOffset0, int coilOffset512) {
		final var channelsCount = kanals.length;
		switch (moduleArtikelnr) {
		case "750-4xx":
			switch (moduleType) {
			case "DI":
				return new Fieldbus4xxDI(parent, ++this.moduleCount4xxDI, coilOffset0, channelsCount);
			}
			break;

		case "750-5xx":
			switch (moduleType) {
			case "DO/DIA":
				return new Fieldbus523RO1Ch(parent, ++this.moduleCount523RO1Ch, coilOffset0, coilOffset512);
			case "DO":
				return new Fieldbus5xxDO(parent, ++this.moduleCount5xxDO, coilOffset512, channelsCount);
			}
			break;
		}
		throw new IllegalArgumentException("WAGO Fieldbus module is unknown! Article [" + moduleArtikelnr + "] Type ["
				+ moduleType + "] Channels [" + kanals.length + "]");
	}

}
