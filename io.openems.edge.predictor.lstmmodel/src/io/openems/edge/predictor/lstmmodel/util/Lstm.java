package io.openems.edge.predictor.lstmmodel.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

public class Lstm {

	private double[] inputData;
	private double outputData;
	private double derivativeLWrtRi = 0;
	private double derivativeLWrtRo = 0;
	private double derivativeLWrtRz = 0;
	private double derivativeLWrtWi = 0;
	private double derivativeLWrtWo = 0;
	private double derivativeLWrtWz = 0;
	private double learningRate; //
	private int epoch = 100;

	protected ArrayList<Cell> cells;

	public static Function<ArrayList<Double>, Integer> getMin = (arrayList) -> {
		return arrayList.indexOf(Collections.min(arrayList));
	};

	public Lstm(LstmBuilder builder) {
		this.inputData = builder.inputData;
		this.outputData = builder.outputData;
//		this.derivativeLWrtRi = builder.derivativeLWrtRi;
//		this.derivativeLWrtRo = builder.derivativeLWrtRo;
//		this.derivativeLWrtRz = builder.derivativeLWrtRz;
//
//		this.derivativeLWrtWi = builder.derivativeLWrtWi;
//		this.derivativeLWrtWo = builder.derivativeLWrtWo;
//		this.derivativeLWrtWz = builder.derivativeLWrtWz;
		this.learningRate = builder.learningRate;
		this.epoch = builder.epoch;
	}

