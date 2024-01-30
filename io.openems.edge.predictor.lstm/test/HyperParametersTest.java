import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.GetObject;
import io.openems.edge.predictor.lstm.common.HyperParameters;

public class HyperParametersTest {

	@Test
	public void test() {
		HyperParameters hyperParameters;
		try {
			hyperParameters = (HyperParameters) GetObject.get();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Creating new hyperparameter object");
			hyperParameters = HyperParameters.getInstance();
		}
		hyperParameters.printHyperParameters();

		fail("Not yet implemented");
	}

}
