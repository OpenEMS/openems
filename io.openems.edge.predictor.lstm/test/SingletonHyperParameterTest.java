import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class SingletonHyperParameterTest {
	/**
	 * A method to test the behavior of the HyperParameters singleton. It
	 * demonstrates the usage of HyperParameters.getInstance(), sets a count value
	 * on one instance, and retrieves the count value from another instance to
	 * verify the singleton pattern.
	 */
	public static void hypTest() {

		HyperParameters obj = HyperParameters.getInstance();
		obj.setCount(200);
		HyperParameters obj1 = HyperParameters.getInstance();
		System.out.println(obj1.getCount());
	}

	@Test
	public void test() {
		SingletonHyperParameterTest.hypTest();
		fail("Not yet implemented");
	}

}