	/**
	 * Forward propagation.
	 */
	public void forwardprop() {// --------------------------------------------------------------------------------check
								// this------------------
		try {
			for (int i = 0; i < this.cells.size(); i++) {
				this.cells.get(i).forwardPropogation();
				if (i < this.cells.size() - 1) {
					this.cells.get(i + 1).yT = this.cells.get(i).yT;
					this.cells.get(i + 1).cT = this.cells.get(i).cT;
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

		double localLearningRate1 = 0;
		double localLearningRate2 = 0;
		double localLearningRate3 = 0;
		double localLearningRate4 = 0;
		double localLearningRate5 = 0;
		double localLearningRate6 = 0;

		for (int i = this.cells.size() - 1; i >= 0; i--) {
			if (i == this.cells.size() - 1) {
				this.cells.get(i).backwardPropogation();
			} else {
				this.cells.get(i).dlByDc = this.cells.get(i + 1).dlByDc;
				this.cells.get(i).backwardPropogation();
			}
		}

		for (int i = 0; i < this.cells.size(); i++) {
			this.derivativeLWrtRi += this.cells.get(i).yT * this.cells.get(i).delI;
			this.derivativeLWrtRo += this.cells.get(i).yT * this.cells.get(i).delO;
			this.derivativeLWrtRz += this.cells.get(i).yT * this.cells.get(i).delZ;

			this.derivativeLWrtWi += this.cells.get(i).xT * this.cells.get(i).delI;
			this.derivativeLWrtWo += this.cells.get(i).xT * this.cells.get(i).delO;
			this.derivativeLWrtWz += this.cells.get(i).xT * this.cells.get(i).delZ;

			adaptiveLearningRate rate = new adaptiveLearningRate();
			localLearningRate1 = rate.adagradOptimizer(learningRate, localLearningRate1, this.derivativeLWrtWi, i);
			localLearningRate2 = rate.adagradOptimizer(learningRate, localLearningRate2, this.derivativeLWrtWo, i);
			localLearningRate3 = rate.adagradOptimizer(learningRate, localLearningRate3, this.derivativeLWrtWz, i);
			localLearningRate4 = rate.adagradOptimizer(learningRate, localLearningRate4, this.derivativeLWrtRi, i);
			localLearningRate5 = rate.adagradOptimizer(learningRate, localLearningRate5, this.derivativeLWrtRo, i);
			localLearningRate6 = rate.adagradOptimizer(learningRate, localLearningRate6, this.derivativeLWrtRz, i);

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
		// System.out.print("Epoch : ");
		for (int i = 0; i < this.epoch; i++) {
			// System.out.print(" " + i + " ..");

			this.forwardprop();
			this.backwardprop();

			ArrayList<Double> temp1 = new ArrayList<Double>();
			ArrayList<Double> temp2 = new ArrayList<Double>();
			ArrayList<Double> temp3 = new ArrayList<Double>();
			ArrayList<Double> temp4 = new ArrayList<Double>();
			ArrayList<Double> temp5 = new ArrayList<Double>();
			ArrayList<Double> temp6 = new ArrayList<Double>();
			ArrayList<Double> temp7 = new ArrayList<Double>();
			ArrayList<Double> temp8 = new ArrayList<Double>();
			
			for (int j = 0; j < this.cells.size(); j++) {
				temp1.add(this.cells.get(j).getWi()); // wi
				temp2.add(this.cells.get(j).getWo()); // wo
				temp3.add(this.cells.get(j).getWz()); // wz
				temp4.add(this.cells.get(j).getRi()); // Ri
				temp5.add(this.cells.get(j).getRo()); // Ro
				temp6.add(this.cells.get(j).getRz()); // Rz
				temp7.add(this.cells.get(j).yT);
				temp8.add(this.cells.get(j).getCt());
			}

			mW.errorList.add(this.cells.get(this.cells.size() - 1).getError());
			mW.wI.add(temp1);
			mW.wO.add(temp2);
			mW.wZ.add(temp3);
			mW.rI.add(temp4);
			mW.rO.add(temp5);
			mW.rZ.add(temp6);
			mW.out.add(temp7);
			mW.cT.add(temp8);
		}

		int ind = findGlobalMinima(mW.errorList);

		ArrayList<ArrayList<Double>> returnArray = new ArrayList<ArrayList<Double>>();
		returnArray.add(mW.wI.get(ind));
		returnArray.add(mW.wO.get(ind));
		returnArray.add(mW.wZ.get(ind));
		returnArray.add(mW.rI.get(ind));
		returnArray.add(mW.rO.get(ind));
		returnArray.add(mW.rZ.get(ind));
		returnArray.add(mW.out.get(ind));
		returnArray.add(mW.cT.get(ind));

		ArrayList<Double> err = new ArrayList<Double>();
		err.add(mW.errorList.get(ind));

		//returnArray.add(err);
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

		ArrayList<Double> mn = new ArrayList<Double>();
		ArrayList<Integer> index = new ArrayList<Integer>();

		for (int idx = 0; idx < data.size() - 1; idx++) {
			if ((data.get(idx) > 0 && data.get(idx + 1) < 0)) {
				mn.add(data.get(idx));
				index.add(idx);
			} else if ((data.get(idx) < 0 && data.get(idx + 1) > 0)) {
				mn.add(data.get(idx + 1));
				index.add(idx + 1);
			} else {
				// do nothing
			}
		}

		return mn.isEmpty() ? getMin.apply(data) : index.get(getMin.apply(mn));

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
	public void setWi(ArrayList<ArrayList<Double>> val) {
		for(int i = 0; i<this.cells.size();i++)
		{
			this.cells.get(i).setWi(val.get(0).get(i));
		}
	}
public void setWo(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).setWo(val.get(1).get(i));
	}
	
	
}
public void setWz(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).setWz(val.get(2).get(i));
	}
	
}
public void setRi(ArrayList<ArrayList<Double>> val) {
	
	
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).setRi(val.get(3).get(i));
	}
}
public void setRo(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).setRo(val.get(4).get(i));
	}
	
}
public void setRz(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).setRz(val.get(5).get(i));
	}
	
}
public void setYt(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
	{
		this.cells.get(i).yT=val.get(6).get(i);
	}
	

}
public void setCt(ArrayList<ArrayList<Double>> val) {
	for(int i = 0; i<this.cells.size();i++)
 
	{
		this.cells.get(i).cT=val.get(7).get(i);
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
		
		public LstmBuilder setWi(ArrayList<ArrayList<Double>> val) {
			
			return this;
		}
		
		
		public Lstm build() {
			return new Lstm(this);
		}

	}

	/**
	 * Initializes the cell with the default data.
	 */
	public void initilizeCells() {
		this.cells = new ArrayList<>();
		for (int i = 0; i < this.inputData.length; i++) {
			Cell a = new Cell(this.inputData[i], this.outputData);
			this.cells.add(a);
		}

	}

	

}
