
import java.util.Collections;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.train.MakeModel;
import io.openems.edge.predictor.lstm.validator.Validation;

public class StandAloneMakeModel {
	/**
	 * Iterates through a series of data sets for training and validation. This
	 * method is designed to loop through data sets and perform training and
	 * validation tasks for each data set. It uses a set of CSV files named with
	 * consecutive numbers (e.g., "1.csv", "2.csv") to load data. For each
	 * iteration, it trains a model on the training data set and performs validation
	 * on the validation data set.
	 */

	public static void itter() {
		int k = 0;
		HyperParameters hyperParameters = new HyperParameters();
		for (int i = 0; i < 1; i++) {

			String pathTrain = Integer.toString(i) + ".csv";
			String pathValidate = Integer.toString(i + 1) + ".csv";

			for (int j = 0; j < hyperParameters.getEpoch(); j++) {
				hyperParameters.setCount(k);
				System.out.println("Epoch=  " + j);

				ReadCsv obj1 = new ReadCsv(pathTrain);
				final ReadCsv obj2 = new ReadCsv(pathValidate);
				hyperParameters.setScalingMax(Collections.max(obj1.getData()));
				hyperParameters.setScalingMin(Collections.min(obj1.getData()));
				MakeModel obj = new MakeModel(obj1.getData(), obj1.getDates(), hyperParameters);
				Validation obj3 = new Validation(obj2.getData(), obj2.getDates(), hyperParameters);
				k = k + 1;
			}
		}
	}

	@Test
	public void test() {
		StandAloneMakeModel.itter();

	}
}
