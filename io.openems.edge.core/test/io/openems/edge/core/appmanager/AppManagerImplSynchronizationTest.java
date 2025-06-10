package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;

public class AppManagerImplSynchronizationTest {

	private AppManagerImpl appManager;

	@Before
	public void before() throws Exception {
		final var appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					Apps.kebaEvcs(t), //
					Apps.solarEdgePvInverter(t) //
			);
		}, null, new PseudoComponentManagerFactory(), new AppManagerImpl());

		this.appManager = appManagerTestBundle.sut;

		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testInstallationOfNotAvailableApp() throws Exception {
		assertThrows(OpenemsNamedException.class, () -> {
			this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
					new AddAppInstance.Request("someAppId", "key", "alias", JsonUtils.buildJsonObject() //
							.build()));
		});

		// if app is not existing no need to modify
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testRemoveOfNotAvailableInstance() throws Exception {
		this.appManager.handleDeleteAppInstanceRequest(DUMMY_ADMIN, new DeleteAppInstance.Request(UUID.randomUUID()));

		// if instance is not existing no need to modify
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.lockModifyingApps.unlock();
		assertFalse(this.appManager.waitingForModified);
	}

	@Test
	public void testSimulateAfterInstallation() throws Exception {
		assertTrue(this.appManager.lockModifyingApps.tryLock());
		this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias", JsonUtils.buildJsonObject() //
						.build()));

		assertTrue(this.appManager.waitingForModified);
		assertFalse(this.appManager.waitingForModifiedCondition.await(1, TimeUnit.MILLISECONDS));
		this.appManager.lockModifyingApps.unlock();
	}

	@Test
	@Ignore
	public void testSimulateLockWaitingForModification() throws Exception {
		this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias", JsonUtils.buildJsonObject() //
						.build()));

		assertTrue(this.appManager.waitingForModified);

		final var second = CompletableFuture.supplyAsync(() -> {
			try {
				return this.appManager.handleAddAppInstanceRequest(DUMMY_ADMIN,
						new AddAppInstance.Request("App.PvInverter.SolarEdge", "key", "alias",
								JsonUtils.buildJsonObject() //
										.build()));
			} catch (OpenemsNamedException e) {
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
