import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DeletModels;
import io.openems.edge.predictor.lstm.common.GetObject;
import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.common.Saveobject;
import io.openems.edge.predictor.lstm.multithread.MultiThreadTrain;

public class MultiThreadTest {

	/**
	 * Trains the model using the specified hyperparameters and data. If a saved
	 * hyperparameters object exists, it is loaded; otherwise, a new one is created.
	 * Checks if the training has been completed in a previous batch and resets the
	 * epoch track accordingly. Iterates over batches, printing progress and RMS
	 * errors for trend and seasonality. Uses multithreading for training on both
	 * training and validation datasets.
	 * 
	 * @throws ClassNotFoundException if the hyperparameters object cannot be found
	 *                                during deserialization.
	 * @throws IOException            if an I/O error occurs during deserialization.
	 */

	public static void train() {

		HyperParameters hyperParameters;
		try {
			hyperParameters = (HyperParameters) GetObject.get();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Creating new hyperparameter object");
			hyperParameters = HyperParameters.getInstance();
		}

		// checking if the training has been completed in previous batch

		if (hyperParameters.getEpochTrack() == hyperParameters.getEpoch()) {
			// hyperParameters.setEpochTrack(0);

		}

		int check = hyperParameters.getOuterLoopCount();
		for (int i = check; i <= 8; i++) {
			hyperParameters.setOuterLoopCount(i);
			System.out.println("Batch:" + i + "/" + 28);
			System.out.println("count :" + hyperParameters.getCount());
			System.out.println("Rms Error of all train for the trend is  = " + hyperParameters.getRmsErrorTrend());
			System.out.println(
					"Rms Error of all train for the seasonality is  = " + hyperParameters.getRmsErrorSeasonality());

			String pathTrain = Integer.toString(i + 1) + ".csv";
			String pathValidate = Integer.toString(9) + ".csv";

			ReadCsv obj1 = new ReadCsv(pathTrain);
			final ReadCsv obj2 = new ReadCsv(pathValidate);

			new MultiThreadTrain(obj1.getData(), obj1.getDates(), obj2.getData(), obj2.getDates(), hyperParameters);

			hyperParameters.setEpochTrack(0);
			hyperParameters.setOuterLoopCount(hyperParameters.getOuterLoopCount() + 1);
			Saveobject.save(hyperParameters);
			DeletModels.delet(hyperParameters);
		}

	}

	@Test
	public void test() {
		MultiThreadTest.train();

		fail("Not yet implemented");
	}

}
