package io.openems.edge.core.appmanager;

import static io.openems.common.utils.DebugUtils.measure;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.CancellationToken;
import io.openems.common.worker.AbstractWorker;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.dependency.AppConfigValidator;

/**
 * This Worker constantly validates:.
 *
 * <ul>
 * <li>that all enabled OpenEMS Apps are properly configured, including required
 * OpenEMS Components, IP addresses, Scheduler settings, etc.
 * </ul>
 */
@Component(//
		service = { ConfigurationListener.class, AppValidateWorker.class } //
)
public class AppValidateWorker extends AbstractWorker implements ConfigurationListener {

	public record Config(Consumer<Boolean> setDefectiveApp) {

	}

	/*
	 * For INITIAL_CYCLES cycles the distance between two checks is
	 * INITIAL_CYCLE_TIME, afterwards the check runs every REGULAR_CYCLE_TIME
	 * milliseconds.
	 *
	 * Why? In the beginning it takes a while till all components are up and
	 * running. So it is likely, that in the beginning not all are immediately
	 * running.
	 */
	private static final int INITIAL_CYCLES = 3;
	private static final int INITIAL_CYCLE_TIME = 300_000; // in ms => 5 minutes
	private static final int REGULAR_CYCLE_TIME = 120 * 60_000; // in ms => 2 hours

	/**
	 * Map from App to defect details.
	 */
	protected final Map<String, String> defectiveApps = new HashMap<>();

	private final Logger log = LoggerFactory.getLogger(AppValidateWorker.class);

	private int cycleCountDown = AppValidateWorker.INITIAL_CYCLES;

	@Reference
	private AppManagerUtil appManagerUtil;
	@Reference
	private ComponentManager componentManager;

	@Reference
	private AppConfigValidator validator;

	private final AtomicReference<CancellationToken> cancellationToken = new AtomicReference<>(new CancellationToken());
	private Config config;

	@Activate
	private void activate() {
		this.activate(this.getClass().getSimpleName());
	}

	@Override
	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	public void configurationEvent(ConfigurationEvent event) {
		this.triggerNextRun();
	}

	@Override
	protected void forever() {
		measure(this.log, "validateApps", () -> {
			try {
				this.validateApps(this.cancellationToken.get());
			} catch (CancellationToken.CancellationException a) {
				this.log.debug("cancelled current validation run");
			}
		});

		final var config = this.config;
		if (config == null) {
			return;
		}
		if (config.setDefectiveApp != null) {
			config.setDefectiveApp.accept(!this.defectiveApps.isEmpty());
		}
	}

	@Override
	protected int getCycleTime() {
		if (this.cycleCountDown > 0) {
			this.cycleCountDown--;
			return AppValidateWorker.INITIAL_CYCLE_TIME;
		}
		return AppValidateWorker.REGULAR_CYCLE_TIME;
	}

	@Override
	public void triggerNextRun() {

		final var newCancellationToken = new CancellationToken();
		this.cancellationToken.getAndSet(newCancellationToken).cancel();

		// Reset Cycle-Counter on explicit run
		this.cycleCountDown = AppValidateWorker.INITIAL_CYCLES;
		super.triggerNextRun();
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	/**
	 * Called by parent debugLog.
	 *
	 * @return a debug log String or null
	 */
	protected String debugLog() {
		var defectiveApps = this.defectiveApps.entrySet().stream() //
				.map(e -> e.getKey() + "[" + e.getValue() + "]") //
				.collect(Collectors.joining(" "));

		if (defectiveApps.isEmpty()) {
			return null;

		}
		return defectiveApps;
	}

	/**
	 * Validates all Apps.
	 *
	 * <p>
	 * 'protected' so that it can be used in a JUnit test.
	 *
	 * @param cancellationToken to cancel the current execution
	 */
	protected void validateApps(CancellationToken cancellationToken) {
		final var unknownApps = new HashSet<String>();
		final var handledKeys = Sets.newHashSet("UNKNOWNAPPS");
		final var appInstances = this.appManagerUtil.getInstantiatedApps();

		final var allConfigs = this.appManagerUtil.appConfigs(appInstances, openemsAppInstance -> true);
		final var configsByInstance = StreamSupport.stream(allConfigs.spliterator(), false) //
				.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

		for (var entry : appInstances.stream() //
				.collect(groupingBy(t -> t.appId)).entrySet()) {
			cancellationToken.throwIfCancelled();

			final var appId = entry.getKey();
			handledKeys.add(appId);
			final var app = this.appManagerUtil.findAppById(appId);
			if (app.isEmpty()) {
				unknownApps.add(appId);
				continue;
			}

			final var errorsOfApp = new ArrayList<String>();
			for (var instance : entry.getValue()) {
				cancellationToken.throwIfCancelled();

				final var instanceConfig = configsByInstance.get(instance);
				if (instanceConfig == null) {
					errorsOfApp.add("Missing configuration for instance " + instance.instanceId);
					continue;
				}
				try {
					this.validator.validate(cancellationToken, instance, instanceConfig, configsByInstance);
				} catch (OpenemsNamedException e) {
					errorsOfApp.add(e.getMessage());
				}
			}
			if (errorsOfApp.isEmpty()) {
				this.defectiveApps.remove(appId);
			} else {
				this.defectiveApps.put(appId, String.join("; ", errorsOfApp));
			}
		}
		if (!unknownApps.isEmpty()) {
			this.defectiveApps.put("UNKNOWNAPPS", String.join("|", unknownApps));
		} else {
			this.defectiveApps.remove("UNKNOWNAPPS");
		}
		this.defectiveApps.keySet().removeIf(t -> !handledKeys.contains(t));
	}

}
