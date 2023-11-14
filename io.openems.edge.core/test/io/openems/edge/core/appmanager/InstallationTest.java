package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public class InstallationTest {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	@Test
	public void testIncompatibleAppInstallation() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setValidatorConfig(ValidatorConfig.create() //
						.setCompatibleCheckableConfigs(DummyValidator.testCheckable(() -> false)) //
						.build())
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			OpenemsException exception = null;
			try {
				appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
						new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
								.build()))
						.get();
			} catch (OpenemsException e) {
				exception = e;
			}
			assertNotNull(exception);
			assertTrue(appManagerTestBundle.sut.getInstantiatedApps().isEmpty());
		});
	}

	@Test
	public void testCompatibleAppInstallation() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setValidatorConfig(ValidatorConfig.create() //
						.setCompatibleCheckableConfigs(DummyValidator.testCheckable(() -> true)) //
						.build())
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			final var response = appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
					new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
							.build()))
					.get();

			assertNotNull(appManagerTestBundle.sut.getInstantiatedApps()
					.get(appManagerTestBundle.sut.getInstantiatedApps().indexOf(response.instance)));
		});
	}

	@Test
	public void testNotInstallableAppInstallation() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setValidatorConfig(ValidatorConfig.create() //
						.setInstallableCheckableConfigs(DummyValidator.testCheckable(() -> false)).build())
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			OpenemsException exception = null;
			try {
				appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
						new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
								.build()))
						.get();
			} catch (OpenemsException e) {
				exception = e;
			}
			assertNotNull(exception);
			assertTrue(appManagerTestBundle.sut.getInstantiatedApps().isEmpty());
		});
	}

	@Test
	public void testInstallableAppInstallation() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setValidatorConfig(ValidatorConfig.create() //
						.setInstallableCheckableConfigs(DummyValidator.testCheckable(() -> true)) //
						.build())
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			final var response = appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
					new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
							.build()))
					.get();

			assertNotNull(appManagerTestBundle.sut.getInstantiatedApps()
					.get(appManagerTestBundle.sut.getInstantiatedApps().indexOf(response.instance)));
		});
	}

	@Test
	public void testAppInstallationWithExceptionalConfiguration() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setConfiguration((t, p, l) -> {
					throw new OpenemsException("Configuration error");
				}) //
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			OpenemsException exception = null;
			try {
				appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
						new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
								.build()))
						.get();
			} catch (OpenemsException e) {
				exception = e;
			}
			assertNotNull(exception);
			assertTrue(appManagerTestBundle.sut.getInstantiatedApps().isEmpty());
		});
	}

	@Test
	public void testAppUpdateWithExceptionalConfiguration() throws Exception {
		final var dummyApp = DummyApp.create() //
				.setConfiguration((t, p, l) -> {
					return switch (t) {
					case UPDATE -> throw new OpenemsException("Configuration error");
					default -> AppConfiguration.empty();
					};
				}) //
				.build();
		singleAppTest(dummyApp, (appManagerTestBundle, app) -> {
			final var response = appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
					new AddAppInstance.Request(app.getAppId(), "key", "alias", JsonUtils.buildJsonObject() //
							.build()))
					.get();

			final var instance = appManagerTestBundle.sut.getInstantiatedApps()
					.get(appManagerTestBundle.sut.getInstantiatedApps().indexOf(response.instance));

			assertNotNull(instance);

			OpenemsException exception = null;
			try {
				appManagerTestBundle.sut.handleUpdateAppInstanceRequest(this.user,
						new UpdateAppInstance.Request(instance.instanceId, "alias", JsonUtils.buildJsonObject() //
								.build()))
						.get();
			} catch (OpenemsException e) {
				exception = e;
			}
			assertNotNull(exception);
			assertEquals(1, appManagerTestBundle.sut.getInstantiatedApps().size());
		});
	}

	private static void singleAppTest(//
			final OpenemsApp app, //
			final ThrowingBiConsumer<AppManagerTestBundle, OpenemsApp, Exception> test //
	) throws Exception {
		final var appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(app);
		}, null, new PseudoComponentManagerFactory());

		assertTrue(appManagerTestBundle.sut.findAppById(app.getAppId()).isPresent());
		test.accept(appManagerTestBundle, app);
	}

}
