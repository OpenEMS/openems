package io.openems.edge.predictor.lstmmodel;

import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArray;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to1DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to2DArrayList;
import static io.openems.edge.predictor.lstmmodel.utilities.UtilityConversion.to2DList;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.predictor.lstmmodel.common.DataStatistics;
import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.PreprocessingPipeImpl;
import io.openems.edge.predictor.lstmmodel.utilities.MathUtils;

public class LstmPredictor {

	/**
	 * Predicts seasonality based on the provided data and models.
	 *
	 * @param data            The input data to predict seasonality for.
	 * @param date            The corresponding date and time information for the
	 *                        data points.
	 * @param hyperParameters The hyperparameters for the prediction model.
	 * @return A list of predicted values for the seasonality.
	 * @throws SomeException If there's any specific exception that might be thrown
	 *                       during the process.
	 */
	public static ArrayList<Double> predictSeasonality(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			HyperParameters hyperParameters) {

		var preprocessing = new PreprocessingPipeImpl(hyperParameters);

		preprocessing.setData(to1DArray(data)).setDates(date);

		var resized = to2DList((double[][][]) preprocessing.interpolate()//
				.scale()//
				.filterOutliers() //
				.groupByHoursAndMinutes()//
				.execute());

		preprocessing.setData(resized);

		var normalized = (double[][]) preprocessing//
				.normalize()//
				.execute();

		var allModel = hyperParameters.getBestModelSeasonality();

		var predicted = predictPre(to2DArrayList(normalized), allModel, hyperParameters);

		preprocessing.setData(to1DArray(predicted))//
				.setMean(DataStatistics.getMean(resized))
				.setStandardDeviation(DataStatistics.getStandardDeviation(resized));

		var seasonalityPrediction = (double[]) preprocessing.reverseNormalize()//
				.reverseScale()//
				.execute();

		return to1DArrayList(seasonalityPrediction);
	}

	/**
	 * Predicts trend values for a given time period using LSTM models.
	 *
	 * @param data            The historical data for trend prediction.
	 * @param date            The corresponding date and time information for the
	 *                        historical data points.
	 * @param until           The target time until which trend values will be
	 *                        predicted.
	 * @param hyperParameters The hyperparameters for the prediction model.
	 * @return A list of predicted trend values.
	 * @throws SomeException If there's any specific exception that might be thrown
	 *                       during the process.
	 */
	public static ArrayList<Double> predictTrend(ArrayList<Double> data, ArrayList<OffsetDateTime> date,
			ZonedDateTime until, HyperParameters hyperParameters) {

		var preprocessing = new PreprocessingPipeImpl(hyperParameters);
		preprocessing.setData(to1DArray(data)).setDates(date);

		var scaled = (double[]) preprocessing//
				.interpolate()//
				.scale()//
				.execute();

		// normalize
		var trendPrediction = new double[hyperParameters.getTrendPoint()];
		var mean = DataStatistics.getMean(scaled);
		var standerDev = DataStatistics.getStandardDeviation(scaled);

		preprocessing.setData(scaled);

		ArrayList<Double> normData = to1DArrayList((double[]) preprocessing//
				.normalize()//
				.execute());

		var predictionFor = until.plusMinutes(hyperParameters.getInterval());
		var val = hyperParameters.getBestModelTrend();
		for (int i = 0; i < hyperParameters.getTrendPoint(); i++) {
			var temp = predictionFor.plusMinutes(i * hyperParameters.getInterval());

			var modlelindex = (int) decodeDateToColumnIndex(temp, hyperParameters);
			double predTemp = LstmPredictor.predict(normData, val.get(modlelindex).get(0), val.get(modlelindex).get(1),
					val.get(modlelindex).get(2), val.get(modlelindex).get(3), val.get(modlelindex).get(4),
					val.get(modlelindex).get(5), val.get(modlelindex).get(7), val.get(modlelindex).get(6),
					hyperParameters);
			normData.add(predTemp);
			normData.remove(0);

			trendPrediction[i] = (predTemp);
		}

		preprocessing.setData(trendPrediction).setMean(mean).setStandardDeviation(standerDev);

		return to1DArrayList((double[]) preprocessing//
				.reverseNormalize()//
				.reverseScale()//
				.execute());
	}

