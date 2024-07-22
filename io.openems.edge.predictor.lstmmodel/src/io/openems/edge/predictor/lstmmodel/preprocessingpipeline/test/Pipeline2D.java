package io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test;

import java.util.ArrayList;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.test.stage.NormalizeStage2D;
import io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion;

public interface Pipeline2D extends Pipeline<double[][]> {

	ArrayList<ArrayList<Double>> to2DList();

	default Pipeline2D next2D(Stage<double[][], double[][]> stage) {
		return Pipeline.of(stage.execute(this.get()), this.getHyperParameters());
	}

	default Pipeline2D normalize() {
		return this.next2D(new NormalizeStage2D(this.getHyperParameters()));
	}

	public static class Pipeline2DImpl implements Pipeline2D {

		private final HyperParameters hyperParameters;
		private final double[][] data;

		public Pipeline2DImpl(HyperParameters hyperParameters, double[][] data) {
			super();
			this.hyperParameters = hyperParameters;
			this.data = data;
		}

		@Override
		public HyperParameters getHyperParameters() {
			// TODO Auto-generated method stub
			return this.hyperParameters;
		}

		@Override
		public double[][] get() {
			// TODO Auto-generated method stub
			return this.data;
		}

		@Override
		public ArrayList<ArrayList<Double>> to2DList() {
			return UtilityConversion.to2DArrayList(this.get());
		}

	}

}
