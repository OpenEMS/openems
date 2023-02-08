package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;

public class ResolveDependenciesTest {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle testBundle;

	@Before
	public void before() throws Exception {
		this.testBundle = new AppManagerTestBundle(//
				null, MyConfig.create() //
						.setApps(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("appId", "App.FENECON.Home") //
										.addProperty("alias", "FENECON Home") //
										.addProperty("instanceId", UUID.randomUUID().toString()) //
										.add("properties", JsonUtils.buildJsonObject() //
												.addProperty("SAFETY_COUNTRY", "GERMANY") //
												.addProperty("MAX_FEED_IN_POWER", 9450) //
												.addProperty("FEED_IN_SETTING", "LEADING_0_95") //
												.addProperty("HAS_AC_METER", false) //
												.addProperty("HAS_DC_PV1", false) //
												.addProperty("HAS_DC_PV2", false) //
												.addProperty("HAS_EMERGENCY_RESERVE", false) //
												.build()) //
										.add("dependencies", JsonUtils.buildJsonArray() //
												// No dependencies they should be installed with the resolver
												.build()) //
										.build())
								.build() //
								.toString())
						.build(),
				t -> {
					return Apps.of(t, //
							Apps::feneconHome, //
							Apps::gridOptimizedCharge, //
							Apps::selfConsumptionOptimization, //
							Apps::prepareBatteryExtension //
					);
				});
	}

	@Test
	public void testResolveDependencies() {
		assertEquals(1, this.testBundle.sut.getInstantiatedApps().size());
		ResolveDependencies.resolveDependencies(this.user, this.testBundle.sut, this.testBundle.appManagerUtil);
		assertEquals(4, this.testBundle.sut.getInstantiatedApps().size());
	}

}
