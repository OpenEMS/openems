package io.openems.edge.predictor.lstmmodel.util;

import static org.junit.Assert.assertEquals;

//import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;

public class CellTest {
	private double wi = 1;
	private double wo = 1;
	private double wz = 1;
	private double ri = 1;
	private double ro = 1;
	private double rz = 1;
	private double yt = 1;
	private double input = 1;
	private double target = 1;
	private double ctMinusOne = 1;
	private double ytMinusOne = 1;

	private double dropOutProb = 0.02; // do not change this
	private Cell obj = new Cell(this.input, this.target, this.wi, this.wo, this.wz, this.ri, this.ro, this.rz, this.yt);

	/**
	 * forwardPropagationTest.
	 */
	// @Test
	public void forwardPropagationTest() {
		this.obj.setCtMinusOne(this.ctMinusOne);
		this.obj.setYtMinusOne(this.ytMinusOne);
		this.obj.forwardPropogation();

		double itExpected = MathUtils.sigmoid(this.wi * this.input + this.ri * this.ytMinusOne);
		double otExpected = MathUtils.sigmoid(this.wo * this.input + this.ro * this.ytMinusOne);
		double ztExpected = MathUtils.tanh(this.wz * this.input + this.rz * this.ytMinusOne);

		// computation without drop out

		double ctExpectedWithoutDropout = this.ctMinusOne + itExpected * ztExpected;
		double ytExpectedWithoutDropout = otExpected * MathUtils.tanh(ctExpectedWithoutDropout);
		final double errorExpectedWithoutDropout = ytExpectedWithoutDropout - this.target;

		// computation with drop out
		double ctExpectedWithDropout = this.ctMinusOne + itExpected * ztExpected * this.dropOutProb;
		double ytExpectedWithDropout = this.ytMinusOne * (1 - this.dropOutProb)
				+ otExpected * MathUtils.tanh(ctExpectedWithDropout) * (this.dropOutProb);
		final double errorExpectedWithDropout = ytExpectedWithDropout - this.target;

		assertEquals(itExpected, this.obj.getIt(), 0.00001);
		assertEquals(otExpected, this.obj.getOt(), 0.00001);
		assertEquals(ztExpected, this.obj.getZt(), 0.00001);

		boolean matchCheckcT = ctExpectedWithoutDropout == this.obj.getCt()
				|| ctExpectedWithDropout == this.obj.getCt();
		boolean matchCheckyT = ytExpectedWithoutDropout == this.obj.getYt()
				|| ytExpectedWithDropout == this.obj.getYt();
		boolean matchCheckerror = errorExpectedWithoutDropout == this.obj.getError()
				|| errorExpectedWithDropout == this.obj.getError();
		assert (matchCheckcT && matchCheckyT && matchCheckerror);

	}

