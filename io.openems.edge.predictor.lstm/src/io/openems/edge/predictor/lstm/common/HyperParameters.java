package io.openems.edge.predictor.lstm.common;

import java.util.ArrayList;

public class HyperParameters {
	private double learningRateUpperLimit = 0.0001;
	private double learnignRateLowerLimit = 0.0005;
	private double dataSplitTrain = 0.7;
	private double dataSplitValidate = 0.2;
	private double wiInit = 1;
	private double woInit = 1;
	private double wzInit = 1;
	private double riInit = -1;
	private double roInit = -1;
	private double rzInit = -1;
	private double ytInit = 0;
	private double ctInit = 0;
	private double wfInit = -1;
	private double rfInit = -1;
	private int interval = 5;
	private int epoch = 24;
	private int trendPoints = 1;
	private int windowSizeSeasonality = 7;
	private int windowSizeTrend = 4;
	private int gdIterration = 100;
	private int count = 0;
	private double scalingMin = -5000;
	private double scalingMax = 5000;
	private boolean trendTrainFlag = false;
	private boolean trainingSeasonality = false;
	private boolean trainingTrend = false;
	// private String modleSuffix = "";

	public void setLearningRateUpperLimit(double rate) {
		this.learningRateUpperLimit = rate;
	}

	public double getLearningRateUpperLimit() {
		return this.learningRateUpperLimit;
	}

	public void setLearningLowerLimit(double val) {
		this.learnignRateLowerLimit = val;
	}

	public double getLearningRateLowerLimit() {
		return this.learnignRateLowerLimit;
	}

	public void setWiInit(double val) {
		this.wiInit = val;
	}

	public double getWiInit() {
		return this.wiInit;

	}

	public void setWoInit(double val) {
		this.woInit = val;
	}

	public double getWoInit() {
		return this.woInit;

	}

	public void setWzInit(double val) {
		this.wzInit = val;
	}

	public double getWzInit() {
		return this.wzInit;

	}

	public void setWfInit(double val) {
		this.wfInit = val;
	}

	public double getWfInit() {
		return this.wfInit;
	}

	public void setriInit(double rate) {
		this.riInit = rate;
	}

	public double getRiInit() {
		return this.riInit;

	}

	public void setRoInit(double val) {
		this.roInit = val;
	}

	public double getRoInit() {
		return this.roInit;

	}

	public void setRzInit(double val) {
		this.rzInit = val;
	}

	public double getRzInit() {
		return this.rzInit;

	}

	public double getRfInit() {
		return this.rfInit;

	}

	public void setRfInit(double val) {
		this.rfInit = val;
	}

	public void setYtInit(double val) {
		this.ytInit = val;
	}

	public double getYtInit() {
		return this.ytInit;

	}

	public void setCtInit(double val) {
		this.ctInit = val;
	}

	public double getCtInit() {
		return this.ctInit;

	}

	public int getWindowSizeSeasonality() {
		return this.windowSizeSeasonality;

	}

	public boolean getTrainTrendFlag() {
		return this.trendTrainFlag;

	}

	public boolean setTrainTrendFlag(boolean val) {
		return this.trendTrainFlag = val;
	}

	public int getGdIterration() {
		return this.gdIterration;

	}

	public void setGdIterration(int val) {
		this.gdIterration = val;
	}

	public int getWindowSizeTrend() {
		return this.windowSizeTrend;

	}

	public double getScalingMin() {
		return this.scalingMin;
	}

	public void setScalingMin(double val) {
		this.scalingMin = val;
	}

	public double getScalingMax() {
		return this.scalingMax;
	}

	public void setScalingMax(double val) {
		this.scalingMax = val;
	}

	public void setCount(int val) {
		this.count = val;
	}

	public int getCount() {
		return this.count;
	}

	public void setDatasplitTrain(double val) {
		this.dataSplitTrain = val;
	}

	public double getDataSplitTrain() {
		return this.dataSplitTrain;
	}

	public void setDatasplitValidate(double val) {
		this.dataSplitValidate = val;
	}

	public double getDataSplitValidate() {
		return this.dataSplitValidate;
	}

	public int getTrendPoint() {
		return this.trendPoints;

	}

	public int getEpoch() {

		return this.epoch;
	}

	public void setEpoch(int val) {
		this.epoch = val;
	}

	public int getInterval() {
		return this.interval;
	}

	public void setInterval(int val) {
		this.interval = val;
	}

	public boolean getTrainStatusSeasinality() {
		return this.trainingSeasonality;
	}

	public boolean setTrainStatusSeasonality(boolean val) {
		return this.trainingSeasonality = val;
	}

	public boolean getTrainStatusTrend() {
		return this.trainingTrend;
	}

	public boolean setTrainStatusTrend(boolean val) {
		return this.trainingTrend = val;
	}

	// public String getModleSuffix() {
	// return this.modleSuffix;
	//
	// }
	//
	// public void setModleSuffix(String val) {
	// this.modleSuffix = val;
	// }

	/**
	 * Prints the hyperParameters.
	 */

	public void printHyperParameters() {

		System.out.println("learningRateUpperLimit = " + this.learningRateUpperLimit);
		System.out.println("learnignRateLowerLimit = " + this.learnignRateLowerLimit);
		System.out.println("wiInit = " + this.wiInit);
		System.out.println("woInit = " + this.woInit);
		System.out.println("wzInit = " + this.wzInit);
		System.out.println("riInit = " + this.riInit);
		System.out.println("roInit = " + this.roInit);
		System.out.println("rzInit = " + this.rzInit);
		System.out.println("ytInit = " + this.ytInit);
		System.out.println("ctInit = " + this.ctInit);
		System.out.println("windowSizeSeasonality = " + this.windowSizeSeasonality);
		System.out.println("windowSizeTrend = " + this.windowSizeTrend);
		System.out.println("scalingMin = " + this.scalingMin);
		System.out.println("scalingMax = " + this.scalingMax);

	}

	/**
	 * iMPLEMENTATION OF METHOD.
	 * 
	 * @param data Training Data.
	 * @return 0
	 */

	public double getMin(ArrayList<Double> data) {
		// TODO Auto-generated method stub
		return 0;
	}
}
