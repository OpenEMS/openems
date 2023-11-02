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

	private double cT;
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

	public enum PropagationType {
		FORWARD, BACKWARD
	}

	public Cell(double xt, double outputData) {
		this.dlByDc = 0;
		this.error = 0;
		this.wI = 1;
		this.wO = 1;
		this.wZ = 1;
		this.rI = 1;
		this.rO = 1;
		this.rZ = 1;
		this.cT = 0;
		this.oT = 0;
		this.zT = 0;
		this.yT = 0;
		this.dlByDy = this.dlByDo = this.dlByDc = this.dlByDi = this.dlByDz = 0;
		this.delI = this.delO = this.delZ = 0;
		this.iT = 0;
		this.xT = xt;

		this.outputDataLoc = outputData;

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
		this.yT = yT;
		this.dlByDy = this.dlByDo = this.dlByDc = this.dlByDi = this.dlByDz = 0;
		this.delI = this.delO = this.delZ = 0;
		this.iT = 0;
		this.xT = xt;
		this.outputDataLoc = outputData;

	}

	/**
	 * Forward propagation.
	 */
	public void forwardPropogation() {

		double dropOutProb;
		// System.out.println(decisionDropout());
		boolean decissionFlag = this.decisionDropout();

		if (decissionFlag == true) {
			dropOutProb = 0.02;
			this.iT = MathUtils.sigmoid(this.wI * this.xT + this.rI * this.yT);
			this.oT = MathUtils.sigmoid(this.wO * this.xT + this.rO * this.yT);
			this.zT = MathUtils.tanh(this.wZ * this.xT + this.rZ * this.yT);
			this.cT = this.cT + this.iT * this.zT * dropOutProb;
			this.yT = this.yT * (1 - dropOutProb) + this.oT * MathUtils.tanh(this.cT) * dropOutProb;
			this.error = this.yT - this.outputDataLoc;
		} else {
			this.iT = MathUtils.sigmoid(this.wI * this.xT + this.rI * this.yT);
			this.oT = MathUtils.sigmoid(this.wO * this.xT + this.rO * this.yT);
			this.zT = MathUtils.tanh(this.wZ * this.xT + this.rZ * this.yT);
			this.cT = this.cT + this.iT * this.zT;
			this.yT = this.oT * MathUtils.tanh(this.cT);
			this.error = this.yT - this.outputDataLoc;
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
		this.delI = this.dlByDi * MathUtils.sigmoidDerivative(this.wI + this.rI * this.yT);
		this.delO = this.dlByDo * MathUtils.sigmoidDerivative(this.wO + this.rO * this.yT);
		this.delZ = this.dlByDz * MathUtils.tanhDerivative(this.wZ + this.rZ * this.yT);
	}

	/**
	 * Generates a random decision with dropout probability. This method generates a
	 * random boolean decision with a dropout probability of 10%. It uses a random
	 * number generator to determine whether the decision is true or false. The
	 * probability of returning true is 10%, and the probability of returning false
	 * is 90%.
	 *
	 * @return true with a 10% probability, false with a 90% probability.
	 */

	public boolean decisionDropout() {

		Random random = new Random();
		int randomNumber = random.nextInt(10) + 1;
		if (randomNumber > 9) {
			return true;
		}
		return false;
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

	/**
	 * Simple to string method.
	 * 
	 * @param propgationType forward or backward propagation
	 * @return result string of the all the values
	 */
	public String toString(PropagationType propgationType) {
		StringBuilder str = new StringBuilder();
		switch (propgationType) {
		case BACKWARD:
			str.append("dlByDy : " + this.dlByDy + " | ");
			str.append("dlByDo : " + this.dlByDo + " | ");
			str.append("dlByDc : " + this.dlByDc + " | ");
			str.append("dlByDi : " + this.dlByDi + " | ");
			str.append("dlByDz : " + this.dlByDz + " | ");
			str.append("delI : " + this.delI + " | ");
			str.append("delO : " + this.delO + " | ");
			str.append("delZ : " + this.delZ + " | ");
			break;
		case FORWARD:
			str.append("xt :" + this.xT + " | ");
			str.append("ot :" + this.oT + " | ");
			str.append("zt :" + this.zT + " | ");
			str.append("ct :" + this.cT + " | ");
			str.append("yt :" + this.yT + " | ");
			break;
		}
		return str.toString();
	}

}
