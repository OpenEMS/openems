package io.openems.edge.controller.dynamicdischarge;

import static org.junit.Assert.fail;

import java.time.LocalDateTime;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class DynamicDischargeTest {

	@SuppressWarnings("all")
	private static class MyConfig extends AbstractComponentConfig implements Config {

		private final String url;
		private final String api_Key;
		private final String essId;
		private final String meterId;
		private final int morningHour;
		private final int eveningHour;

		public MyConfig(String id, String url, String api_key, String essID, String meterId, int morningHour,
				int eveningHour) {
			super(Config.class, id);
			this.url = url;
			this.api_Key = api_key;
			this.essId = essID;
			this.meterId = meterId;
			this.morningHour = morningHour;
			this.eveningHour = eveningHour;
		}

		@Test
		public void test() {
			fail("Not yet implemented");
		}

		@Override
		public String url() {
			return this.url;
		}

		@Override
		public String apikey() {
			return this.api_Key;
		}

		@Override
		public String ess_id() {
			return this.essId;
		}

		@Override
		public String meter_id() {
			return this.meterId;
		}

		@Override
		public int Max_Morning_hour() {
			return this.morningHour;
		}

		@Override
		public int Max_Evening_hour() {
			return this.eveningHour;
		}
	}

	@Test
	public void test() throws Exception {

		Integer[] productionValues = { 0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0 };
		Integer[] consumptionValues = { 0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0, 0, //
				0, 0, 0, 0 };
		LocalDateTime startHour = LocalDateTime.now();

		// Initialize the controller
		// DynamicDischarge controller = new DynamicDischarge();

		DynamicDischarge controller = new DynamicDischarge(productionValues, consumptionValues, startHour);
		DummyComponentManager componentManager = new DummyComponentManager();
		controller.componentManager = componentManager;

		ChannelAddress ess0 = new ChannelAddress("ess0", "Soc");
		ChannelAddress essCapacity = new ChannelAddress("ess0", "Capacity");
		ChannelAddress ctrlChannel = new ChannelAddress("ctrl1", "SelfOptimized");

		MyConfig myconfig = new MyConfig("ctrl1", "https://api.awattar.com/v1/marketdata",
				"ak_7YTR42jBwtnk5kXuMZRYEju8hvj918H0", "ess0", "meter0", 8, 17);
		controller.activate(null, myconfig);
		controller.activate(null, myconfig);

		ManagedSymmetricEss ess = new DummyManagedSymmetricEss("ess0");

		// Build and run test
		new ControllerTest(controller, componentManager, ess).next(new TestCase() //
				.input(ess0, 14) //
				.input(essCapacity, 1000) //
				.output(ctrlChannel, true)) //
				.run();

	}
}
