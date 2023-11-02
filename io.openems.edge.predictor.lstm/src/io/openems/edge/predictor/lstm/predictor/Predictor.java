
package io.openems.edge.predictor.lstm.predictor;

import java.util.ArrayList;

import io.openems.edge.predictor.lstm.common.DataModification;
import io.openems.edge.predictor.lstm.common.DataStatistics;
import io.openems.edge.predictor.lstm.utilities.MathUtils;

public class Predictor {

	/**
	 * Predict output values based on input data and a list of model parameters for
	 * multiple instances. This method takes a list of input data instances and a
	 * list of model parameters and predicts output values for each instance using
	 * the model.
	 *
	 * @param inputData An ArrayList of ArrayLists of Doubles, where each inner
	 *                  ArrayList represents input data for one instance.
	 * @param val       An ArrayList of ArrayLists of ArrayLists of Doubles
	 *                  representing the model parameters for each instance. Each
	 *                  innermost ArrayList should contain model parameters in the
	 *                  following order: 0: Input weight vector (wi) 1: Output
	 *                  weight vector (wo) 2: Recurrent weight vector (wz) 3:
	 *                  Recurrent input activations (rI) 4: Recurrent output
	 *                  activations (rO) 5: Recurrent update activations (rZ) 6:
	 *                  Current cell state (ct) 7: Current output (yt)
	 * @return An ArrayList of Double values representing the predicted output for
	 *         each input data instance.
	 */

	public static ArrayList<Double> predictPre(ArrayList<ArrayList<Double>> inputData,
			ArrayList<ArrayList<ArrayList<Double>>> val) {

		ArrayList<Double> result = new ArrayList<Double>();
		for (int i = 0; i < inputData.size(); i++) {
			ArrayList<Double> wi = val.get(i).get(0);
			ArrayList<Double> wo = val.get(i).get(1);
			ArrayList<Double> wz = val.get(i).get(2);
			ArrayList<Double> rI = val.get(i).get(3);
			ArrayList<Double> rO = val.get(i).get(4);
			ArrayList<Double> rZ = val.get(i).get(5);
			ArrayList<Double> ct = val.get(i).get(7);
			ArrayList<Double> yt = val.get(i).get(6);

			result.add(predict(inputData.get(i), wi, wo, wz, rI, rO, rZ, ct, yt));
		}

		return result;
	}

	/**
	 * Predict an output value based on input data and model parameters. This method
	 * predicts a single output value based on input data and a set of model
	 * parameters for a recurrent neural network (RNN) model.
	 * 
	 * @param inputData An ArrayList of Doubles representing the input data for
	 *                  prediction.
	 * @param wi        An ArrayList of Doubles representing the input weight vector
	 *                  (wi) for the RNN model.
	 * @param wo        An ArrayList of Doubles representing the output weight
	 *                  vector (wo) for the RNN model.
	 * @param wz        An ArrayList of Doubles representing the recurrent weight
	 *                  vector (wz) for the RNN model.
	 * @param rI        An ArrayList of Doubles representing the recurrent input
	 *                  activations (rI) for the RNN model.
	 * @param rO        An ArrayList of Doubles representing the recurrent output
	 *                  activations (rO) for the RNN model.
	 * @param rZ        An ArrayList of Doubles representing the recurrent update
	 *                  activations (rZ) for the RNN model.
	 * @param cta       An ArrayList of Doubles representing the current cell state
	 *                  (ct) for the RNN model.
	 * @param yta       An ArrayList of Doubles representing the current output (yt)
	 *                  for the RNN model.
	 * @return A double representing the predicted output value based on the input
	 *         data and model parameters.
	 */

	public static double predict(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ,
			ArrayList<Double> cta, ArrayList<Double> yta) {
		double ct = 0;
		double xt = 0;
		double it = 0;
		double ot = 0;
		double zt = 0;
		double yt = 0;
		ArrayList<Double> standData = DataModification.standardize(inputData);

		for (int i = 0; i < standData.size(); i++) {
			xt = standData.get(i);
			it = MathUtils.sigmoid(wi.get(i) * xt + rI.get(i) * yt);
			ot = MathUtils.sigmoid(wo.get(i) * xt + rO.get(i) * yt);
			zt = MathUtils.tanh(wz.get(i) * xt + rZ.get(i) * yt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);

		}
		double res = DataModification.reverseStandrize(DataStatistics.getMean(inputData),
				DataStatistics.getStanderDeviation(inputData), yt);

		return res;
	}

	/**
	 * Predict a focused output value based on input data and model parameters. This
	 * method predicts a single focused output value based on input data and a set
	 * of model parameters for a recurrent neural network (RNN) model with a focus
	 * on specific activations.
	 *
	 * @param inputData An ArrayList of Doubles representing the input data for
	 *                  prediction.
	 * @param wi        An ArrayList of Doubles representing the input weight vector
	 *                  (wi) for the RNN model.
	 * @param wo        An ArrayList of Doubles representing the output weight
	 *                  vector (wo) for the RNN model.
	 * @param wz        An ArrayList of Doubles representing the recurrent weight
	 *                  vector (wz) for the RNN model.
	 * @param rI        An ArrayList of Doubles representing the recurrent input
	 *                  activations (rI) for the RNN model.
	 * @param rO        An ArrayList of Doubles representing the recurrent output
	 *                  activations (rO) for the RNN model.
	 * @param rZ        An ArrayList of Doubles representing the recurrent update
	 *                  activations (rZ) for the RNN model.
	 * @param cta       An ArrayList of Doubles representing the current cell state
	 *                  (ct) for the RNN model.
	 * @param yta       An ArrayList of Doubles representing the current output (yt)
	 *                  for the RNN model.
	 * @return A double representing the predicted focused output value based on the
	 *         input data and model parameters.
	 */

	public static double predictFocoused(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ,
			ArrayList<Double> cta, ArrayList<Double> yta) {
		double ct = 0;
		double xt = 0;
		double it = 0;
		double ot = 0;
		double zt = 0;
		double yt = 0;
		ArrayList<Double> standData = DataModification.standardize(inputData);

		for (int i = 0; i < standData.size(); i++) {
			xt = standData.get(i);
			it = MathUtils.sigmoid(rI.get(i) * yt);
			ot = MathUtils.sigmoid(rO.get(i) * yt);
			zt = MathUtils.tanh(wz.get(i) * xt);
			ct = ct + it * zt;
			yt = ot * MathUtils.tanh(ct);

		}
		double res = DataModification.reverseStandrize(DataStatistics.getMean(inputData),
				DataStatistics.getStanderDeviation(inputData), yt);

		return res;
	}

}
