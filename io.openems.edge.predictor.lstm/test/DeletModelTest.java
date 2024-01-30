import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.DeletModels;
import io.openems.edge.predictor.lstm.common.GetObject;
import io.openems.edge.predictor.lstm.common.HyperParameters;

public class DeletModelTest {

	/**
	 * Attempts to retrieve a HyperParameters instance using GetObject. If
	 * successful, the retrieved instance is stored in the local 'hyp' variable. If
	 * an exception occurs (ClassNotFoundException or IOException) during retrieval,
	 * an error stack trace is printed, and the 'hyp' variable remains unchanged. *
	 * 
	 */
	public static void del() {
		HyperParameters hyp = HyperParameters.getInstance();
		try {
			hyp = (HyperParameters) GetObject.get();
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DeletModels.delet(hyp);
		System.out.println("File deleated");

	}

	@Test
	public void test() {
		DeletModelTest.del();
		fail("Not yet implemented");
	}

}
