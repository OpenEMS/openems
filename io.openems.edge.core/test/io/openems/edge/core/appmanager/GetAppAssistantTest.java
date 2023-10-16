package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;

public class GetAppAssistantTest {
	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle testBundle;

	@Before
	public void before() throws Exception {
		this.testBundle = new AppManagerTestBundle(null, null, t -> {
			return Apps.of(t, //
					Apps::feneconHome, //
					Apps::awattarHourly, //
					Apps::stromdaoCorrently, //
					Apps::tibber, //
					Apps::modbusTcpApiReadOnly, //
					Apps::modbusTcpApiReadWrite, //
					Apps::restJsonApiReadOnly, //
					Apps::hardyBarthEvcs, //
					Apps::kebaEvcs, //
					Apps::iesKeywattEvcs, //
					Apps::evcsCluster, //
					Apps::heatPump, //
					Apps::gridOptimizedCharge, //
					Apps::selfConsumptionOptimization, //
					Apps::socomecMeter, //
					Apps::prepareBatteryExtension //
			);
		});
	}

	@Test
	public void testGetAppAssistantAll() {
		this.testBundle.sut.availableApps.forEach(this::testGetAppAssistant);
	}

	private void testGetAppAssistant(OpenemsApp app) {
		final var appAssistant = app.getAppAssistant(this.user);
		assertNotNull(appAssistant);
		assertNotNull(appAssistant.alias);
		assertNotNull(appAssistant.name);
		assertNotNull(appAssistant.fields);
		assertTrue(appAssistant.fields.isJsonArray());
	}

}
