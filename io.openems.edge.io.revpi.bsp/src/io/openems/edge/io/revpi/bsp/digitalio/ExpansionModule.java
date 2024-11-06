package io.openems.edge.io.revpi.bsp.digitalio;

public enum ExpansionModule {
    /**
     * enhancement board Data InOut. 
     */
    REVPI_DIO(0), //
    /**
     * enhancement board Data In. 
     */
    REVPI_DI(1), //
    /**
     * enhancement board Data Out. 
     */
    REVPI_DO(2), //
    ;

    private ExpansionModule(int typeSelector) {
	this.typeSelector = typeSelector;
    }

    public int typeSelector;

}
