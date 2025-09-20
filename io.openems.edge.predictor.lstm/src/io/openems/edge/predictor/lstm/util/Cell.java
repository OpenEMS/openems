package io.openems.edge.predictor.lstm.util;

import java.util.Random;

import io.openems.edge.predictor.lstm.utilities.MathUtils;

public class Cell {

	private double error;
	private double wI;
	private double wO;
	private double wZ;

	private double rI;
	private double rO;
	private double rZ;

	private double yT;
	private double ytMinusOne;

	private double cT;
	private double ctMinusOne;
	private double oT;
	private double zT;

	private double iT;
	private double dlByDy;
	private double dlByDo;
	private double dlByDc;
	private double dlByDi;
	private double dlByDz;
	private double delI;
	private double delO;
	private double delZ;

	private double xT;
	private double outputDataLoc;

	private double delF;

	public Cell(double xt, double outputData) {
		this(xt, outputData, 1, 1, 1, 1, 1, 1, 0);
	}

	public Cell(double xt, double outputData, double wI, double wO, double wZ, double rI, double rO, double rZ,
			double yT) {
		this.dlByDc = 0;
		this.error = 0;
		this.wI = wI;
		this.wO = wO;
		this.wZ = wZ;
		this.rI = rI;
		this.rO = rO;
		this.rZ = rZ;
		this.cT = 0;
		this.oT = 0;
		this.zT = 0;
		this.yT = 0;
		this.ytMinusOne = 0;
		this.ctMinusOne = 0;
		this.ytMinusOne = this.yT;
		this.dlByDy = 0;
		this.dlByDo = 0;
		this.dlByDc = 0;
		this.dlByDi = 0;
		this.dlByDz = 0;
		this.delI = 0;
		this.delO = 0;
		this.delZ = 0;
		this.iT = 0;
		this.xT = xt;
		this.outputDataLoc = outputData;
	}

	/**
	 * Forward propagation.
	 */
	public void forwardPropogation() {
		double dropOutProb;
		boolean decissionFlag = this.decisionDropout();
		if (decissionFlag) {
			dropOutProb = 0.0;
			this.iT = MathUtils.sigmoid(this.wI * this.xT + this.rI * this.ytMinusOne);
			this.oT = MathUtils.sigmoid(this.wO * this.xT + this.rO * this.ytMinusOne);
			this.zT = MathUtils.tanh(this.wZ * this.xT + this.rZ * this.ytMinusOne);
			this.cT = this.ctMinusOne + this.iT * this.zT * dropOutProb;
			this.yT = this.ytMinusOne * (1 - dropOutProb) + this.oT * MathUtils.tanh(this.cT) * dropOutProb;
			this.error = Math.abs(this.yT - this.outputDataLoc) / Math.sqrt(2);
		} else {
			// When not dropping out, apply the scale factor to maintain expected output magnitude
			this.iT = MathUtils.sigmoid(this.wI * this.xT + this.rI * this.ytMinusOne);
			this.oT = MathUtils.sigmoid(this.wO * this.xT + this.rO * this.ytMinusOne);
			this.zT = MathUtils.tanh(this.wZ * this.xT + this.rZ * this.ytMinusOne);
			this.cT = this.ctMinusOne + this.iT * this.zT;
			// Apply dropoutScale to scale the output during training
			this.yT = this.oT * MathUtils.tanh(this.cT) * this.dropoutScale;
			this.error = Math.abs(this.yT - this.outputDataLoc) / Math.sqrt(2);
		}
	}

	/**
	 * Backward propagation.
	 */
	public void backwardPropogation() {
		this.dlByDy = this.error;
		this.dlByDo = this.dlByDy * MathUtils.tanh(this.cT);
		this.dlByDc = this.dlByDy * this.oT * MathUtils.tanhDerivative(this.cT) + this.dlByDc;
		this.dlByDi = this.dlByDc * this.zT;
		this.dlByDz = this.dlByDc * this.iT;
		this.delI = this.dlByDi * MathUtils.sigmoidDerivative(this.wI * this.xT + this.rI * this.ytMinusOne);
		this.delO = this.dlByDo * MathUtils.sigmoidDerivative(this.wO * this.xT + this.rO * this.ytMinusOne);
		this.delZ = this.dlByDz * MathUtils.tanhDerivative(this.wZ * this.xT + this.rZ * this.ytMinusOne);
	}

	// Dropout regularization properties
	private boolean dropoutEnabled = false;
	private double dropoutRate = 0.3; // Default: drop 30% of connections
	private double dropoutScale = 1.0;
	
