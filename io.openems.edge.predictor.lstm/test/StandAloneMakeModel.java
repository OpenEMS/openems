
import org.junit.Test;

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
		for (int i = 0; i < 27; i++) {
			int itterNumb = i;
			System.out.println("Training for data set " + i);
			String pathTrain = Integer.toString(itterNumb + 1) + ".csv";
			String pathValidate = Integer.toString(itterNumb + 2) + ".csv";
			ReadCsv obj1 = new ReadCsv(pathTrain);
			ReadCsv obj2 = new ReadCsv(pathValidate);
			MakeModel obj = new MakeModel(obj1.getData(), obj1.getDates(), itterNumb);
			Validation obj3 = new Validation(obj2.getData(), obj2.getDates(), itterNumb);
		}
	}

	@Test
	public void test() {
		StandAloneMakeModel.itter();

	}
}
