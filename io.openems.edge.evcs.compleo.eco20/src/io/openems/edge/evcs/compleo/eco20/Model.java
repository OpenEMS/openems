package io.openems.edge.evcs.compleo.eco20;

public enum Model {
    COMPLEO_ECO_20(2), //
    WALLBE_ECO_20(3); //

    protected final int scaleFactor;

    Model(int scaleFactor) {
	this.scaleFactor = scaleFactor;
    }
}
