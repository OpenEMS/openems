package io.openems.edge.edge2edge.update;

import java.time.Instant;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.ServiceRegistration;
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

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.SemanticVersion;
import io.openems.common.utils.TextProvider;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;

@Component(//
		service = ClusterMasterSlavesUpdateable.class, //
		immediate = true, //
		scope = ServiceScope.SINGLETON //
)
public class ClusterMasterSlavesUpdateable implements Updateable {

	private static final long POLL_INTERVAL_MS = 2000;
	private static final int MAX_UPDATE_LOG_LINES = 200;
	private static final long DEFAULT_STARTUP_GRACE_MS = 5_000;
	private Instant currentSlaveDeadline = Instant.MIN;

	private final ScheduledExecutorService scheduler;
	private ScheduledFuture<?> pollTask;

	private final List<ClusterSlaveUpdateable> pendingSlaves = new CopyOnWriteArrayList<>();
	private ClusterSlaveUpdateable currentSlave;

	private final Logger log = LoggerFactory.getLogger(ClusterMasterSlavesUpdateable.class);
	private final List<ClusterSlaveUpdateable> slaves = new CopyOnWriteArrayList<>();
	private final List<String> updateLogs = new CopyOnWriteArrayList<>();
	private ComponentContext context;
	private ServiceRegistration<Updateable> updateableRegistration = null;

	@Reference(//
			cardinality = ReferenceCardinality.MULTIPLE, //
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY //
	)
	protected void bindClusterSlaveUpdateable(ClusterSlaveUpdateable slave) {
		this.slaves.add(slave);
		this.updateUpdateableRegistration();
	}

	protected void unbindClusterSlaveUpdateable(ClusterSlaveUpdateable slave) {
		this.slaves.remove(slave);
		this.updateUpdateableRegistration();
	}

	@Activate
	public ClusterMasterSlavesUpdateable(ComponentContext context) {
		this.context = context;
		this.scheduler = Executors.newSingleThreadScheduledExecutor();
		this.updateUpdateableRegistration();
	}

	@Deactivate
	protected void deactivate() {
		this.unregisterUpdateable();
		if (this.pollTask != null) {
			this.pollTask.cancel(true);
		}
		this.scheduler.shutdownNow();
		this.context = null;
	}

	@Override
	public UpdateableMetaInfo getMetaInfo(Language language) {
		final var name = TextProvider //
				.byTranslation(ClusterMasterSlavesUpdateable.class, "ClusterMasterSlaveUpdateable.Metainfo.name") //
				.getText(language);
		final var description = TextProvider //
				.byTranslation(ClusterMasterSlavesUpdateable.class, "ClusterMasterSlaveUpdateable.Metainfo.description") //
				.getText(language);
		return new Updateable.UpdateableMetaInfo(name, description, Role.OWNER);
	}

	@Override
	public synchronized void executeUpdate() {
		if (this.currentSlave != null) {
			this.log.warn("Update already running. Ignoring request.");
			return;
		}
		this.updateLogs.clear();
		this.addUpdateLog("Starting update for all slaves...");
		this.pendingSlaves.clear();
		this.pendingSlaves.addAll(this.slaves);
		this.startNextSlaveOrFinish();
	}

	@Override
	public synchronized GetUpdateState.UpdateState getUpdateState(Language lang) {

		if (this.slaves.isEmpty()) {
			return new GetUpdateState.UpdateState.Unknown();
		}

		final Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates = this.slaves.stream() //
				.collect(Collectors.toMap(Function.identity(), //
						slave -> slave.getUpdateState(lang), //
						(existing, replacement) -> existing, //
						LinkedHashMap::new));

		final var errorSlaveStates = this.filterForError(slaveStates);

		final var errorMessages = errorSlaveStates.entrySet().stream() //
				.map(entry -> entry.getKey().getName() //
						+ ": " //
						+ entry.getValue().errorMessage()) //
				.collect(Collectors.joining("; "));
		final var availableStates = this.filterForAvailable(slaveStates);
		final var updatedStates = this.filterForUpdated(slaveStates);
		final var runningStates = this.filterForRunning(slaveStates);

		if (!runningStates.isEmpty()) {
			return this.aggregateRunning(availableStates.size(), updatedStates.size(), runningStates.getFirst());
		}

		if (!errorMessages.isEmpty()) {
			return new GetUpdateState.UpdateState.Error(errorMessages);
		}

		if (updatedStates.size() == this.slaves.size()) {
			return new GetUpdateState.UpdateState.Updated(updatedStates.getFirst().version());
		}

		if (!availableStates.isEmpty()) {
			final var firstAvailable = availableStates.getFirst();
			return new GetUpdateState.UpdateState.Available(firstAvailable.currentVersion(),
					firstAvailable.latestVersion());
		}

		return new GetUpdateState.UpdateState.Unknown();
	}

	private List<GetUpdateState.UpdateState> filterForState(//
			Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates, //
			Class<?> stateClass //
	) {
		return slaveStates.values().stream().filter(stateClass::isInstance).toList();
	}

	private List<GetUpdateState.UpdateState.Running> filterForRunning(//
			Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates //
	) {
		return this.filterForState(slaveStates, GetUpdateState.UpdateState.Running.class).stream() //
				.map(state -> (GetUpdateState.UpdateState.Running) state) //
				.toList();
	}

	private List<GetUpdateState.UpdateState.Available> filterForAvailable(//
			Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates //
	) {
		return this.filterForState(slaveStates, GetUpdateState.UpdateState.Available.class).stream() //
				.map(state -> (GetUpdateState.UpdateState.Available) state) //
				.filter(state -> this.isMainVersion(state.latestVersion())) //
				.toList();
	}

