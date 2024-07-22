package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage2D;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.ShuffleStage;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public interface Pipeline3D extends Pipeline<double[][][]> {

	Pipeline2D to2DList();

	default Pipeline3D next3D(Stage<double[][][], double[][][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	default Pipeline2D next2D(Stage<double[][], double[][]> stage) {
		var data = UtilityConversion.to2DList(this.get());
		return Pipeline.of(stage.execute(data), this.getHyperParameters());
	}

	default Pipeline3D normalize() {
		return this.next3D(new NormalizeStage(this.getHyperParameters()));
	}

	default Pipeline2D normalize2d() {
		return this.next2D(new NormalizeStage2D(this.getHyperParameters()));
	}

	default Pipeline3D shuffle() {
		return this.next3D(new ShuffleStage());
	}

	public static class Pipeline3DImpl implements Pipeline3D {

		private final HyperParameters hyperParameters;
		private final double[][][] data;

		public Pipeline3DImpl(HyperParameters hyperParameters, double[][][] data) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
		}

		@Override
		public HyperParameters getHyperParameters() {
			return this.hyperParameters;
		}

		@Override
		public double[][][] get() {
			return this.data;
		}

		@Override
		public Pipeline2D to2DList() {
			return Pipeline.of(UtilityConversion.to2DList(this.get()), this.getHyperParameters());
		}

	}

}
