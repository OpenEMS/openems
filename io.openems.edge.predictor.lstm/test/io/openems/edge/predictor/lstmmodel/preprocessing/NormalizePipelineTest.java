package io.openems.edge.predictor.lstmmodel.preprocessing;

import org.junit.Test;

import io.openems.edge.predictor.lstmmodel.common.HyperParameters;
import io.openems.edge.predictor.lstmmodel.preprocessingpipeline.NormalizePipe;

public class NormalizePipelineTest {

	@Test
	public void test() {
		double[] data = { 1.0, 2.0 };
		double[][] data2D = { { 1.0, 2.0 }, { 3.0, 4.0 } };

		var hyperParameters = new HyperParameters();
		var np = new NormalizePipe(hyperParameters);
		np.execute(data);
		var result = (double[][]) np.execute(data2D);
		System.out.print(result[0][0]);

	}
	
	

}


