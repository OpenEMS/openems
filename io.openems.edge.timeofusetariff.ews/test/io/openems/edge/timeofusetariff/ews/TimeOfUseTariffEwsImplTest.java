package io.openems.edge.timeofusetariff.ews;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.currency.Currency.EUR;
import static io.openems.edge.timeofusetariff.ews.Data.JSON_DATA;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.common.channel.Level;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

public class TimeOfUseTariffEwsImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		final var dummyMeta = new DummyMeta() //
				.withCurrency(EUR);
		final var sut = new TimeOfUseTariffEwsImpl();
		new ComponentTest(sut) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("httpBridgeFactory",
						DummyBridgeHttpFactory.ofBridgeImpl(DummyBridgeHttpFactory::dummyEndpointFetcher,
								DummyBridgeHttpFactory::dummyBridgeHttpExecutor)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEnabled(false) //
						.setAccessToken("foo-bar") //
						.setLogVerbosity(LogVerbosity.NONE) //
						.build()) //
				.next(new TestCase() //
						.activateStrictMode() //
						.onBeforeProcessImage(() -> sut.handleResponse(HttpResponse.ok(JSON_DATA))) //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(TimeOfUseTariffEws.ChannelId.HTTP_STATUS_CODE, 200) //
						.output(TimeOfUseTariffEws.ChannelId.STATUS_AUTHENTICATION_FAILED, false) //
						.output(TimeOfUseTariffEws.ChannelId.STATUS_SERVER_ERROR, false)) //
				.deactivate();
	}
}