	/**
	 * Decodes a ZonedDateTime to its corresponding column index based on prediction
	 * interval and window size.
	 *
	 * @param predictionFor   The ZonedDateTime for which the column index is to be
	 *                        decoded.
	 * @param hyperParameters The hyperparameters for the prediction model.
	 * @return The decoded column index for the given ZonedDateTime. If the index is
	 *         negative, it is adjusted to the corresponding positive index for a
	 *         24-hour period.
	 */
	public static double decodeDateToColumnIndex(ZonedDateTime predictionFor, HyperParameters hyperParameters) {

		var hour = predictionFor.getHour();

		var minute = predictionFor.getMinute();
		var index = (Integer) hour * (60 / hyperParameters.getInterval()) + minute / hyperParameters.getInterval();
		var modifiedIndex = index - hyperParameters.getWindowSizeTrend();
		if (modifiedIndex >= 0) {
			return modifiedIndex;
		} else {
			return modifiedIndex + 60 / hyperParameters.getInterval() * 24;
		}
	}

	/**
	 * Re-arranges an ArrayList of Double values by splitting it at the specified
	 * index and moving the second part to the front.
	 *
	 * @param splitIndex  The index at which the ArrayList will be split.
	 * @param singleArray An ArrayList of Double values to be re-arranged.
	 * @return A new ArrayList containing the Double values after re-arrangement.
	 */
	public static ArrayList<Double> getArranged(int splitIndex, ArrayList<Double> singleArray) {
		var arranged = new ArrayList<Double>();
		var firstGroup = new ArrayList<Double>();
		var secondGroup = new ArrayList<Double>();

		for (var i = 0; i < singleArray.size(); i++) {
			if (i < splitIndex) {
				firstGroup.add(singleArray.get(i));
			} else {
				secondGroup.add(singleArray.get(i));
			}
		}

		arranged.addAll(secondGroup);
		arranged.addAll(firstGroup);

		return arranged;
	}

	/**
	 * Calculates the index of a specific hour and minute combination within a
	 * 24-hour period, divided into 15-minute intervals.
	 *
	 * @param hour            The hour component (0-23) to be used for the
	 *                        calculation.
	 * @param minute          The minute component (0, 5, 10, ..., 55) to be used
	 *                        for the
	 * @param hyperParameters is the object of class HyperParameters, calculation.
	 * @return The index representing the specified hour and minute combination.
	 */
	public static Integer getIndex(Integer hour, Integer minute, HyperParameters hyperParameters) {

		var k = 0;
		for (var i = 0; i < 24; i++) {
			for (var j = 0; j < (int) 60 / hyperParameters.getInterval(); j++) {
				var h = i;
				var m = j * hyperParameters.getInterval();
				if (hour == h && minute == m) {
					return k;
				} else {
					k = k + 1;
				}
			}
		}
		return k;
	}

	/**
	 * Predict output values based on input data and a list of model parameters for
	 * multiple instances. This method takes a list of input data instances and a
	 * list of model parameters and predicts output values for each instance using
	 * the model.
	 *
	 * @param inputData       An ArrayList of ArrayLists of Doubles, where each
	 *                        inner ArrayList represents input data for one
	 *                        instance.
	 * @param val             An ArrayList of ArrayLists of ArrayLists of Doubles
	 *                        representing the model parameters for each instance.
	 *                        Each innermost ArrayList should contain model
	 *                        parameters in the following order: 0: Input weight
	 *                        vector (wi) 1: Output weight vector (wo) 2: Recurrent
	 *                        weight vector (wz) 3: Recurrent input activations (rI)
	 *                        4: Recurrent output activations (rO) 5: Recurrent
	 *                        update activations (rZ) 6: Current cell state (ct) 7:
	 *                        Current output (yt)
	 * @param hyperParameters instance of class HyperParamters data
	 * @return An ArrayList of Double values representing the predicted output for
	 *         each input data instance.
	 */
	public static ArrayList<Double> predictPre(ArrayList<ArrayList<Double>> inputData,
			ArrayList<ArrayList<ArrayList<Double>>> val, HyperParameters hyperParameters) {

		var result = new ArrayList<Double>();
		for (var i = 0; i < inputData.size(); i++) {

			var wi = val.get(i).get(0);
			var wo = val.get(i).get(1);
			var wz = val.get(i).get(2);
			var rI = val.get(i).get(3);
			var rO = val.get(i).get(4);
			var rZ = val.get(i).get(5);
			var ct = val.get(i).get(7);
			var yt = val.get(i).get(6);

			result.add(predict(inputData.get(i), wi, wo, wz, rI, rO, rZ, ct, yt, hyperParameters));
		}
		return result;
	}

