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

public interface Pipeline1D extends Pipeline<double[]> {

	OffsetDateTime[] getDates();

	ArrayList<Double> to1DList();

	default Pipeline1D next1D(Stage<double[], double[]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getDates(), this.getHyperParameters());
	}

	default Pipeline2D next2D(Stage<double[], double[][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	default Pipeline3D next3D(Stage<double[], double[][][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	default Pipeline1D interpolate() {
		return this.next1D(new InterpolationStage(this.getHyperParameters(), this.getDates()));
	}

	default Pipeline1D scale() {
		return this.next1D(new ScalingStage(this.getHyperParameters()));
	}

	default Pipeline1D filterOutliers() {
		return this.next1D(FilterOutliers::filterOutlier);
	}

	default Pipeline3D groupByHoursAndMinutes() {
		return this.next3D(new GroupByStage(this.getHyperParameters(), this.getDates()));
	}

	default Pipeline1D reverseNormalize() {
		return this.next1D(new ReverseNormalizationStage(this.getHyperParameters()));
	}

	default Pipeline1D reverseNormalize(double mean, double standarDeviation) {
		return this.next1D(new ReverseNormalizationStage(this.getHyperParameters(), mean, standarDeviation));
	}

	default Pipeline1D reverseNormalize(double[] mean, double[] standarDeviation) {
		return this.next1D(new ReverseNormalization2dStage(this.getHyperParameters(), mean, standarDeviation));
	}

	default Pipeline1D reverseScale() {
		return this.next1D(new ReverseScalingStage(this.getHyperParameters()));
	}

	default Pipeline1D normalize() {
		return this.next1D(new NormalizeStage1D(this.getHyperParameters()));
	}

	default Pipeline1D removeNegatives() {
		return this.next1D(new RemoveNegativeStage());
	}

	default Pipeline1D movingAverage() {
		return this.next1D(new MovingAverageStage());
	}

	default Pipeline3D groupToWIndowSeasonality() {
		return this.next3D(new GroupToWIndowSeasonalityStage(this.getHyperParameters().getWindowSizeSeasonality()));
	}

	default Pipeline2D modifyForTrendPrediction() {
		return this.next2D(new ModifyDataForTrendStage(this.getDates(), this.getHyperParameters()));
	}

	default Pipeline3D groupedToStiffedWindow() {
		return this.next3D(new GroupedToStiffedWindowStage(this.getHyperParameters()));
	}

	public static class Pipeline1DImpl implements Pipeline1D {

		private final HyperParameters hyperParameters;
		private final double[] data;
		private OffsetDateTime[] dates = null;

		public Pipeline1DImpl(HyperParameters hyperParameters, double[] data, OffsetDateTime[] dates) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
			this.dates = dates;
		}

		public Pipeline1DImpl(HyperParameters hyperParameters, double[] data) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
		}

		@Override
		public HyperParameters getHyperParameters() {
			return this.hyperParameters;
		}

		@Override
		public double[] get() {
			return this.data;
		}

		@Override
		public OffsetDateTime[] getDates() {
			return this.dates;
		}

		@Override
		public ArrayList<Double> to1DList() {
			return UtilityConversion.to1DArrayList(this.get());
		}

	}

}
