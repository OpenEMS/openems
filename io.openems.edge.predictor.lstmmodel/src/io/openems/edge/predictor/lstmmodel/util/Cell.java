package io.openems.edge.predictor.lstmmodel.util;

import java.util.function.BiFunction;
import java.util.function.Function;

import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;

public class Cell {

	public static final BiFunction<Double, Double, Double> MULTIPLY = (x, y) -> x * y;
	public static final BiFunction<Double, Double, Double> ADD = (x, y) -> x + y;
	public static final Function<Double, Double> SIGMOID = MathUtils::sigmoid;

	private double error;
	private double wI;
	private double wO;
	private double wZ;
	private double rI;
	private double rO;
	private double rZ;
	public double yT;

	public double cT;
	public double oT;
	public double zT;

	public double iT;
	public double dlByDy;
	public double dlByDo;
	public double dlByDc;
	public double dlByDi;
	public double dlByDz;
	public double delI;
	public double delO;
	public double delZ;

	public double xT;
	public double outputDataLoc;

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
		this.iT = MathUtils.sigmoid(this.wI * this.xT + this.rI * this.yT);
		this.oT = MathUtils.sigmoid(this.wO * this.xT + this.rO * this.yT);
		this.zT = MathUtils.tanh(this.wZ * this.xT + this.rZ * this.yT);
		this.cT = this.cT + this.iT * this.zT;
		this.yT = this.oT * MathUtils.tanh(this.cT);
		this.error = this.yT - this.outputDataLoc;

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
