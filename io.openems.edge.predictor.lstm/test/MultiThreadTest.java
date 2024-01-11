import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;
import io.openems.edge.predictor.lstm.common.ReadCsv;
import io.openems.edge.predictor.lstm.muliithread.MultiThreadTrain;

public class MultiThreadTest {

	public static void train() {

		int k = 0;

		HyperParameters hyperParameters = new HyperParameters();
		for (int i = 0; i < 28; i++) {
			System.out.println("Batch:" + i + "/" + 28);

			String pathTrain = Integer.toString(i + 1) + ".csv";
			String pathValidate = Integer.toString(29) + ".csv";

			
				hyperParameters.setCount(k);
				

				ReadCsv obj1 = new ReadCsv(pathTrain);
				final ReadCsv obj2 = new ReadCsv(pathValidate);
				
				new MultiThreadTrain(obj1.getData(), obj1.getDates(),obj2.getData(),obj2.getDates(),hyperParameters);
				

			
		}

	}

	@Test
	public void test() {
		MultiThreadTest.train();
		
		fail("Not yet implemented");
	}

}
