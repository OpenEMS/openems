package io.openems.edge.controller.heatnetwork.master;

import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.rest.remote.device.general.api.RestRemoteChannel;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/*
 * Example JUNit test case
 *
 */

public class HeatNetworkMasterImplTest {

	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String id;
		private final String alias;
		private final boolean enabled;
		private final String service_pid;
		private final String[] requests;
		private final String [] readyResponse;
		private final String allocatedController;
		private final int temperatureSetPoint;

		public MyConfig(String id, String alias, boolean enabled,
						String service_pid, String[] requests, String[] readyResponse, String allocatedController,
						int temperatureSetPoint) {
			super(Config.class, id);
			this.id = id;
			this.alias = alias;
			this.enabled = enabled;
			this.service_pid = service_pid;
			this.requests = requests;
			this.readyResponse = readyResponse;
			this.allocatedController = allocatedController;
			this.temperatureSetPoint = temperatureSetPoint;
		}

		@Override
		public String service_pid() {
			return this.service_pid;
		}
		@Override
		public String[] requests() {
			return this.requests;
		}
		@Override
		public String[] readyResponse() {
			return this.readyResponse;
		}
		@Override
		public String allocatedController() {
			return this.allocatedController;
		}
		@Override
		public int temperatureSetPoint() {
			return this.temperatureSetPoint;
		}
	}

	private static HeatNetworkMasterImpl master;
	private static DummyComponentManager cpm;
	private RestRemoteChannel allocatedRead;
	private RestRemoteChannel allocatedWrite;



	@Test
	public void simple() {
		HeatNetworkMasterImpl impl = new HeatNetworkMasterImpl();
		assertNotNull(impl);
	}

}
