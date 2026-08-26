package $basePackageName$;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new MyControllerImpl()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
