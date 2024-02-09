import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.GetObject;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.Saveobject;
import io.openems.edge.predictor.lstm.multithread.TrainInBatch;

public class TrainInBatchTest {

	/**
	 * This method trains a model in batches using specified hyperparameters. It
	 * iterates through an outer loop, adjusting hyperparameters for each iteration,
	 * and performs training on different sets of training and validation data.
	 * 
	 * @throws ClassNotFoundException If there is an issue loading the
	 *                                HyperParameters object.
	 * @throws IOException            If there is an issue reading or writing files.
	 */
	public static void trainInBatchtest() {

		HyperParameters hyperParameters;
		String modelName = "Consumption";
		try {
			hyperParameters = (HyperParameters) GetObject.get(modelName);
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
//			System.out.println("Creating new hyperparameter object");
			hyperParameters = HyperParameters.getInstance();
			hyperParameters.setModelName(modelName);
		}
		int check = hyperParameters.getOuterLoopCount();

		for (int i = check; i <= 26; i++) {
			hyperParameters.setOuterLoopCount(i);

//			System.out.println("");
			final String pathTrain = Integer.toString(i+1) + ".csv";
			final String pathValidate = Integer.toString(27) + ".csv";
			System.out.println("");
			hyperParameters.printHyperParameters();
			System.out.println("");

			System.out.println(pathTrain);
			System.out.println(pathValidate);

			ReadCsv obj1 = new ReadCsv(pathTrain);
			final ReadCsv obj2 = new ReadCsv(pathValidate);

			new TrainInBatch(obj1.getData(), obj1.getDates(), obj2.getData(), obj2.getDates(), hyperParameters);

			hyperParameters.setEpochTrack(0);
			hyperParameters.setBatchTrack(0);
			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
			Saveobject.save(hyperParameters);
		}

	}

	@Test
	public void test() {
		TrainInBatchTest.trainInBatchtest();

		fail("Not yet implemented");
	}

}
