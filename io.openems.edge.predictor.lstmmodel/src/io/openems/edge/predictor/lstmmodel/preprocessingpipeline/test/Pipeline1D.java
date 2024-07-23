package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.FilterOutliers;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.GroupByStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.GroupToWIndowSeasonalityStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.GroupedToStiffedWindowStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.InterpolationStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ModifyDataForTrendStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.MovingAverageStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage1D;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.RemoveNegativeStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ReverseNormalization2dStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ReverseNormalizationStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ReverseScalingStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ScalingStage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

/**
 * The {@code Pipeline1D} interface extends {@code Pipeline} for 1-dimensional
 * (1D) data arrays. It provides methods to transform and process the 1D data
 * with various stages and utilities.
 */
public interface Pipeline1D extends Pipeline<double[]> {

	/**
	 * Gets the array of dates corresponding to the 1D data.
	 *
	 * @return an array of {@code OffsetDateTime} representing the dates of the data
	 */
	OffsetDateTime[] getDates();

	/**
	 * Converts the 1D array data to a list of doubles.
	 *
	 * @return an {@code ArrayList<Double>} containing the 1D data
	 */
	ArrayList<Double> to1DList();

	/**
	 * Applies the given stage to the current 1D pipeline and returns the resulting
	 * 1D pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline1D} instance with the stage applied
	 */
	default Pipeline1D next1D(Stage<double[], double[]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getDates(), this.getHyperParameters());
	}

	/**
	 * Applies the given stage to the current 1D pipeline and returns the resulting
	 * 2D pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline2D} instance with the stage applied
	 */
	default Pipeline2D next2D(Stage<double[], double[][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	/**
	 * Applies the given stage to the current 1D pipeline and returns the resulting
	 * 3D pipeline.
	 *
	 * @param stage the stage to apply
	 * @return a new {@code Pipeline3D} instance with the stage applied
	 */
	default Pipeline3D next3D(Stage<double[], double[][][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	/**
	 * Applies interpolation to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with interpolated data
	 */
	default Pipeline1D interpolate() {
		return this.next1D(new InterpolationStage(this.getHyperParameters(), this.getDates()));
	}

	/**
	 * Applies scaling to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with scaled data
	 */
	default Pipeline1D scale() {
		return this.next1D(new ScalingStage(this.getHyperParameters()));
	}

	/**
	 * Filters outliers from the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with outliers filtered out
	 */
	default Pipeline1D filterOutliers() {
		return this.next1D(FilterOutliers::filterOutlier);
	}

	/**
	 * Groups the data by hours and minutes and returns a new 3D pipeline.
	 *
	 * @return a new {@code Pipeline3D} instance with data grouped by hours and
	 *         minutes
	 */
	default Pipeline3D groupByHoursAndMinutes() {
		return this.next3D(new GroupByStage(this.getHyperParameters(), this.getDates()));
	}

	/**
	 * Applies reverse normalization to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with reverse normalized data
	 */
	default Pipeline1D reverseNormalize() {
		return this.next1D(new ReverseNormalizationStage(this.getHyperParameters()));
	}

	/**
	 * Applies reverse normalization to the data in the pipeline with specified mean
	 * and standard deviation.
	 *
	 * @param mean             the mean value for reverse normalization
	 * @param standarDeviation the standard deviation value for reverse
	 *                         normalization
	 * @return a new {@code Pipeline1D} instance with reverse normalized data
	 */
	default Pipeline1D reverseNormalize(double mean, double standarDeviation) {
		return this.next1D(new ReverseNormalizationStage(this.getHyperParameters(), mean, standarDeviation));
	}

	/**
	 * Applies reverse normalization to the data in the pipeline with specified
	 * means and standard deviations for each dimension.
	 *
	 * @param mean             an array of mean values for each dimension
	 * @param standarDeviation an array of standard deviation values for each
	 *                         dimension
	 * @return a new {@code Pipeline1D} instance with reverse normalized data
	 */
	default Pipeline1D reverseNormalize(double[] mean, double[] standarDeviation) {
		return this.next1D(new ReverseNormalization2dStage(this.getHyperParameters(), mean, standarDeviation));
	}

	/**
	 * Applies reverse scaling to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with reverse scaled data
	 */
	default Pipeline1D reverseScale() {
		return this.next1D(new ReverseScalingStage(this.getHyperParameters()));
	}

	/**
	 * Applies normalization to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with normalized data
	 */
	default Pipeline1D normalize() {
		return this.next1D(new NormalizeStage1D(this.getHyperParameters()));
	}

	/**
	 * Removes negative values from the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with negative values removed
	 */
	default Pipeline1D removeNegatives() {
		return this.next1D(new RemoveNegativeStage());
	}

	/**
	 * Applies a moving average to the data in the pipeline.
	 *
	 * @return a new {@code Pipeline1D} instance with a moving average applied
	 */
	default Pipeline1D movingAverage() {
		return this.next1D(new MovingAverageStage());
	}

	/**
	 * Groups data to a window seasonality and returns a new 3D pipeline.
	 *
	 * @return a new {@code Pipeline3D} instance with data grouped by window
	 *         seasonality
	 */
	default Pipeline3D groupToWIndowSeasonality() {
		return this.next3D(new GroupToWIndowSeasonalityStage(this.getHyperParameters().getWindowSizeSeasonality()));
	}

	/**
	 * Modifies data for trend prediction and returns a new 2D pipeline.
	 *
	 * @return a new {@code Pipeline2D} instance with modified data for trend
	 *         prediction
	 */
	default Pipeline2D modifyForTrendPrediction() {
		return this.next2D(new ModifyDataForTrendStage(this.getDates(), this.getHyperParameters()));
	}

	/**
	 * Groups data to stiffened windows and returns a new 3D pipeline.
	 *
	 * @return a new {@code Pipeline3D} instance with data grouped to stiffened
	 *         windows
	 */
	default Pipeline3D groupedToStiffedWindow() {
		return this.next3D(new GroupedToStiffedWindowStage(this.getHyperParameters()));
	}

	/**
	 * The {@code Pipeline1DImpl} class is an implementation of the
	 * {@code Pipeline1D} interface.
	 */
	public static class Pipeline1DImpl implements Pipeline1D {

		private final HyperParameters hyperParameters;
		private final double[] data;
		private OffsetDateTime[] dates = null;

		/**
		 * Constructs a new {@code Pipeline1DImpl} with the given hyperparameters, data,
		 * and dates.
		 *
		 * @param hyperParameters the hyperparameters for the pipeline
		 * @param data            the 1D array of double values
		 * @param dates           the array of dates corresponding to the data
		 */
		public Pipeline1DImpl(HyperParameters hyperParameters, double[] data, OffsetDateTime[] dates) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
			this.dates = dates;
		}

		/**
		 * Constructs a new {@code Pipeline1DImpl} with the given hyperparameters and
		 * data.
		 *
		 * @param hyperParameters the hyperparameters for the pipeline
		 * @param data            the 1D array of double values
		 */
		public Pipeline1DImpl(HyperParameters hyperParameters, double[] data) {
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
		public double[] get() {
			return this.data;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public OffsetDateTime[] getDates() {
			return this.dates;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public ArrayList<Double> to1DList() {
			return UtilityConversion.to1DArrayList(this.get());
		}

	}

}
