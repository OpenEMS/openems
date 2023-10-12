package io.openems.edge.predictor.lstm;

import org.junit.Test;

public class GetModelTest {

	@Test
	public void test() {
		var path = this.getClass().getResource("SavedModel.txt").getFile();
		System.out.println(path);
	}

}
