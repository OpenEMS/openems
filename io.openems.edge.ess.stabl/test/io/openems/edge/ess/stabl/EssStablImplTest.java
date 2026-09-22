package io.openems.edge.ess.stabl;

import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_POWER;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.ess.stabl.enums.ActualMainState;
import io.openems.edge.ess.stabl.enums.EssState;
import io.openems.edge.ess.test.DummyPower;

public class EssStablImplTest {

	private static final String COMPONENT_ID = "component0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {

		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final var fetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor(clock, true);

		fetcher.addEndpointHandler(t -> {
			if (t.url().contains("pdist-calculator-stabl") && t.method() == HttpMethod.POST) {
				try {
					System.out.println(" Dummy solver received request: " + t.body());

					var requestJson = JsonUtils.parse(t.body()).getAsJsonObject();
					var observation = requestJson.getAsJsonArray("observation");

					System.out.println(" Solver request - power: " + observation.get(0).getAsDouble() + ", strings: "
							+ (observation.size() - 1));

					var response = """
							 {
							                                           "results": [
							                                             {
							                                               "action": [0.0222, -0.0613],
							                                               "battery_powers": [3.0542, 3.134, 3.453, 3.2935, 3.3333, 3.3732, 3.2137, 3.5327, 3.6125],
							                                               "ranks": [9, 8, 3, 6, 5, 4, 7, 2, 1]

							                                             },
							                                             {
							                                               "action": [0.0263, -0.0436],
							                                               "battery_powers": [3.1443, 3.2113, 3.4358, 3.2664, 3.3333, 3.3609, 3.1914, 3.4948, 3.5617],
							                                               "ranks": [9, 8, 3, 6, 5, 4, 2, 7, 1]
							                                             },
							                                             {
							                                               "action": [0.0217, -0.0607],
							                                               "battery_powers": [3.0104, 3.3124, 3.4633, 3.2746, 3.3124, 3.3501, 3.1991, 3.5011, 3.5766],
							                                               "ranks": [9, 8, 3, 6, 2, 5, 4, 7, 1]
							                                             }
							                                           ]
							                                         }
							""";

					System.out.println("Dummy solver returning pdist weights");
					return HttpResponse.ok(response);

				} catch (Exception e) {
					System.out.println("Solver error: " + e.getMessage());
					e.printStackTrace();
					return HttpResponse.ok("{}");
				}
			}

			System.out.println("Unknown endpoint: " + t.url());
			return HttpResponse.ok("{}");
		});

		// Keep reference to the component instance
		var essStablImpl = new EssStablImpl();
		var sut = new ComponentTest(essStablImpl) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("power", new DummyPower()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofBridgeImpl(//
						() -> fetcher, //
						() -> executor)) //

				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID) //
						.setEssState(EssState.FORCE_TO_START)//
						.setUseExternalSorting(true) //
						.setNumberOfStrings(3)//
						.build());

		sut.next(new TestCase()//
				.input(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_1, 9)
				.input(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_2, 9)
				.input(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_3, 9)//
				.input(EssStabl.ChannelId.ACTUAL_MAIN_STATE, ActualMainState.RUN)//
				.input(ACTIVE_POWER, -1000));

		sut.next(new TestCase());
		sut.next(new TestCase());
		sut.next(new TestCase());

		try {
			essStablImpl.applyPower(-1000, 0); // Charge 1000W
		} catch (Exception e) {
			e.printStackTrace();
		}

		sut.next(new TestCase()//
				.output(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC, -100));
	}
}