	/**
	 * Predict the output values based on input data and model parameters. This
	 * method takes input data and a set of model parameters and predicts output
	 * values for each data point using the model.
	 *
	 * @param data            A 2D array representing the input data where each row
	 *                        is a data point.
	 * @param val             An ArrayList containing model parameters, including
	 *                        weight vectors and activation values. The ArrayList
	 *                        should contain the following sublists in this order:
	 *                        0: Input weight vector (wi) 1: Output weight vector
	 *                        (wo) 2: Recurrent weight vector (wz) 3: Recurrent
	 *                        input activations (rI) 4: Recurrent output activations
	 *                        (rO) 5: Recurrent update activations (rZ) 6: Current
	 *                        output (yt) 7: Current cell state (ct)
	 *
	 * @param hyperParameters instance of class HyperParamters data
	 * 
	 * @return An ArrayList of Double values representing the predicted output for
	 *         each input data point.
	 * 
	 */
	public static ArrayList<Double> predictPre(double[][] data, List<ArrayList<Double>> val,
			HyperParameters hyperParameters) {

		var result = new ArrayList<Double>();

		var wi = val.get(0);
		var wo = val.get(1);
		var wz = val.get(2);
		var rI = val.get(3);
		var rO = val.get(4);
		var rZ = val.get(5);
		var yt = val.get(6);
		var ct = val.get(7);

		for (var i = 0; i < data.length; i++) {
			result.add(predict(data[i], wi, wo, wz, rI, rO, rZ, yt, ct, hyperParameters));
		}
		return result;
	}

