package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage2D;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

/**
 * The {@code Pipeline2D} interface defines a pipeline for processing 2D data
 * arrays with hyperparameters.
 */
public interface Pipeline2D extends Pipeline<double[][]> {

	/**
	 * Converts the 2D array data to a list of lists of doubles.
	 *
	 * @return a 2D list of double values
	 */
	ArrayList<ArrayList<Double>> to2DList();

	/**
	 * Applies the given stage to the current pipeline and returns the resulting
	 * pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline2D} instance with the stage applied
	 */
	default Pipeline2D next2D(Stage<double[][], double[][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	/**
	 * Normalizes the data in the pipeline using the {@code NormalizeStage2D}.
	 *
	 * @return a new {@code Pipeline2D} instance with normalized data
	 */
	default Pipeline2D normalize() {
		return this.next2D(new NormalizeStage2D(this.getHyperParameters()));
	}

	/**
	 * The {@code Pipeline2DImpl} class is an implementation of the
	 * {@code Pipeline2D} interface.
	 */
	public static class Pipeline2DImpl implements Pipeline2D {

		private final HyperParameters hyperParameters;
		private final double[][] data;

		/**
		 * Constructs a new {@code Pipeline2DImpl} with the given hyperparameters and
		 * data.
		 *
		 * @param hyperParameters the hyperparameters for the pipeline
		 * @param data            the 2D array of double values
		 */
		public Pipeline2DImpl(HyperParameters hyperParameters, double[][] data) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public HyperParameters getHyperParameters() {
			return this.hyperParameters;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public double[][] get() {
			return this.data;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ArrayList<ArrayList<Double>> to2DList() {
			return UtilityConversion.to2DArrayList(this.get());
		}
	}
}
