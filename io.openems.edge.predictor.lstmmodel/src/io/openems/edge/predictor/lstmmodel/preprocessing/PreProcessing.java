package io.openems.edge.predictor.lstmmodel.preprocessing;

public interface PreProcessing {

	public double minScaled = 0.2;
	public double maxScaled = 0.8;

	public void scale(double minScaled, double maxScaled);

	public double[][] getFeatureData(int lower, int upper) throws Exception;

	public double[] getTargetData(int lower, int upper) throws Exception;

}
