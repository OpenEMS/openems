package io.openems.edge.timedata.mssql;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TimedataMssqlTest {

	private static final String CTRL_ID = "mssql0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new TimedataMssqlImpl()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.build())
				.next(new TestCase());
	}

}