	/**
	 * Backpropagation.
	 */
	// @Test
	public void backwardPropogationTest() {

		this.obj.setCtMinusOne(this.ctMinusOne);
		this.obj.setYtMinusOne(this.ytMinusOne);
		this.obj.setDlByDc(0.0);

		this.obj.forwardPropogation();
		this.obj.backwardPropogation();
		// common calculation
		double itExpected = MathUtils.sigmoid(this.wi * this.input + this.ri * this.ytMinusOne);
		double otExpected = MathUtils.sigmoid(this.wo * this.input + this.ro * this.ytMinusOne);
		double ztExpected = MathUtils.tanh(this.wz * this.input + this.rz * this.ytMinusOne);

		// without drop out
		// Forward propagation
		double ctExpectedWithoutDropout = this.ctMinusOne + itExpected * ztExpected;
		double ytExpectedWithoutDropout = otExpected * MathUtils.tanh(ctExpectedWithoutDropout);
		double errorExpectedWithoutDropout = ytExpectedWithoutDropout - this.target;
		// backward propagation
		double dlByDyWithoutDropout = errorExpectedWithoutDropout;

		double dlByDoWithoutDropout = dlByDyWithoutDropout * MathUtils.tanh(ctExpectedWithoutDropout);
		double dlByDcWithoutDropout = 0;
		double dlByDcWithDropout = 0;

		dlByDcWithoutDropout = dlByDyWithoutDropout * otExpected * MathUtils.tanhDerivative(ctExpectedWithoutDropout)
				+ dlByDcWithoutDropout;

		double dlByDiWithoutDropout = dlByDcWithoutDropout * ztExpected;
		double dlByDzWithoutDropout = dlByDcWithoutDropout * itExpected;
		double delIWithoutDropout = dlByDiWithoutDropout
				* MathUtils.sigmoidDerivative(this.wi * this.input + this.ri * this.ytMinusOne);
		double delOWithoutDropout = dlByDoWithoutDropout
				* MathUtils.sigmoidDerivative(this.wo * this.input + this.ro * this.ytMinusOne);
		double delZWithoutDropout = dlByDzWithoutDropout
				* MathUtils.tanhDerivative(this.wz * this.input + this.rz * this.ytMinusOne);

		// computation with drop out
		// Forwar Propagatin

		double ctExpectedWithDropout = this.ctMinusOne + itExpected * ztExpected * this.dropOutProb;
		double ytExpectedWithDropout = this.ytMinusOne * (1 - this.dropOutProb)
				+ otExpected * MathUtils.tanh(ctExpectedWithDropout) * (this.dropOutProb);
		double errorExpectedWithDropout = ytExpectedWithDropout - this.target;
		// backward propagation
		double dlByDyWithDropout = errorExpectedWithDropout;

		double dlByDoWithDropout = dlByDyWithDropout * MathUtils.tanh(ctExpectedWithDropout);

		dlByDcWithDropout = dlByDyWithDropout * otExpected * MathUtils.tanhDerivative(ctExpectedWithDropout)
				+ dlByDcWithDropout;

		double dlByDiWithDropout = dlByDcWithDropout * ztExpected;
		double dlByDzWithDropout = dlByDcWithDropout * itExpected;
		double delIWithDropout = dlByDiWithDropout
				* MathUtils.sigmoidDerivative(this.wi * this.input + this.ri * this.ytMinusOne);
		double delOWithDropout = dlByDoWithDropout
				* MathUtils.sigmoidDerivative(this.wo * this.input + this.ro * this.ytMinusOne);
		double delZWithDropout = dlByDzWithDropout
				* MathUtils.tanhDerivative(this.wz * this.input + this.rz * this.ytMinusOne);

		boolean dlByDydecission = this.obj.getDlByDy() == dlByDyWithDropout
				|| this.obj.getDlByDy() == dlByDyWithoutDropout;
		boolean dlByDodecission = this.obj.getDlByDo() == dlByDoWithDropout
				|| this.obj.getDlByDo() == dlByDoWithoutDropout;

		boolean dlByDcdecission = this.obj.getDlByDc() == dlByDcWithDropout
				|| this.obj.getDlByDc() == dlByDcWithoutDropout;
		boolean dlByDidecission = this.obj.getDlByDi() == dlByDiWithDropout
				|| this.obj.getDlByDi() == dlByDiWithoutDropout;
		boolean dlByDzdecission = this.obj.getDlByDz() == dlByDzWithDropout
				|| this.obj.getDlByDz() == dlByDzWithoutDropout;

		boolean delIdecission = this.obj.getDelI() == delIWithDropout || this.obj.getDelI() == delIWithoutDropout;
		boolean delOdecission = this.obj.getDelO() == delOWithDropout || this.obj.getDelO() == delOWithoutDropout;
		boolean delZdecission = this.obj.getDelZ() == delZWithDropout || this.obj.getDelZ() == delZWithoutDropout;

		assert (dlByDydecission && dlByDodecission && dlByDcdecission && dlByDidecission && dlByDzdecission
				&& delIdecission && delOdecission && delZdecission);

	}

}
