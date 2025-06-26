package io.openems.edge.core.appmanager;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

@Component(immediate = true, scope = ServiceScope.SINGLETON)
public class ResolveOpenemsHardware implements Runnable {

	public static final String OPENEMS_HARDWARE_FILE_NAME = "hardware.conf";
	public static final String OPENEMS_HARDWARE_APP_KEY = "appId";

	private final Logger log = LoggerFactory.getLogger(ResolveOpenemsHardware.class);

	private final ComponentContext context;
	private final AppManagerImpl appManagerImpl;
	private final AppManagerUtil appManagerUtil;
	private final String requireApp;
	private CompletableFuture<Void> task;
	private final Executor delayedExecutor;

	/**
	 * Binds a {@link OpenemsApp}.
	 * 
	 * @param app the {@link OpenemsApp} to bind
	 */
	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	public void bindApp(OpenemsApp app) {
		this.trigger();
	}

	/**
	 * Unbinds a {@link OpenemsApp}.
	 * 
	 * @param app the {@link OpenemsApp} to unbind
	 */
	public void unbindApp(OpenemsApp app) {
		// empty
	}

	public ResolveOpenemsHardware(//
			ComponentContext context, //
			AppManager appManagerImpl, //
			AppManagerUtil appManagerUtil, //
			Executor delayedExecutor //
	) {
		super();
		this.context = context;
		this.appManagerImpl = (AppManagerImpl) appManagerImpl;
		this.appManagerUtil = appManagerUtil;
		this.delayedExecutor = delayedExecutor;

		final var hardwareProperties = this.getHardwareProperties();
		this.requireApp = hardwareProperties.get(OPENEMS_HARDWARE_APP_KEY);

		this.trigger();
	}

	@Activate
	public ResolveOpenemsHardware(//
			ComponentContext context, //
			@Reference AppManager appManagerImpl, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		this(context, appManagerImpl, appManagerUtil, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS));
	}

	@Deactivate
	private void deactivate() {
		final var task = this.task;
		if (task != null) {
			task.cancel(false);
		}
	}

	private synchronized void trigger() {
		if (this.requireApp == null) {
			this.task = CompletableFuture.runAsync(this.context.getComponentInstance()::dispose);
			return;
		}

		final var activeTask = this.task;
		if (activeTask != null) {
			activeTask.cancel(false);
		}

		// waits 5 seconds until every app is activated
		this.task = CompletableFuture.runAsync(() -> {
			try {
				this.run();
			} finally {
				this.context.getComponentInstance().dispose();
			}
		}, this.delayedExecutor);
	}

	@Override
	public void run() {
		// currently its ignored if the app of an installed hardware app is not yet
		// active or available after a 5 seconds delay from the last activated app and
		// would therefore not be returned by the following method which could result in
		// 2 hardware apps being installed
		final var hardwareApps = this.appManagerUtil
				.getInstantiatedAppsByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

		if (hardwareApps.size() >= 1) {
			// validate
			if (hardwareApps.size() > 1) {
				// more than 1 installed => impossible
				this.appManagerImpl._setHardwareMissmatch(true);
				return;
			}

			final var installedHardwareApp = hardwareApps.get(0);
			if (installedHardwareApp.appId.equals(this.requireApp)) {
				// installed hardware app matches the on in the properties file
				this.appManagerImpl._setHardwareMissmatch(false);
				return;
			}

			this.appManagerImpl._setHardwareMissmatch(true);
			return;
		}

		this.log.trace("Try to install '" + this.requireApp + "'");
		try {
			this.appManagerImpl.handleAddAppInstanceRequest(null,
					new AddAppInstance.Request(this.requireApp, null, null, JsonUtils.buildJsonObject() //
							.build()),
					true);

			this.log.trace("Installed '" + this.requireApp + "' successfully");
			this.appManagerImpl._setHardwareMissmatch(false);
		} catch (Exception e) {
			this.log.error("Installation of '" + this.requireApp + "' failed", e);
			this.appManagerImpl._setHardwareMissmatch(true);
		}

	}

	private Map<String, String> getHardwareProperties() {
		final var configDir = System.getProperty("felix.cm.dir");
		if (configDir == null) {
			return emptyMap();
		}
		final var hardwareFile = new File(configDir, OPENEMS_HARDWARE_FILE_NAME);

		if (!hardwareFile.exists()) {
			return emptyMap();
		}

		try {
			return Files.lines(hardwareFile.toPath()) //
					.map(t -> t.split("=", 2)) //
					.filter(t -> t.length == 2) //
					.collect(toMap(t -> t[0], t -> t[1]));

		} catch (Exception e) {
			this.log.error("Unable to read hardware info file", e);
			return emptyMap();
		}
	}

}
