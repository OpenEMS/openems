package io.openems.edge.predictor.lstmmodel;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessing.DataModification;

public class LstmPredictorTest {
	private HyperParameters hyperParameters = new HyperParameters();

	private ArrayList<ArrayList<Double>> modelTrend = new ArrayList<>(Arrays.asList(
			new ArrayList<>(Arrays.asList(0.30000000000000004, -0.10191832534531027, -0.19262844428679757,
					0.016925024201681654)),
			new ArrayList<>(
					Arrays.asList(-0.7999999999999999, -0.3142909416393413, -0.3341676120993015, -0.09089772222510135)),
			new ArrayList<>(Arrays.asList(-0.4999999999999999, 0.051555896559209405, -0.11477687998526631,
					0.10826117268571883)),
			new ArrayList<>(Arrays.asList(-0.6, -1.449260711226437, -1.6789748520719996, -1.6707673970279129)),
			new ArrayList<>(
					Arrays.asList(1.9000000000000004, 2.0276163313785935, 2.0457575003167086, 1.716902676376759)),
			new ArrayList<>(Arrays.asList(0.09999999999999995, -0.40632251238009526, -0.2902480457595551,
					-0.21870167929155354)),
			new ArrayList<>(Arrays.asList(-0.08825349909436375, -0.10024408682002699, -0.0891597522413061,
					-0.11174726093461877)),
			new ArrayList<>(Arrays.asList(-0.2529282639641216, -0.24738024250988547, -0.18556978270548535,
					-0.2302537524898713))));

	/**
	 * Prediction test.
	 */
	// @Test
	public void predictTest() {
	

		/**
		 * IMPULSE RESPONSE : impulses are the sudden change in consumption for a very
		 * short period of time
		 * 
		 * Example : When someone runs the electric drilling machine
		 * 
		 * When the change magnitude of data last indexed data is very high compared to
		 * other data in an array, model identifies it as an impulse.
		 * 
		 * Model will make a prediction negating the drastic change
		 * 
		 *
		 */
		double result;
		ArrayList<Double> impulseSimulation = new ArrayList<>(Arrays.asList(50.0, 55.0, 55.0, 150.0));
		result = LstmPredictor.predict(
				DataModification.scale(impulseSimulation, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Impulse Response = " + result);

		/*
		 * STEP RESPONSE : Example: plugging in EV for charging
		 * 
		 */

		ArrayList<Double> stepSimulation1 = new ArrayList<>(Arrays.asList(55.0, 45.0, 150.0, 150.0));
		result = LstmPredictor.predict(
				DataModification.scale(stepSimulation1, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Response to Step input 1 = " + result);
		assertEquals(result, 96.37775106958219, 0.001);

		ArrayList<Double> stepSimulation2 = new ArrayList<>(Arrays.asList(45.0, 150.0, 150.0, 150.0));
		result = LstmPredictor.predict(
				DataModification.scale(stepSimulation2, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Response to Step input 2 = " + result);
		assertEquals(result, 119.93810155853586, 0.0001);

		ArrayList<Double> stepSimulation3 = new ArrayList<>(Arrays.asList(150.0, 150.0, 150.0, 150.0));
		result = LstmPredictor.predict(
				DataModification.scale(stepSimulation3, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Response to Step input 3 = " + result);
		assertEquals(result, 149.9999999999999, 0.001);

		/*
		 * RESPONSE TO RAMP INPUT
		 */

		ArrayList<Double> rampInput = new ArrayList<>(Arrays.asList(100.0, 200.0, 400.0, 800.0));
		result = LstmPredictor.predict(
				DataModification.scale(rampInput, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Response to Ramp input = " + result);
		assertEquals(result, 359.85918502556444, 0.001);

		/*
		 * RESPONSE to exponential input
		 * 
		 */

		ArrayList<Double> expInput = new ArrayList<>(Arrays.asList(20.0, 400.0, 160000.0, 3200000000.0));
		result = LstmPredictor.predict(
				DataModification.scale(expInput, this.hyperParameters.getScalingMin(),
						this.hyperParameters.getScalingMax()),
				this.modelTrend.get(0), this.modelTrend.get(1), this.modelTrend.get(2), this.modelTrend.get(3),
				this.modelTrend.get(4), this.modelTrend.get(5), this.modelTrend.get(7), this.modelTrend.get(6),
				this.hyperParameters);
		result = DataModification.scaleBack(result, this.hyperParameters.getScalingMin(),
				this.hyperParameters.getScalingMax());

		// System.out.println("Response toExp inPut= " + result);
		assertEquals(result, 7.253657290977542E8, 0.001);

	}

}
