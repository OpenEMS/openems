package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.AppManagerTestBundle.CheckablesBundle;
import io.openems.edge.core.appmanager.DummyValidator.TestCheckable;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelper;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.validator.CheckAppsNotInstalled;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.relaycount.CheckRelayCount;

public class AppManagerImpSynchronizationTest {

	private AppManagerImpl appManager;

	@Before
	public void before() throws Exception {
		this.appManager = new AppManagerImpl();
		ReflectionUtils.setAttribute(AppManagerImpl.class, this.appManager, "appValidateWorker",
				new AppValidateWorker());
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);

		final var cm = new DummyConfigurationAdmin();
		final var componentManager = new DummyComponentManager();
		componentManager.setConfigJson(JsonUtils.buildJsonObject() //
				.add("components", JsonUtils.buildJsonObject() //
						.add("scheduler0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Scheduler.AllAlphabetically") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.add("controllers.ids", JsonUtils.buildJsonArray() //
												.build()) //
										.build()) //
								.build())
						.build())
				.add("factories", JsonUtils.buildJsonObject() //
						.build())
				.build());
		componentManager.setConfigurationAdmin(cm);

		// create config for scheduler
		cm.getOrCreateEmptyConfiguration(componentManager.getEdgeConfig().getComponent("scheduler0").get().getPid());
		final var componentUtil = new ComponentUtilImpl(componentManager);
		final var appManagerUtil = new AppManagerUtilImpl(componentManager);
		final var validator = new DummyValidator();

		final var checkablesBundle = new CheckablesBundle(//
				new TestCheckable(), //
				new CheckCardinality(this.appManager, appManagerUtil,
						AppManagerTestBundle.getComponentContext(CheckCardinality.COMPONENT_NAME)), //
				new CheckRelayCount(componentUtil,
						AppManagerTestBundle.getComponentContext(CheckRelayCount.COMPONENT_NAME), null), //
				new CheckAppsNotInstalled(this.appManager,
						AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME)), //
				new CheckHome(this.appManager.componentManager,
						AppManagerTestBundle.getComponentContext(CheckHome.COMPONENT_NAME),
						new CheckAppsNotInstalled(this.appManager,
								AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME))) //
		);

		validator.setCheckables(checkablesBundle.all());

		new ComponentTest(this.appManager) //
				.addReference("cm", cm) //
				.addReference("componentManager", componentManager) //
				.addReference("csoAppManagerAppHelper",
						AppManagerTestBundle.<AppManagerAppHelper>cso(
								new DummyAppManagerAppHelper(componentManager, componentUtil, appManagerUtil))) //
				.addReference("validator", validator) //
				.addReference("backendUtil", new DummyAppCenterBackendUtil()) //
				.addReference("availableApps", Lists.newArrayList(//
						new KebaEvcs(componentManager,
								AppManagerTestBundle.getComponentContext("App.PvInverter.SolarEdge"), cm, componentUtil) //
				)) //
				.activate(MyConfig.create() //
						.setApps("[]") //
						.build());

		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testInstallationOfNotAvailableApp() throws Exception {
		try {
			this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
					new AddAppInstance.Request("someAppId", "key", "alias", JsonUtils.buildJsonObject() //
							.build()));
		} catch (OpenemsNamedException e) {
			// expected
		}

		// if app is not existing no need to modify
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testRemoveOfNotAvailableInstance() throws Exception {
		try {
			this.appManager.handleDeleteAppInstanceRequest(DUMMY_ADMIN,
					new DeleteAppInstance.Request(UUID.randomUUID()));
		} catch (OpenemsNamedException e) {
			// expected
		}

		// if instance is not existing no need to modify
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testSimulateAfterInstallaion() throws Exception {
		this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias", JsonUtils.buildJsonObject() //
						.build()))
				.get();

		assertTrue(this.appManager.lockModifyingApps.tryLock());
		assertTrue(this.appManager.waitingForModified);
		assertFalse(this.appManager.waitingForModifiedCondition.await(1, TimeUnit.MILLISECONDS));
		this.appManager.lockModifyingApps.unlock();
	}

	@Test
	@Ignore
	public void testSimulateLockWaitingForModification() throws Exception {
		this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias", JsonUtils.buildJsonObject() //
						.build()))
				.get();

		assertTrue(this.appManager.waitingForModified);

		final var second = CompletableFuture.supplyAsync(() -> {
			try {
				return this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
						new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias",
								JsonUtils.buildJsonObject() //
										.build()))
						.get();
			} catch (InterruptedException | ExecutionException | OpenemsNamedException e) {
				throw new RuntimeException(e);
			}
		});

		Thread.sleep(5000);
		assertFalse(second.isDone());

		this.appManager.modified(new DummyComponentContext(), MyConfig.create() //
				.setApps("[]") //
				.build());

		Thread.sleep(5000);
		assertTrue(second.isDone());
	}

	@Test
	public void testSimulateBeforeModified() throws Exception {
		// simulate after an instance got created and the configuration was requested to
		// update
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.waitingForModified = true;
		this.appManager.lockModifyingApps.unlock();

		this.appManager.modified(new DummyComponentContext(), MyConfig.create() //
				.setApps("[]") //
				.build());

		assertTrue(this.appManager.lockModifyingApps.tryLock());
		assertFalse(this.appManager.waitingForModified);
	}

}
