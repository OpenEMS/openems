package io.openems.edge.predictor.lstmmodel.preprocessingpipeline;

import java.lang.reflect.Array;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class NormalizePipe implements Stage<Object, Object> {

	private HyperParameters hyperParameters;

	public NormalizePipe(HyperParameters hyper) {
		this.hyperParameters = hyper;

	}

	@Override
	public Object execute(Object input) {

		var decission = new Decission(input, this.hyperParameters);

		return decission.process.execute(null);

	}

	private class Decission {
		private Object input;
		private Stage<?, ?> process;
		private HyperParameters hyperParameters;

		public Decission(Object in, HyperParameters hyper) {
			this.input = in;
			this.hyperParameters = hyper;
			this.make();

		}

		public void make() {

			if (this.input instanceof double[][][]) {
				this.process = new Normalize3Dim(this.hyperParameters,(double[][][])this.input );
			} else if (this.input instanceof double[][]) {
				this.process = new Normalize2Dim(this.hyperParameters,(double[][])this.input);
			} else if (this.input instanceof double[]) {
				this.process = new Normalize1Dim(this.hyperParameters,(double[])this.input);
			} else {
				throw new IllegalArgumentException("Illegal Argument encountered during normalization");
			}

		}

		
	}

	private class Normalize3Dim implements Stage<double[][][], double[][][]> {
		private HyperParameters hyperParameters;
		private double[][][]input ;

		public Normalize3Dim(HyperParameters hype,double[][][]data) {
			this.hyperParameters = hype;
			this.input=data;
		}

		
		
		

		@Override
		public double[][][] execute(double[][][] in) {
			double[][][] inputData = this.input;
			double[][] train = inputData[0];
			double[] target = inputData[1][0];
			double[][] normTrain = DataModification.normalizeData(train, this.hyperParameters);
			double[] normTarget = DataModification.normalizeData(train, target, this.hyperParameters);
			double[][][] temp1 = new double[2][][];
			double[][] temp2 = new double[1][];
			temp2[0] = normTarget;
			temp1[0] = normTrain;
			temp1[1] = temp2;
			return temp1;
			
		}
	}

	private class Normalize2Dim implements Stage<double[][], double[][]> {

		private HyperParameters hyperParameters;
		double[][] input;

		public Normalize2Dim(HyperParameters hype,double[][]data) {
			this.hyperParameters = hype;
			this.input=data;
		}

		@Override
		public double[][] execute(double[][]in) {
			double[][] inputData = this.input;
			double[][] normdata = DataModification.normalizeData(inputData, this.hyperParameters);
			return normdata;

		}
	}

	private class Normalize1Dim implements Stage<double[], double[]> {
		private HyperParameters hyperParameters;
		double[] input;

		public Normalize1Dim(HyperParameters hype,double[] data) {
			this.hyperParameters = hype;
			this.input= data;
		}

		@Override
		public double[] execute(double[] in) {
			double[] inputData = this.input;
			return DataModification.standardize(inputData, this.hyperParameters);

		}
	}
	
	
}
