package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class ResolveOpenemsHardwareTest {

	@Rule
	public TemporaryFolder folder = TemporaryFolder.builder() //
			.assureDeletion() //
			.build();

	private AppManagerTestBundle appManagerTestBundle;
	private OpenemsApp dummyHardwareApp;
	private OpenemsApp dummyHardwareApp2;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.dummyHardwareApp = Apps.techbaseCm4(t), //
					this.dummyHardwareApp2 = Apps.techbaseCm3(t) //
			);
		});
		System.setProperty("felix.cm.dir", this.folder.getRoot().getPath());
	}

	@Test
	public void testSuccessfulInstallation() throws Exception {
		this.setHardwareApp(this.dummyHardwareApp.getAppId());

		final var completed = new CompletableFuture<Void>();
		final var context = new DummyComponentContext(new Hashtable<String, Object>(), DummyComponentInstance.create() //
				.setDispose(() -> completed.complete(null)) //
				.build());

		final var testExecutor = new TestExecutor();
		final var resolver = new ResolveOpenemsHardware(context, this.appManagerTestBundle.sut,
				this.appManagerTestBundle.appManagerUtil, testExecutor);

		resolver.bindApp(this.dummyHardwareApp);
		resolver.bindApp(this.dummyHardwareApp2);
		testExecutor.runAll();

		completed.join();

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertFalse(this.appManagerTestBundle.sut.getHardwareMissmatchChannel().getNextValue().get());
	}

	@Test
	public void testNotExistingAppId() throws Exception {
		this.setHardwareApp("Random.App.Id");

		final var completed = new CompletableFuture<Void>();
		final var context = new DummyComponentContext(new Hashtable<String, Object>(), DummyComponentInstance.create() //
				.setDispose(() -> completed.complete(null)) //
				.build());

		final var testExecutor = new TestExecutor();
		final var resolver = new ResolveOpenemsHardware(context, this.appManagerTestBundle.sut,
				this.appManagerTestBundle.appManagerUtil, testExecutor);

		resolver.bindApp(this.dummyHardwareApp);
		resolver.bindApp(this.dummyHardwareApp2);
		testExecutor.runAll();

		completed.join();

		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertTrue(this.appManagerTestBundle.sut.getHardwareMissmatchChannel().getNextValue().get());
	}

	@Test
	public void testAppIdNotSet() throws Exception {
		final var completed = new CompletableFuture<Void>();
		final var context = new DummyComponentContext(new Hashtable<String, Object>(), DummyComponentInstance.create() //
				.setDispose(() -> completed.complete(null)) //
				.build());

		final var testExecutor = new TestExecutor();
		final var resolver = new ResolveOpenemsHardware(context, this.appManagerTestBundle.sut,
				this.appManagerTestBundle.appManagerUtil, testExecutor);

		resolver.bindApp(this.dummyHardwareApp);
		resolver.bindApp(this.dummyHardwareApp2);
		testExecutor.runAll();

		completed.join();

		assertEquals(0, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertFalse(this.appManagerTestBundle.sut.getHardwareMissmatchChannel().getNextValue().get());
	}

	@Test
	public void testWrongHardwareAppInstalled() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.dummyHardwareApp2.getAppId(), "key", null, new JsonObject()));

		this.setHardwareApp(this.dummyHardwareApp.getAppId());

		final var completed = new CompletableFuture<Void>();
		final var context = new DummyComponentContext(new Hashtable<String, Object>(), DummyComponentInstance.create() //
				.setDispose(() -> completed.complete(null)) //
				.build());

		final var testExecutor = new TestExecutor();
		final var resolver = new ResolveOpenemsHardware(context, this.appManagerTestBundle.sut,
				this.appManagerTestBundle.appManagerUtil, testExecutor);

		resolver.bindApp(this.dummyHardwareApp);
		resolver.bindApp(this.dummyHardwareApp2);
		testExecutor.runAll();

		completed.join();

		assertEquals(1, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertTrue(this.appManagerTestBundle.sut.getHardwareMissmatchChannel().getNextValue().get());
	}

	private void setHardwareApp(String appId) throws Exception {
		final var hardwareInfoFile = this.folder.newFile(ResolveOpenemsHardware.OPENEMS_HARDWARE_FILE_NAME);
		try (final var fw = new FileWriter(hardwareInfoFile)) {
			fw.write(ResolveOpenemsHardware.OPENEMS_HARDWARE_APP_KEY + "=" + appId);
		}
	}

	private static class TestExecutor implements Executor {

		private final List<Runnable> tasks = new ArrayList<>();

		@Override
		public void execute(Runnable command) {
			this.tasks.add(command);
		}

		public void runAll() {
			this.tasks.forEach(Runnable::run);
			this.tasks.clear();
		}

	}

}
