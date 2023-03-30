package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Preprocessing {

	ArrayList<ArrayList<Double>> TrainData;
	ArrayList<ArrayList<Double>> ValidateData;
	ArrayList<ArrayList<Double>> TestData;

	public double[][] TrainData1;
	public double[][] ValidateData1;
	public double[][] TestData1;

	public double[] TrainTarget1;
	public double[] ValidateTarget1;
	public double[] TestTarget1;

	ArrayList<Double> TrainTarget;
	ArrayList<Double> ValidateTarget;
	ArrayList<Double> TestTarget;

	ArrayList<Double> data;
	ArrayList<Double> ScaledData;
	int LenTrain = 0;
	int LenValidate = 0;
	int window = 0;

//	public Preprocessing() {
//
//		GetData alldata=new GetData();
//		this.data=(ArrayList<Float>) alldata.data;
//		this.ScaledData=new ArrayList<Double>();
//		this.TrainData=new ArrayList<ArrayList<Double>>();
//		this.ValidateData=new ArrayList<ArrayList<Double>>();
//		this.TestData=new ArrayList<ArrayList<Double>>();
//		this.TrainTarget=new ArrayList<Double>();
//		this.ValidateTarget=new ArrayList<Double>();
//		this.TestTarget=new ArrayList<Double>();
//		//System.out.print((int)(0.6*this.data.size()));
//		LenTrain=(int)(0.6*this.data.size());
//		this.LenValidate=this.LenTrain+(int)(0.2*this.data.size());
//		this.window=100;
//		scale();
//		
//		TrainTestValidateData(0,this.LenTrain,this.window,-1);
//		TrainTestValidateTarget(0,this.LenTrain,this.window,-1);
//		TrainTestValidateData(this.LenTrain,this.LenValidate,this.window,0);
//		TrainTestValidateTarget(this.LenTrain,this.LenValidate,this.window,0);
//		TrainTestValidateData(this.LenTrain,this.LenValidate,this.window,1);
//		TrainTestValidateTarget(this.LenTrain,this.LenValidate,this.window,1);
//		//System.out.println(this.TestData);
//		convertdata(-1);
//		convertdata(0);
//		convertdata(1);
//		converttarget(-1);
//		converttarget(0);
//		converttarget(1);
//	}

	public Preprocessing(List<Double> data, int windowSize) {

		this.data = (ArrayList<Double>) data;
		this.ScaledData = new ArrayList<Double>();
		this.TrainData = new ArrayList<ArrayList<Double>>();
		this.ValidateData = new ArrayList<ArrayList<Double>>();
		this.TestData = new ArrayList<ArrayList<Double>>();
		this.TrainTarget = new ArrayList<Double>();
		this.ValidateTarget = new ArrayList<Double>();
		this.TestTarget = new ArrayList<Double>();
		// System.out.print((int)(0.6*this.data.size()));
		LenTrain = (int) (0.6 * this.data.size());
		this.LenValidate = this.LenTrain + (int) (0.2 * this.data.size());
		this.window = windowSize;
		scale();

		TrainTestValidateData(0, this.LenTrain, this.window, -1);
		TrainTestValidateTarget(0, this.LenTrain, this.window, -1);
		TrainTestValidateData(this.LenTrain, this.LenValidate, this.window, 0);
		TrainTestValidateTarget(this.LenTrain, this.LenValidate, this.window, 0);
		TrainTestValidateData(this.LenTrain, this.LenValidate, this.window, 1);
		TrainTestValidateTarget(this.LenTrain, this.LenValidate, this.window, 1);
		// System.out.println(this.TestData);
		convertdata(-1);
		convertdata(0);
		convertdata(1);
		converttarget(-1);
		converttarget(0);
		converttarget(1);
	}

	public void scale() {
		double max = Collections.max(this.data);
		double min=Collections.min(this.data);
		double minScaled=0.2;
		double maxScaled=0.8;
		for (int i = 0; i < this.data.size(); i++) {
			double temp=((this.data.get(i)- min)/max)*(maxScaled-minScaled);
			this.ScaledData.add(temp+minScaled);
		}
	}

	public void TrainTestValidateTarget(int lower, int upper, int window, int a) {
		for (int i = lower; i < (upper - window); i++) {
			if (a == -1) {
				TrainTarget.add(this.ScaledData.get(i + window));
			} else if (a == 0) {
				ValidateTarget.add((double) this.ScaledData.get(i + window));
			} else if (a == 1) {
				TestTarget.add((double) this.ScaledData.get(i + window));
			} else {
				System.out.println(
						"If you are seeing this, there is an error in TrainTestValidateTarget method of Preprocessing Class");
			}
		}

	}

	// pass -1 to Get TrainData, 0 to get
	// validate data and 0 1 o get test
	// data, also pass the index of the data
	public void TrainTestValidateData(int lower, int upper, int window, int a) {

		for (int i = lower; i < upper - window; i++) {
			ArrayList<Double> temp = new ArrayList();
			for (int j = 0; j < window; j++) {
				double b = this.ScaledData.get(i + j);
				temp.add(b);
				// System.out.println(temp);

			}
			if (a == -1) {
				TrainData.add(temp);
			} else if (a == 0) {
				ValidateData.add(temp);
			} else if (a == 1) {
				TestData.add(temp);
			} else {
				System.out.println(
						"If you are seeing this, there is an error in TrainTestValidate method of Preprocessing Class");
			}
		}
	}

	// converts ArrayList<ArrayList<Double> to double[][], a=-1 is to convert test
	// data,0 is to convert validation data and 1 is to convert test data
	public void convertdata(int a)

	{
		if (a == -1) {
			this.TrainData1 = new double[this.TrainData.size()][this.TrainData.get(0).size()];
			for (int i = 0; i < this.TrainData.size(); i++) {
				for (int j = 0; j < this.TrainData.get(0).size(); j++) {
					this.TrainData1[i][j] = this.TrainData.get(i).get(j);
				}
			}

		}

		else if (a == 0) {
			this.ValidateData1 = new double[this.ValidateData.size()][this.ValidateData.get(0).size()];
			for (int i = 0; i < this.ValidateData.size(); i++) {
				for (int j = 0; j < this.ValidateData.get(0).size(); j++) {
					this.ValidateData1[i][j] = this.ValidateData.get(i).get(j);
				}
			}

		}

		else if (a == 1) {
			this.TestData1 = new double[this.TestData.size()][this.TestData.get(0).size()];
			for (int i = 0; i < this.TestData.size(); i++) {
				for (int j = 0; j < this.TestData.get(0).size(); j++) {
					this.TestData1[i][j] = this.TestData.get(i).get(j);
				}
			}

		}

		else {
			System.out
					.println("If you are seeing this, there is an error in convertdata method of Preprocessing Class");
		}

	}

	public void converttarget(int a)// converts ArrayList<Double> to double[]
	{
		if (a == -1) {
			this.TrainTarget1 = new double[this.TrainTarget.size()];
			for (int i = 0; i < this.TrainData.size(); i++) {

				this.TrainTarget1[i] = this.TrainTarget.get(i);

			}

		}

		else if (a == 0) {
			this.ValidateTarget1 = new double[this.ValidateTarget.size()];
			for (int i = 0; i < this.ValidateTarget.size(); i++) {

				this.ValidateTarget1[i] = this.ValidateTarget.get(i);

			}
		} else if (a == 1) {
			this.TestTarget1 = new double[this.TestTarget.size()];
			for (int i = 0; i < this.TestTarget.size(); i++)

			{

				this.TestTarget1[i] = this.TestTarget.get(i);

			}

		} else {
			System.out
					.println("If you are seeing this, there is an error in convertdata method of Preprocessing Class");
		}

	}

}
