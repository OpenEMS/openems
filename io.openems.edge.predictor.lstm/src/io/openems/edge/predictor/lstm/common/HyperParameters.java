package io.openems.edge.predictor.lstm.common;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;

public class HyperParameters implements Serializable {

	private static final long serialVersionUID = 1L;
	private double learningRateUpperLimit = 0.1;
	private double learnignRateLowerLimit = 0.05;
	private double dataSplitTrain = 0.7;
	private double dataSplitValidate = 0.2;

	private double wiInit = 1;
	private double woInit = 1;
	private double wzInit = 1;
	private double riInit = -1;
	private double roInit = -1;
	private double rzInit = -1;
	private double ytInit = 0;// do not change; default value = 0;
	private double ctInit = 0;// do not change; default value = 0
	private double wfInit = -1;
	private double rfInit = -1;

	private int interval = 5;
	private int batchSize = 1;
	private int batchTrack = 0;
	private int epoch = 5;
	private int epochTrack = 0;
	private int trendPoints = 1;
	private int windowSizeSeasonality = 7;// do not change on fly
	private int windowSizeTrend = 4; // do not change on fly
	private int gdIterration = 1;
	private int count = 0;

	private double scalingMin = -5000;
	private double scalingMax = 5000;
	private boolean trendTrainFlag = false;
	private boolean trainingSeasonality = false;
	private boolean trainingTrend = false;
	private ArrayList<Double> rmsErrorTrend = new ArrayList<Double>();
	private ArrayList<Double> rmsErrorSeasonality = new ArrayList<Double>();
	private OffsetDateTime lastTrainedDate = null;
	private static HyperParameters instance;
	private int outerLoopCount = 0;

	private HyperParameters() {

	}

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

	public void setRmsErrorTrend(double val) {
		this.rmsErrorTrend.add(val);

	}

	public void setRmsErrorSeasonality(double val) {
		this.rmsErrorSeasonality.add(val);

	}

	public ArrayList<Double> getRmsErrorSeasonality() {
		return this.rmsErrorSeasonality;
	}

	public ArrayList<Double> getRmsErrorTrend() {
		return this.rmsErrorTrend;
	}

	public void setEpochTrack(int val) {
		this.epochTrack = val;

	}

	public int getEpochTrack() {
		return this.epochTrack;
	}

	public int getMinimumErrorModelSeasonality() {
		return this.rmsErrorSeasonality.indexOf(Collections.min(this.rmsErrorSeasonality));
	}

	public int getMinimumErrorModelTrend() {

		return this.rmsErrorTrend.indexOf(Collections.min(this.rmsErrorTrend));
	}

	public void setLastTrainedDateTime(OffsetDateTime val) {
		this.lastTrainedDate = val;

	}

	public OffsetDateTime getLastTrainedDateTime() {
		return this.lastTrainedDate;
	}

	/**
	 * Gets the singleton instance of the HyperParameters class.
	 *
	 * <p>
	 * The method ensures that only one instance of the HyperParameters class is
	 * created. If the instance is null, a new instance is created; otherwise, the
	 * existing instance is returned.
	 * </p>
	 *
	 * @return The singleton instance of the HyperParameters class.
	 */

	public static HyperParameters getInstance() {
		if (instance == null) {
			instance = new HyperParameters();
		}
		return instance;
	}

	public int getOuterLoopCount() {
		return this.outerLoopCount;
	}

	public void setOuterLoopCount(int val) {
		this.outerLoopCount = val;
	}

	public int getBatchSize() {
		return this.batchSize;
	}

	public int getBatchTrack() {
		return this.batchTrack;
	}

	public void setBatchTrack(int val) {
		this.batchTrack = val;

	}
	
	/**
	 * Prints the current values of hyperparameters and related attributes to the console.
	 *
	 * 
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
		System.out.println("Epoch = " + this.epoch);
		System.out.println("windowSizeSeasonality = " + this.windowSizeSeasonality);
		System.out.println("windowSizeTrend = " + this.windowSizeTrend);
		System.out.println("scalingMin = " + this.scalingMin);
		System.out.println("scalingMax = " + this.scalingMax);
		System.out.println("RMS error trend = " + this.getRmsErrorTrend());
		System.out.println("RMS error Seasonlality =" + this.getRmsErrorSeasonality());
		System.out.println("Count value = " + this.count);
		System.out.println("Outer loop Count  = " + this.outerLoopCount);
		System.out.println("Epoch track = " + this.epochTrack);

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