	private List<GetUpdateState.UpdateState.Updated> filterForUpdated(//
			Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates //
	) {
		return this.filterForState(slaveStates, GetUpdateState.UpdateState.Updated.class).stream() //
				.map(state -> (GetUpdateState.UpdateState.Updated) state) //
				.toList();
	}

	private Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState.Error> filterForError(//
			Map<ClusterSlaveUpdateable, GetUpdateState.UpdateState> slaveStates //
	) {
		return slaveStates.entrySet().stream()
				.filter(entry -> entry.getValue() instanceof GetUpdateState.UpdateState.Error).collect(Collectors.toMap(//
						Map.Entry::getKey, //
						entry -> (GetUpdateState.UpdateState.Error) entry.getValue(), //
						(existing, replacement) -> existing, //
						LinkedHashMap::new));
	}

	private GetUpdateState.UpdateState.Running aggregateRunning(//
			int availableStatesSize, //
			int updatedStatesSize, //
			GetUpdateState.UpdateState.Running currentRunning //
	) {
		final int totalSize = availableStatesSize + updatedStatesSize + 1;

		final int totalPercentOfCompletedAndRunning = updatedStatesSize * 100 + currentRunning.percentCompleted();
		final int avgPercent = totalPercentOfCompletedAndRunning / totalSize;
		final int estimatedMinutes = totalSize * ClusterSlaveUpdateable.ESTIMATED_UPDATE_MINUTES;

		return new GetUpdateState.UpdateState.Running(avgPercent, estimatedMinutes, this.updateLogs);
	}

	private synchronized void updateUpdateableRegistration() {
		if (this.context == null) {
			return;
		}

		if (this.slaves.isEmpty()) {
			this.unregisterUpdateable();
			return;
		}

		if (this.updateableRegistration == null) {
			this.updateableRegistration = this.context.getBundleContext() //
					.registerService(Updateable.class, this, new Hashtable<>());
		}
	}

	private synchronized void unregisterUpdateable() {
		if (this.updateableRegistration == null) {
			return;
		}

		this.updateableRegistration.unregister();
		this.updateableRegistration = null;
	}

	private synchronized void startNextSlaveOrFinish() {
		if (this.pendingSlaves.isEmpty()) {
			this.addUpdateLog("Update for all slaves completed.");
			this.currentSlave = null;
			return;
		}

		final var slave = this.pendingSlaves.removeFirst();
		final var state = slave.getUpdateState(Language.DEFAULT);

		if (state instanceof GetUpdateState.UpdateState.Available available
				&& this.isMainVersion(available.latestVersion())) {

			this.currentSlave = slave;
			this.addUpdateLog(String.format("Starting update for slave %s", slave.getName()));
			slave.executeUpdate();

			this.pollTask = this.scheduler.scheduleWithFixedDelay(//
					this::pollCurrentSlave, POLL_INTERVAL_MS, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
			this.currentSlaveDeadline = Instant.now().plusMillis(DEFAULT_STARTUP_GRACE_MS);
		} else {
			this.addUpdateLog(String.format("Skipping slave %s", slave.getName()));
			this.startNextSlaveOrFinish();
		}
	}

	private synchronized void pollCurrentSlave() {
		if (this.currentSlave == null) {
			return;
		}
		final var state = this.currentSlave.getUpdateState(Language.DEFAULT);

		switch (state) {
		case GetUpdateState.UpdateState.Running running -> {
			this.currentSlaveDeadline = Instant.MIN;
			this.addUpdateLog(
					String.format("Slave %s running: %d%%", this.currentSlave.getName(), running.percentCompleted()));
		}
		case GetUpdateState.UpdateState.Updated(String version) -> {
			this.addUpdateLog(String.format("Slave %s updated to %s", this.currentSlave.getName(), version));
			this.advance();
		}
		case GetUpdateState.UpdateState.Error(String errorMessage) -> {
			this.addUpdateLog(String.format("Slave %s failed: %s", this.currentSlave.getName(), errorMessage));
			this.advance();
		}
		case GetUpdateState.UpdateState.Unknown unknown -> this.handleAwaitingStartup();
		case GetUpdateState.UpdateState.Available available -> this.handleAwaitingStartup();
		}
	}

	private void advance() {
		this.currentSlave = null;
		this.currentSlaveDeadline = Instant.MIN;
		if (this.pollTask != null) {
			this.pollTask.cancel(false);
			this.pollTask = null;
		}
		this.startNextSlaveOrFinish();
	}

	private void addUpdateLog(String message) {
		this.updateLogs.add(message);
		if (this.updateLogs.size() > MAX_UPDATE_LOG_LINES) {
			this.updateLogs.removeFirst();
		}
	}

	private void handleAwaitingStartup() {
		final var now = Instant.now();
		if (now.isBefore(this.currentSlaveDeadline)) {
			return;
		}

		if (this.currentSlaveDeadline == Instant.MIN) {
			// Running was already observed, but the slave regressed to Unknown or Available
			// without reporting Updated or Error
			this.addUpdateLog(String.format("Slave %s update ended unexpectedly", this.currentSlave.getName()));
		} else {
			this.addUpdateLog(String.format("Slave %s did not start update in time.", this.currentSlave.getName()));
		}
		this.advance();
	}

	private boolean isMainVersion(String version) {
		final var semanticVersion = SemanticVersion.fromStringOrZero(version);
		return !(semanticVersion == SemanticVersion.ZERO || !semanticVersion.additional().isEmpty());
	}
}
