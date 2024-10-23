package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

/**
 * The {@code Pipeline3D} interface extends {@code Pipeline} for 3-dimensional (3D) data arrays.
 * It provides methods to transform and process the 3D data with various stages and utilities.
 */
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage2D;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ShuffleStage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public interface Pipeline3D extends Pipeline<double[][][]> {

	/**
	 * Converts the 3D array data to a 2D pipeline.
	 *
	 * @return a {@code Pipeline2D} instance containing the converted 2D data
	 */
	Pipeline2D to2DList();

	/**
	 * Applies the given stage to the current 3D pipeline and returns the resulting
	 * 3D pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline3D} instance with the stage applied
	 */
	default Pipeline3D next3D(Stage<double[][][], double[][][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	/**
	 * Applies the given stage to the current 3D pipeline and returns the resulting
	 * 2D pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline2D} instance with the stage applied
	 */
	default Pipeline2D next2D(Stage<double[][], double[][]> stage) {
		var data = UtilityConversion.to2DList(this.get());
		return Pipeline.of(stage.execute(data), this.getHyperParameters());
	}

	/**
	 * Normalizes the data in the pipeline.
	 *
	 * @return a new {@code Pipeline3D} instance with normalized data
	 */
	default Pipeline3D normalize() {
		return this.next3D(new NormalizeStage(this.getHyperParameters()));
	}

	/**
	 * Normalizes the data in the pipeline to 2D.
	 *
	 * @return a new {@code Pipeline2D} instance with normalized 2D data
	 */
	default Pipeline2D normalize2d() {
		return this.next2D(new NormalizeStage2D(this.getHyperParameters()));
	}

	/**
	 * Shuffles the data in the pipeline.
	 *
	 * @return a new {@code Pipeline3D} instance with shuffled data
	 */
	default Pipeline3D shuffle() {
		return this.next3D(new ShuffleStage());
	}

	/**
	 * The {@code Pipeline3DImpl} class is an implementation of the
	 * {@code Pipeline3D} interface.
	 */
	public static class Pipeline3DImpl implements Pipeline3D {

		private final HyperParameters hyperParameters;
		private final double[][][] data;

		/**
		 * Constructs a new {@code Pipeline3DImpl} with the given hyperparameters and
		 * data.
		 *
		 * @param hyperParameters the hyperparameters for the pipeline
		 * @param data            the 3D array of double values
		 */
		public Pipeline3DImpl(HyperParameters hyperParameters, double[][][] data) {
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
		public double[][][] get() {
			return this.data;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Pipeline2D to2DList() {
			return Pipeline.of(UtilityConversion.to2DList(this.get()), this.getHyperParameters());
		}

	}

}
