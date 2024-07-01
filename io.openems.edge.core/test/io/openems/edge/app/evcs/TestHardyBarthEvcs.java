package io.openems.edge.app.evcs;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestHardyBarthEvcs {

	private AppManagerTestBundle appManagerTestBundle;

	private HardyBarthEvcs hardyBarthEvcs;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.hardyBarthEvcs = Apps.hardyBarthEvcs(t) //
			);
		});
	}

	@Test
	public void testInstallationAndUpdate() throws Exception {
		final var installResponse = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardyBarthEvcs.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), 1) //
						.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), "192.168.1.30") //
						.build()));

		final var installProps = installResponse.instance().properties;
		final var firstCreatedEvcsId = installProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString();
		final var firstCreatedCtrlEvcsId = installProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString();
		assertTrue(installResponse.warnings() == null || installResponse.warnings().isEmpty());
		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertEquals("evcs0", firstCreatedEvcsId);
		assertEquals("ctrlEvcs0", firstCreatedCtrlEvcsId);
		assertFalse(installProps.has(HardyBarthEvcs.Property.EVCS_ID_CP_2.name()));
		assertFalse(installProps.has(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name()));
		assertFalse(installProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.ALIAS_CP_2.name()));
		assertFalse(installProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name()));

		final var updateToDoubleResponse = this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(installResponse.instance().instanceId, "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), 2) //
								.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), "192.168.1.30") //
								.addProperty(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name(), "192.168.1.31") //
								.addProperty(HardyBarthEvcs.SubPropertySecondChargepoint.ALIAS_CP_2.name(), "alias 2") //
								.build()));

		final var updateDoubleProps = updateToDoubleResponse.instance().properties;
		final var firstCreatedEvcsIdOfSecond = updateDoubleProps.get(HardyBarthEvcs.Property.EVCS_ID_CP_2.name())
				.getAsString();
		final var firstCreatedCtrlEvcsIdOfSecond = updateDoubleProps
				.get(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name()).getAsString();
		assertTrue(updateToDoubleResponse.warnings() == null || updateToDoubleResponse.warnings().isEmpty());
		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertEquals(firstCreatedEvcsId, updateDoubleProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString());
		assertEquals(firstCreatedCtrlEvcsId,
				updateDoubleProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString());
		assertEquals("evcs1", firstCreatedEvcsIdOfSecond);
		assertEquals("ctrlEvcs1", firstCreatedCtrlEvcsIdOfSecond);
		assertNotEquals(updateDoubleProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString(),
				updateDoubleProps.get(HardyBarthEvcs.Property.EVCS_ID_CP_2.name()).getAsString());
		assertNotEquals(updateDoubleProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString(),
				updateDoubleProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name()).getAsString());
		assertTrue(updateDoubleProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.ALIAS_CP_2.name()));
		assertTrue(updateDoubleProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name()));

		final var updateBackToSingleResponse = this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(installResponse.instance().instanceId, "alias",
						JsonUtils.buildJsonObject() //
								.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), 1) //
								.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), "192.168.1.30") //
								.build()));

		final var updateSingleProps = updateBackToSingleResponse.instance().properties;
		assertTrue(updateBackToSingleResponse.warnings() == null || updateBackToSingleResponse.warnings().isEmpty());
		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertEquals(firstCreatedEvcsId, updateSingleProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString());
		assertEquals(firstCreatedEvcsId, updateSingleProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString());
		assertEquals(firstCreatedCtrlEvcsId,
				updateSingleProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString());
	}

	@Test
	public void testInstallationDouble() throws Exception {
		final var installResponse = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardyBarthEvcs.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), 2) //
						.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), "192.168.1.30") //
						.addProperty(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name(), "192.168.1.31") //
						.addProperty(HardyBarthEvcs.SubPropertySecondChargepoint.ALIAS_CP_2.name(), "alias 2") //
						.build()));
		final var updateProps = installResponse.instance().properties;
		assertTrue(installResponse.warnings() == null || installResponse.warnings().isEmpty());
		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertTrue(updateProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString().startsWith("evcs"));
		assertTrue(updateProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString().startsWith("ctrlEvcs"));
		assertTrue(updateProps.get(HardyBarthEvcs.Property.EVCS_ID_CP_2.name()).getAsString().startsWith("evcs"));
		assertTrue(
				updateProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name()).getAsString().startsWith("ctrlEvcs"));
		assertNotEquals(updateProps.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString(),
				updateProps.get(HardyBarthEvcs.Property.EVCS_ID_CP_2.name()).getAsString());
		assertNotEquals(updateProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name()).getAsString(),
				updateProps.get(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name()).getAsString());
		assertTrue(updateProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.ALIAS_CP_2.name()));
		assertTrue(updateProps.has(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name()));
	}

}
