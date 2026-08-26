package io.openems.edge.predictor.api.mlcore.clustering;

import org.apache.commons.math3.ml.clustering.Clusterable;

public record DataPoint(double[] point) implements Clusterable {

	@Override
	public double[] getPoint() {
		return this.point;
	}
}
