package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestMultipleIds;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestSettingComponentIds {

	private AppManagerTestBundle appManagerTestBundle;

	private TestMultipleIds testMultipleIds;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.testMultipleIds = Apps.testMultipleIds(t) //
			);
		});
	}

	@Test
	public void testSettingInitially() throws Exception {
		final var installResponse = this.add(4);

		final var installProps = installResponse.instance().properties;
		final var initId1 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_1.name());
		final var initId2 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_2.name());
		final var initId3 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_3.name());
		final var initId4 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_4.name());

		assertEquals(4, Sets.newHashSet(initId1, initId2, initId3, initId4).size());
	}

	@Test
	public void testSettingOnUpdate() throws Exception {
		final var installResponse = this.add(2);

		final var installProps = installResponse.instance().properties;
		final var initId1 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_1.name());
		final var initId2 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_2.name());
		assertEquals(2, Sets.newHashSet(initId1, initId2).size());
		assertEquals(3, installProps.size());

		final var updateResponse = this.update(installResponse.instance().instanceId, 4);
		final var updateProps = updateResponse.instance().properties;
		final var updatedId1 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_1.name());
		final var updatedId2 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_2.name());
		final var initId3 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_3.name());
		final var initId4 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_4.name());

		assertEquals(initId1, updatedId1);
		assertEquals(initId2, updatedId2);
		assertEquals(4, Sets.newHashSet(initId1, initId2, initId3, initId4).size());
		assertEquals(5, updateProps.size());
	}

	@Test
	public void testRemoveIds() throws Exception {
		final var installResponse = this.add(4);

		final var installProps = installResponse.instance().properties;
		final var initId1 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_1.name());
		final var initId2 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_2.name());
		final var initId3 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_3.name());
		final var initId4 = JsonUtils.getAsString(installProps, TestMultipleIds.Property.ID_4.name());

		assertEquals(4, Sets.newHashSet(initId1, initId2, initId3, initId4).size());

		final var updateResponse = this.update(installResponse.instance().instanceId, 2);
		final var updateProps = updateResponse.instance().properties;
		final var updatedId1 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_1.name());
		final var updatedId2 = JsonUtils.getAsString(updateProps, TestMultipleIds.Property.ID_2.name());

		assertEquals(initId1, updatedId1);
		assertEquals(initId2, updatedId2);
		assertFalse(updateProps.has(TestMultipleIds.Property.ID_3.name()));
		assertFalse(updateProps.has(TestMultipleIds.Property.ID_4.name()));
	}

	private AddAppInstance.Response add(int setIds) throws Exception {
		return this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.testMultipleIds.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
						.addProperty(TestMultipleIds.Property.SET_IDS.name(), setIds) //
						.build()));
	}

	private UpdateAppInstance.Response update(UUID instanceId, int setIds) throws Exception {
		return this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(instanceId, "alias", JsonUtils.buildJsonObject() //
						.addProperty(TestMultipleIds.Property.SET_IDS.name(), setIds) //
						.build()));
	}

}
