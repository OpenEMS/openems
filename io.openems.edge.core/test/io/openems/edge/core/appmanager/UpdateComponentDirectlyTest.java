package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppConfig;

public class UpdateComponentDirectlyTest {
	private AppManagerTestBundle appManagerTestBundle;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//

			);
		}, null, new PseudoComponentManagerFactory());
	}

	@Test
	public void testAdminOnlyAsInstaller() throws OpenemsNamedException {
		final var testTest = List.of(new UpdateComponentConfigRequest.Property("id", "evcs1"),
				new UpdateComponentConfigRequest.Property("debugMode", false));
		this.appManagerTestBundle.componentManger.handleCreateComponentConfigRequest(DUMMY_ADMIN,
				new CreateComponentConfig.Request("Evcs.Keba.KeContact", testTest));
		this.appManagerTestBundle.sut.handleUpdateAppConfigRequest(DUMMY_ADMIN,
				new UpdateAppConfig.Request("evcs1", JsonUtils.buildJsonObject().addProperty("debugMode", true)//
						.build()));
		final var properties = JsonUtils.buildJsonObject()//
				.addProperty("debugMode", true)//
				.build();
		this.appManagerTestBundle
				.assertComponentExist(new EdgeConfig.Component("evcs1", "", "Evcs.Keba.KeContact", properties));

	}
}
