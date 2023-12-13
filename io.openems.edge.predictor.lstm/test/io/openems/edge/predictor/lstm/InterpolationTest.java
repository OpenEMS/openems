package io.openems.edge.predictor.lstm;

import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.interpolation.InterpolationManager;

public class InterpolationTest {
	/**
	 * The main class that reads data from a CSV file, performs interpolation, and
	 * prints the results.
	 */
	public void main() {
		HyperParameters hyperParameters = new HyperParameters();
		String csvFileName = "1.csv";
		ReadCsv csv = new ReadCsv(csvFileName);
		// ArrayList<ArrayList<Double>> weight1 = new ArrayList<ArrayList<Double>>();
		// System.out.println("data = " + csv.getData());

		InterpolationManager inter = new InterpolationManager(csv.getData(), csv.getDates(), hyperParameters);
		System.out.println(inter.getInterpolatedData());
		System.out.println(inter.getNewDates());
	}

	@Test
	public void test() {
		InterpolationTest obj = new InterpolationTest();
		obj.main();
		fail("Not yet implemented");
	}

}
