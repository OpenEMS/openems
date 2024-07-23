package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline1D.Pipeline1DImpl;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline2D.Pipeline2DImpl;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.Pipeline3D.Pipeline3DImpl;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArray;

/**
 * The {@code Pipeline} interface defines a pipeline for processing data with
 * {@link HyperParameters}.
 *
 * @param <O> the type of output produced by the pipeline
 */
public interface Pipeline<O> {

	/**
	 * Creates a 1D pipeline from a list of doubles and a list of dates with the
	 * specified {@link HyperParameters}.
	 *
	 * @param data            the list of double values
	 * @param date            the list of dates corresponding to the data
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline1D} instance
	 */
	static Pipeline1D of(ArrayList<Double> data, ArrayList<OffsetDateTime> date, HyperParameters hyperParameters) {
		var dataAr = to1DArray(data);
		var dateAr = to1DArray(date);
		return new Pipeline1DImpl(hyperParameters, dataAr, dateAr);
	}

	/**
	 * Creates a 1D pipeline from a list of doubles with the specified
	 * {@link HyperParameters}.
	 *
	 * @param data            the list of double values
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline1D} instance
	 */
	static Pipeline1D of(ArrayList<Double> data, HyperParameters hyperParameters) {
		var dataAr = to1DArray(data);
		return new Pipeline1DImpl(hyperParameters, dataAr);
	}

	/**
	 * Creates a 1D pipeline from an array of doubles and an array of dates with the
	 * specified {@link HyperParameters}.
	 *
	 * @param data            the array of double values
	 * @param date            the array of dates corresponding to the data
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline1D} instance
	 */
	static Pipeline1D of(double[] data, OffsetDateTime[] date, HyperParameters hyperParameters) {
		return new Pipeline1DImpl(hyperParameters, data, date);
	}

	/**
	 * Creates a 1D pipeline from an array of doubles with the specified
	 * {@link HyperParameters}.
	 *
	 * @param data            the array of double values
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline1D} instance
	 */
	static Pipeline1D of(double[] data, HyperParameters hyperParameters) {
		return new Pipeline1DImpl(hyperParameters, data);
	}

	/**
	 * Creates a 2D pipeline from a 2D array of doubles with the specified
	 * {@link HyperParameters}.
	 *
	 * @param data            the 2D array of double values
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline2D} instance
	 */
	static Pipeline2D of(double[][] data, HyperParameters hyperParameters) {
		return new Pipeline2DImpl(hyperParameters, data);
	}

	/**
	 * Creates a 3D pipeline from a 3D array of doubles with the specified
	 * {@link HyperParameters}.
	 *
	 * @param data            the 3D array of double values
	 * @param hyperParameters the {@link HyperParameters} for the pipeline
	 * @return a {@code Pipeline3D} instance
	 */
	static Pipeline3D of(double[][][] data, HyperParameters hyperParameters) {
		return new Pipeline3DImpl(hyperParameters, data);
	}

	/**
	 * Gets the {@link HyperParameters} of the pipeline.
	 *
	 * @return the hyperparameters
	 */
	HyperParameters getHyperParameters();

	/**
	 * Gets the output of the pipeline.
	 *
	 * @return the output
	 */
	O get();

}
