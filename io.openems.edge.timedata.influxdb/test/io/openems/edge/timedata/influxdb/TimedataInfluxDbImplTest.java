package io.openems.edge.timedata.influxdb;

import static io.openems.common.channel.PersistencePriority.MEDIUM;
import static io.openems.shared.influxdb.QueryLanguageConfig.INFLUX_QL;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyCycle;

public class TimedataInfluxDbImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TimedataInfluxDbImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cycle", new DummyCycle(1000)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.activate(MyConfig.create() //
						.setId("influx0") //
						.setQueryLanguage(INFLUX_QL) //
						.setUrl("http://localhost:8086") //
						.setOrg("-") //
						.setApiKey("username:password") //
						.setBucket("database/retentionPolicy") //
						.setMeasurement("data") //
						.setNoOfCycles(1) //
						.setMaxQueueSize(5000) //
						.setReadOnly(false) //
						.setPersistencePriority(MEDIUM) //
						.build()) //
				.next(new TestCase()) //
		;
	}

}
