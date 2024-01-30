import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.edge.predictor.lstm.common.HyperParameters;

public class SingletonHyperParameterMulThrd implements Runnable {
	private HyperParameters hyper;

	public SingletonHyperParameterMulThrd(HyperParameters hyp) {
		this.hyper = hyp;

	}

	@Test
	public void test() {
		HyperParameters hyperParameters = HyperParameters.getInstance();
		var obj = new SingletonHyperParameterMulThrd(hyperParameters);
		obj.run();

		for (int i = 0; i < 30; i++) {

			hyperParameters.setRmsErrorSeasonality(i);

		}
		fail("Not yet implemented");
	}

	@Override
	public void run() {

		for (int i = 0; i < 30; i++) {
			System.out.println(this.hyper.getRmsErrorSeasonality().get(i));
		}

	}

}
