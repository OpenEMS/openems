package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Lstm {

	private double[] inputData;
	private double outputData;
	private double derivativeLWrtRi = 0;
	private double derivativeLWrtRo = 0;
	private double derivativeLWrtRz = 0;
	private double derivativeLWrtWi = 0;
	private double derivativeLWrtWo = 0;
	private double derivativeLWrtWz = 0;
	private double learningRate;
	private int epoch = 100;

	protected ArrayList<Cell> cells;

	public Lstm(LstmBuilder builder) {
		this.inputData = builder.inputData;
		this.outputData = builder.outputData;
		this.learningRate = builder.learningRate;
		this.epoch = builder.epoch;
	}

	/**
	 * Forward propagation.
	 */
	public void forwardprop() {
		try {
			for (int i = 0; i < this.cells.size(); i++) {
				this.cells.get(i).forwardPropogation();
				if (i < this.cells.size() - 1) {
					this.cells.get(i + 1).setYtMinusOne(this.cells.get(i).getYt());
					this.cells.get(i + 1).setCtMinusOne(this.cells.get(i).getCt());
					this.cells.get(i).setError((Math.abs(this.cells.get(i).getYt() - this.cells.get(i + 1).getXt())/Math.sqrt(2)));
				}
			}
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Backward propagation.
	 */
	public void backwardprop() {

		var rate = new AdaptiveLearningRate();

		var localLearningRate1 = 0.;
		var localLearningRate2 = 0.;
		var localLearningRate3 = 0.;
		var localLearningRate4 = 0.;
		var localLearningRate5 = 0.;
		var localLearningRate6 = 0.;

		for (int i = this.cells.size() - 1; i >= 0; i--) {
			if (i < this.cells.size() - 1) {
				this.cells.get(i).setDlByDc(this.cells.get(i + 1).getDlByDc());
			}
			this.cells.get(i).backwardPropogation();
		}

		for (int i = 0; i < this.cells.size(); i++) {
			this.derivativeLWrtRi += this.cells.get(i).getYtMinusOne() * this.cells.get(i).getDelI();
			this.derivativeLWrtRo += this.cells.get(i).getYtMinusOne() * this.cells.get(i).getDelO();
			this.derivativeLWrtRz += this.cells.get(i).getYtMinusOne() * this.cells.get(i).getDelZ();

			this.derivativeLWrtWi += this.cells.get(i).getXt() * this.cells.get(i).getDelI();
			this.derivativeLWrtWo += this.cells.get(i).getXt() * this.cells.get(i).getDelO();
			this.derivativeLWrtWz += this.cells.get(i).getXt() * this.cells.get(i).getDelZ();

			localLearningRate1 = rate.adagradOptimizer(this.learningRate, localLearningRate1, this.derivativeLWrtWi, i);
			localLearningRate2 = rate.adagradOptimizer(this.learningRate, localLearningRate2, this.derivativeLWrtWo, i);
			localLearningRate3 = rate.adagradOptimizer(this.learningRate, localLearningRate3, this.derivativeLWrtWz, i);
			localLearningRate4 = rate.adagradOptimizer(this.learningRate, localLearningRate4, this.derivativeLWrtRi, i);
			localLearningRate5 = rate.adagradOptimizer(this.learningRate, localLearningRate5, this.derivativeLWrtRo, i);
			localLearningRate6 = rate.adagradOptimizer(this.learningRate, localLearningRate6, this.derivativeLWrtRz, i);

			this.cells.get(i).setWi(this.cells.get(i).getWi() - localLearningRate1 * this.derivativeLWrtWi);
			this.cells.get(i).setWo(this.cells.get(i).getWo() - localLearningRate2 * this.derivativeLWrtWo);
			this.cells.get(i).setWz(this.cells.get(i).getWz() - localLearningRate3 * this.derivativeLWrtWz);
			this.cells.get(i).setRi(this.cells.get(i).getRi() - localLearningRate4 * this.derivativeLWrtRi);
			this.cells.get(i).setRo(this.cells.get(i).getRo() - localLearningRate5 * this.derivativeLWrtRo);
			this.cells.get(i).setRz(this.cells.get(i).getRz() - localLearningRate6 * this.derivativeLWrtRz);
		}
	}

	/**
	 * Train to get the weight matrix.
	 * 
	 * @return weight matrix trained weight matrix
	 */
	public ArrayList<ArrayList<Double>> train() {

		MatrixWeight mW = new MatrixWeight();
		for (int i = 0; i < this.epoch; i++) {

			this.forwardprop();
			this.backwardprop();

			var wiList = new ArrayList<Double>();
			var woList = new ArrayList<Double>();
			var wzList = new ArrayList<Double>();
			var riList = new ArrayList<Double>();
			var roList = new ArrayList<Double>();
			var rzList = new ArrayList<Double>();
			var ytList = new ArrayList<Double>();
			var ctList = new ArrayList<Double>();

			for (int j = 0; j < this.cells.size(); j++) {
				wiList.add(this.cells.get(j).getWi()); //
				woList.add(this.cells.get(j).getWo()); //
				wzList.add(this.cells.get(j).getWz()); //
				riList.add(this.cells.get(j).getRi()); //
				roList.add(this.cells.get(j).getRo()); //
				rzList.add(this.cells.get(j).getRz()); //
				ytList.add(this.cells.get(j).getYt()); //
				ctList.add(this.cells.get(j).getCt()); //
			}

			mW.getErrorList().add(this.cells.get(this.cells.size() - 1).getError());
			mW.getWi().add(wiList);
			mW.getWo().add(woList);
			mW.getWz().add(wzList);
			mW.getRi().add(riList);
			mW.getRo().add(roList);
			mW.getRz().add(rzList);
			mW.getOut().add(ytList);
			mW.getCt().add(ctList);
		}

		int globalMinimaIndex = findGlobalMinima(mW.getErrorList());
		
		var returnArray = new ArrayList<ArrayList<Double>>();

		returnArray.add(mW.getWi().get(globalMinimaIndex));
		returnArray.add(mW.getWo().get(globalMinimaIndex));
		returnArray.add(mW.getWz().get(globalMinimaIndex));
		returnArray.add(mW.getRi().get(globalMinimaIndex));
		returnArray.add(mW.getRo().get(globalMinimaIndex));
		returnArray.add(mW.getRz().get(globalMinimaIndex));
		returnArray.add(mW.getOut().get(globalMinimaIndex));
		returnArray.add(mW.getCt().get(globalMinimaIndex));

		return returnArray;
	}

	/**
	 * Get the index of the Global minima. element arr.get(index x) is a local
	 * minimum if it is less than both its neighbors and an arr can have multiple
	 * local minima.
	 * 
	 * @param data {@link java.util.ArrayList} of double
	 * @return index index of the global minima in the data
	 */
	public static int findGlobalMinima(ArrayList<Double> data) {
		return IntStream.range(0, data.size())//
				.boxed()//
				.min(Comparator.comparingDouble(i -> Math.abs(data.get(i))))//
				.orElse(-1);
	}

	public double[] getInputData() {
		return this.inputData;
	}

	public double getOutputData() {
		return this.outputData;
	}

	public double getDerivativeLWrtRi() {
		return this.derivativeLWrtRi;
	}

	public double getDerivativeLWrtRo() {
		return this.derivativeLWrtRo;
	}

	public double getDerivativeLWrtRz() {
		return this.derivativeLWrtRz;
	}

	public double getDerivativeLWrtWi() {
		return this.derivativeLWrtWi;
	}

	public double getDerivativeLWrtWo() {
		return this.derivativeLWrtWo;
	}

	public double getDerivativeLWrtWz() {
		return this.derivativeLWrtWz;
	}

	public double getLearningRate() {
		return this.learningRate;
	}

	public ArrayList<Cell> getCells() {
		return this.cells;
	}

	public synchronized void setWi(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(0).size(); i++) {
			try {
				this.cells.get(i).setWi(val.get(0).get(i));
			} catch (ArithmeticException | IndexOutOfBoundsException e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized void setWo(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(1).size(); i++) {
			this.cells.get(i).setWo(val.get(1).get(i));
		}
	}

	public synchronized void setWz(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(2).size(); i++) {
			this.cells.get(i).setWz(val.get(2).get(i));
		}
	}

	public synchronized void setRi(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(3).size(); i++) {
			this.cells.get(i).setRi(val.get(3).get(i));
		}
	}

	public synchronized void setRo(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(4).size(); i++) {
			this.cells.get(i).setRo(val.get(4).get(i));
		}
	}

	public synchronized void setRz(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(5).size(); i++) {
			this.cells.get(i).setRz(val.get(5).get(i));
		}
	}

	public synchronized void setYt(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(6).size(); i++) {
			this.cells.get(i).setYt(val.get(6).get(i));
		}
	}

	public synchronized void setCt(ArrayList<ArrayList<Double>> val) {
		for (int i = 0; i < val.get(7).size(); i++) {
			this.cells.get(i).setCt(val.get(7).get(i));
		}
	}

	/**
	 * Please build the model with input and target.
	 * 
	 */
	public static class LstmBuilder {

		protected double[] inputData;
		protected double outputData;

		protected double learningRate; //
		protected int epoch = 100; //

		public LstmBuilder(double[] inputData, double outputData) {
			this.inputData = inputData;
			this.outputData = outputData;
		}

		public LstmBuilder setInputData(double[] inputData) {
			this.inputData = inputData;
			return this;
		}

		public LstmBuilder setOutputData(double outputData) {
			this.outputData = outputData;
			return this;
		}

		public LstmBuilder setLearningRate(double learningRate) {
			this.learningRate = learningRate;
			return this;
		}

		public LstmBuilder setEpoch(int epoch) {
			this.epoch = epoch;
			return this;
		}

		public Lstm build() {
			return new Lstm(this);
		}

	}

	/**
	 * Initializes the cell with the default data.
	 */

	public synchronized void initilizeCells() {
		this.cells = new ArrayList<>();
		for (int i = 0; i < this.inputData.length; i++) {
			Cell cell = new Cell(this.inputData[i], this.outputData);
			this.cells.add(cell);
		}

	}

}
