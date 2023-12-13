import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class HyperparmTest {
	
 /**
 * Test case.
 *  
 */
	
	public void hyp() {
		HyperParameters hp = new HyperParameters();
		hp.setLearningRateUpperLimit(0);
		hp.setLearningLowerLimit(0);
		hp.setWiInit(0);
		hp.setWoInit(0);
		hp.setWzInit(0);
		hp.setriInit(0);
		hp.setRoInit(0);
		hp.setRzInit(0);
		hp.setYtInit(1);
		hp.setCtInit(1);
		hp.printHyperParameters();
		
		
	}

	@Test
	public void test() {
		HyperparmTest hpt = new HyperparmTest();
		hpt.hyp();
			

		fail("Not yet implemented");
	}

}
