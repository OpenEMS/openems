package io.openems.edge.core.appmanager.validator.relaycount;

import static io.openems.edge.app.common.props.RelayProps.emptyFilter;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.openemshardware.TechbaseCm4sGen2;
import io.openems.edge.app.openemshardware.TechbaseCm4sGen3;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.io.test.DummyCustomInputOutput;

class TechbaseCm4sGen3FilterTest {

	private AppManagerTestBundle test;
	private TechbaseCm4sGen3Filter techbaseCm4sGen3Filter;

	@BeforeEach
	void setUp() throws Exception {
		this.test = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					Apps.techbaseCm4sGen3(t), //
					Apps.techbaseCm4sGen2(t), //
					Apps.masterBox2v0(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		this.techbaseCm4sGen3Filter = new TechbaseCm4sGen3Filter();
		this.test.addComponentAggregateTask();
	}

	@Test
	void testApplyWithGen2() throws OpenemsError.OpenemsNamedException {
		this.test.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(TechbaseCm4sGen2.APPID, "key",
				"alias", JsonUtils.buildJsonObject().build()));

		var techbaseGen2AppInstance = this.test.appManagerUtil.getInstantiatedAppsOfApp(TechbaseCm4sGen2.APPID)
				.getFirst();

		this.techbaseCm4sGen3Filter.setProperties(Map.of("onlyHighVoltageRelays", true, //
				"deviceHardware", techbaseGen2AppInstance //
		));
		var relayContactFilter = this.techbaseCm4sGen3Filter.apply();

		assertEquals(emptyFilter(), relayContactFilter);
	}

	@Test
	void testApplyWithGen3() throws OpenemsError.OpenemsNamedException {
		this.test.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(TechbaseCm4sGen3.APPID, "key",
				"alias", JsonUtils.buildJsonObject().build()));

		var techbaseGen3AppInstance = this.test.appManagerUtil.getInstantiatedAppsOfApp(TechbaseCm4sGen3.APPID)
				.getFirst();

		this.techbaseCm4sGen3Filter.setProperties(Map.of("onlyHighVoltageRelays", true, //
				"deviceHardware", techbaseGen3AppInstance //
		));

		var relayContactFilter = this.techbaseCm4sGen3Filter.apply();
		DigitalOutput masterboxRelayDummy = new DummyCustomInputOutput("io0", "RELAY", 1, 6);

		assertEquals(List.of("nicht Freigegeben"),
				relayContactFilter.disabledReasons().apply(masterboxRelayDummy, masterboxRelayDummy.channel("Relay4")));

		assertFalse(
				relayContactFilter.channelFilter().test(masterboxRelayDummy, masterboxRelayDummy.channel("Relay6")));
	}

}