	/**
	 * Predict an output value based on input data and model parameters. This method
	 * predicts a single output value based on input data and a set of model
	 * parameters for a LSTM model.
	 * 
	 * @param inputData       An ArrayList of Doubles representing the input data
	 *                        for prediction.
	 * @param wi              An ArrayList of Doubles representing the input weight
	 *                        vector (wi) for the RNN model.
	 * @param wo              An ArrayList of Doubles representing the output weight
	 *                        vector (wo) for the RNN model.
	 * @param wz              An ArrayList of Doubles representing the recurrent
	 *                        weight vector (wz) for the RNN model.
	 * @param rI              An ArrayList of Doubles representing the recurrent
	 *                        input activations (rI) for the RNN model.
	 * @param rO              An ArrayList of Doubles representing the recurrent
	 *                        output activations (rO) for the RNN model.
	 * @param rZ              An ArrayList of Doubles representing the recurrent
	 *                        update activations (rZ) for the RNN model.
	 * @param cta             An ArrayList of Doubles representing the current cell
	 *                        state (ct) for the RNN model.
	 * @param yta             An ArrayList of Doubles representing the current
	 *                        output (yt) for the RNN model.
	 * @param hyperParameters instance of class HyperParamters data
	 * @return A double representing the predicted output value based on the input
	 *         data and model parameters.
	 */
	public static double predict(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ,
			ArrayList<Double> cta, ArrayList<Double> yta, HyperParameters hyperParameters) {
		var ct = hyperParameters.getCtInit();
		var yt = hyperParameters.getYtInit();
		var standData = inputData;// DataModification.standardize(inputData, hyperParameters);

		for (var i = 0; i < standData.size(); i++) {
			var ctMinusOne = ct;
			var yTMinusOne = yt;
			var xt = standData.get(i);
			var it = MathUtils.sigmoid(wi.get(i) * xt + rI.get(i) * yTMinusOne);
			var ot = MathUtils.sigmoid(wo.get(i) * xt + rO.get(i) * yTMinusOne);
			var zt = MathUtils.tanh(wz.get(i) * xt + rZ.get(i) * yTMinusOne);
			ct = ctMinusOne + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		return yt;
	}

	/**
	 * Predict an output value based on input data and model parameters. This method
	 * predicts a single output value based on input data and a set of model
	 * parameters for a LSTM model.
	 * 
	 * @param inputData       An ArrayList of Doubles representing the input data
	 *                        for prediction.
	 * @param wi              An ArrayList of Doubles representing the input weight
	 *                        vector (wi) for the RNN model.
	 * @param wo              An ArrayList of Doubles representing the output weight
	 *                        vector (wo) for the RNN model.
	 * @param wz              An ArrayList of Doubles representing the recurrent
	 *                        weight vector (wz) for the RNN model.
	 * @param rI              An ArrayList of Doubles representing the recurrent
	 *                        input activations (rI) for the RNN model.
	 * @param rO              An ArrayList of Doubles representing the recurrent
	 *                        output activations (rO) for the RNN model.
	 * @param rZ              An ArrayList of Doubles representing the recurrent
	 *                        update activations (rZ) for the RNN model.
	 * @param cta             An ArrayList of Doubles representing the current cell
	 *                        state (ct) for the RNN model.
	 * @param yta             An ArrayList of Doubles representing the current
	 *                        output (yt) for the RNN model.
	 * @param hyperParameters instance of class HyperParamters data
	 * @return A double representing the predicted output value based on the input
	 *         data and model parameters.
	 */
	public static double predict(double[] inputData, ArrayList<Double> wi, ArrayList<Double> wo, ArrayList<Double> wz,
			ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ, ArrayList<Double> cta,
			ArrayList<Double> yta, HyperParameters hyperParameters) {
		var ct = hyperParameters.getCtInit();
		var yt = hyperParameters.getYtInit();
		var standData = inputData;// DataModification.standardize(inputData, hyperParameters);

		for (var i = 0; i < standData.length; i++) {
			var ctMinusOne = ct;
			var yTMinusOne = yt;
			var xt = standData.length;
			var it = MathUtils.sigmoid(wi.get(i) * xt + rI.get(i) * yTMinusOne);
			var ot = MathUtils.sigmoid(wo.get(i) * xt + rO.get(i) * yTMinusOne);
			var zt = MathUtils.tanh(wz.get(i) * xt + rZ.get(i) * yTMinusOne);
			ct = ctMinusOne + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		return yt;
	}

	/**
	 * Predict a focused output value based on input data and model parameters. This
	 * method predicts a single focused output value based on input data and a set
	 * of model parameters for a LSTM model with a focus on specific activations.
	 *
	 * @param inputData       An ArrayList of Doubles representing the input data
	 *                        for prediction.
	 * @param wi              An ArrayList of Doubles representing the input weight
	 *                        vector (wi) for the RNN model.
	 * @param wo              An ArrayList of Doubles representing the output weight
	 *                        vector (wo) for the RNN model.
	 * @param wz              An ArrayList of Doubles representing the recurrent
	 *                        weight vector (wz) for the RNN model.
	 * @param rI              An ArrayList of Doubles representing the recurrent
	 *                        input activations (rI) for the RNN model.
	 * @param rO              An ArrayList of Doubles representing the recurrent
	 *                        output activations (rO) for the RNN model.
	 * @param rZ              An ArrayList of Doubles representing the recurrent
	 *                        update activations (rZ) for the RNN model.
	 * @param cta             An ArrayList of Doubles representing the current cell
	 *                        state (ct) for the RNN model.
	 * @param yta             An ArrayList of Doubles representing the current
	 *                        output (yt) for the RNN model.
	 * @param hyperParameters instance of class HyperParamters data
	 * @return A double representing the predicted focused output value based on the
	 *         input data and model parameters.
	 */
	public static double predictFocoused(ArrayList<Double> inputData, ArrayList<Double> wi, ArrayList<Double> wo,
			ArrayList<Double> wz, ArrayList<Double> rI, ArrayList<Double> rO, ArrayList<Double> rZ,
			ArrayList<Double> cta, ArrayList<Double> yta, HyperParameters hyperParameters) {
		var ct = hyperParameters.getCtInit();
		var yt = hyperParameters.getYtInit();

		var standData = inputData;

		for (var i = 0; i < standData.size(); i++) {
			var ctMinusOne = ct;
			var ytMinusOne = yt;
			var xt = standData.get(i);
			var it = MathUtils.sigmoid(rI.get(i) * ytMinusOne);
			var ot = MathUtils.sigmoid(rO.get(i) * ytMinusOne);
			var zt = MathUtils.tanh(wz.get(i) * xt);
			ct = ctMinusOne + it * zt;
			yt = ot * MathUtils.tanh(ct);
		}
		return yt;
	}
}
