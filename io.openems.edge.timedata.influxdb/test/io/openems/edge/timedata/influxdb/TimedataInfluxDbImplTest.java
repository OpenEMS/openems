package io.openems.edge.timedata.influxdb;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;
import io.openems.shared.influxdb.QueryLanguageConfig;

public class TimedataInfluxDbImplTest {

	private static final String COMPONENT_ID = "influx0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new TimedataInfluxDbImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cycle", new DummyCycle(1000)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setQueryLanguage(QueryLanguageConfig.INFLUX_QL) //
						.setUrl("http://localhost:8086") //
						.setOrg("-") //
						.setApiKey("username:password") //
						.setBucket("database/retentionPolicy") //
						.setMeasurement("data") //
						.setNoOfCycles(1) //
						.setMaxQueueSize(5000) //
						.setReadOnly(false) //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
