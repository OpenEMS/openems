package io.openems.edge.predictor.production.linearmodel;

public enum ModelComplexity {

	LOW(50), //

	HIGH(100), //

	;

	private final int numTrees;

	ModelComplexity(int numTrees) {
		this.numTrees = numTrees;
	}

	public int getNumTrees() {
		return this.numTrees;
	}
}