	/**
	 * Enable or disable dropout regularization.
	 * 
	 * @param enabled true to enable dropout, false to disable
	 */
	public void setDropoutEnabled(boolean enabled) {
		this.dropoutEnabled = enabled;
	}
	
	/**
	 * Set the dropout rate (percentage of connections to randomly zero out).
	 * 
	 * @param rate dropout rate between 0.0 and 1.0
	 */
	public void setDropoutRate(double rate) {
		this.dropoutRate = rate;
	}
	
	/**
	 * Set the scale factor to apply to remaining connections after dropout.
	 * 
	 * @param scale scale factor to maintain expected output magnitude
	 */
	public void setDropoutScale(double scale) {
		this.dropoutScale = scale;
	}
	
	/**
	 * Generates a random decision with dropout probability. This method generates a
	 * random boolean decision with a dropout probability configurable via dropoutRate.
	 * It uses a random number generator to determine whether the decision is true or false.
	 * 
	 * <p>
	 * When dropout is enabled, the probability of returning true is dropoutRate,
	 * and the probability of returning false is (1-dropoutRate).
	 * 
	 * <p>
	 * When dropout is disabled, it always returns false (no dropout applied).
	 * 
	 * @return true if the connection should be dropped, false otherwise
	 */
	public boolean decisionDropout() {
		if (!this.dropoutEnabled) {
			return false;
		}
		
		Random random = new Random();
		double randomValue = random.nextDouble();
		return randomValue < this.dropoutRate;
	}

	public double getError() {
		return this.error;
	}

	public void setError(double error) {
		this.error = error;
	}

	public double getWi() {
		return this.wI;
	}

	public void setWi(double wi) {
		this.wI = wi;
	}

	public double getWo() {
		return this.wO;
	}

	public void setWo(double wo) {
		this.wO = wo;
	}

	public double getWz() {
		return this.wZ;
	}

	public void setWz(double wz) {
		this.wZ = wz;
	}

	public double getRi() {
		return this.rI;
	}

	public void setRi(double ri) {
		this.rI = ri;
	}

	public double getRo() {
		return this.rO;
	}

	public void setRo(double ro) {
		this.rO = ro;
	}

	public double getRz() {
		return this.rZ;
	}

	public void setRz(double rz) {
		this.rZ = rz;
	}

	public double getCt() {
		return this.cT;
	}

	public void setCt(double ct) {
		this.cT = ct;
	}

	public double getCtMinusOne() {
		return this.ctMinusOne;
	}

	public void setCtMinusOne(double ct) {
		this.ctMinusOne = ct;
	}

	public double getYtMinusOne() {
		return this.ytMinusOne;
	}

	public void setYtMinusOne(double yt) {
		this.ytMinusOne = yt;
	}

	public double getYt() {
		return this.yT;
	}

	public void setYt(double yt) {
		this.yT = yt;
	}

	public void setIt(double iT) {
		this.iT = iT;
	}

	public double getIt() {
		return this.iT;
	}

	public double getOt() {
		return this.oT;
	}

	public double getZt() {
		return this.zT;
	}

	public void setDlByDy(double dlByDy) {
		this.dlByDy = dlByDy;
	}

	public double getDlByDy() {
		return this.dlByDy;
	}

	public void setDlByDo(double dlByDo) {
		this.dlByDo = dlByDo;
	}

	public double getDlByDo() {
		return this.dlByDo;
	}

	public void setDlByDc(double dlByDc) {
		this.dlByDc = dlByDc;
	}

	public double getDlByDc() {

		return this.dlByDc;
	}

	public void setDlByDi(double dlByDi) {
		this.dlByDi = dlByDi;
	}

	public double getDlByDi() {
		return this.dlByDi;
	}

	public void setDlByDz(double dlByDz) {
		this.dlByDz = dlByDz;
	}

	public double getDlByDz() {
		return this.dlByDz;
	}

	public void setDelI(double delI) {
		this.delI = delI;
	}

	public double getDelI() {
		return this.delI;
	}

	public void setDelO(double delO) {
		this.delO = delO;
	}

	public double getDelF() {
		return this.delF;
	}

	public void setDelF(double delF) {
		this.delF = delF;
	}

	public double getDelO() {
		return this.delO;
	}

	public void setDelZ(double delZ) {
		this.delZ = delZ;
	}

	public double getDelZ() {
		return this.delZ;
	}

	public void setXt(double xt) {
		this.xT = xt;
	}

	public double getXt() {
		return this.xT;
	}
}
