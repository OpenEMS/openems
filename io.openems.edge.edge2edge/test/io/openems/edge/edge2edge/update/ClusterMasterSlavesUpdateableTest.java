package io.openems.edge.edge2edge.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.common.update.jsonrpc.GetUpdateState;

class ClusterMasterSlavesUpdateableTest {

	private ClusterMasterSlavesUpdateable sut;

	@BeforeEach
	void setup() {
		this.sut = new ClusterMasterSlavesUpdateable(new DummyComponentContext());
	}

	@Test
	void testGetMetaInfo() {
		assertEquals(new Updateable.UpdateableMetaInfo("Systemupdates für Cluster Slaves",
				"Systemupdates aller Cluster Slaves", Role.OWNER), this.sut.getMetaInfo(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateWithoutSlavesReturnsUnknown() {
		assertInstanceOf(GetUpdateState.UpdateState.Unknown.class, this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateAggregatesErrors() {
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave0", new GetUpdateState.UpdateState.Error("error0")));
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave1", new GetUpdateState.UpdateState.Error("error1")));
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave2", new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1")));

		assertEquals(new GetUpdateState.UpdateState.Error("slave0: error0; slave1: error1"),
				this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testInvalidVersion() {
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave0", new GetUpdateState.UpdateState.Available("1.0.0.SNAPSHOT", "1.0.1.SNAPSHOT")));

		assertEquals(new GetUpdateState.UpdateState.Unknown(), this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateAvailable() {
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave0", new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1")));
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave1", new GetUpdateState.UpdateState.Updated("1.1.0")));

		assertEquals(new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"),
				this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateUpdated() {
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave0", new GetUpdateState.UpdateState.Updated("1.0.1")));
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave1", new GetUpdateState.UpdateState.Updated("1.0.1")));

		assertEquals(new GetUpdateState.UpdateState.Updated("1.0.1"), this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateRunningDuringStartRunning() {
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave0", new GetUpdateState.UpdateState.Running(0, 5, Collections.emptyList())));
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave1", new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1")));

		assertEquals(new GetUpdateState.UpdateState.Running(0, 10, Collections.emptyList()),
				this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void testGetUpdateStateRunningDuringMiddleOfRunning() {
		this.sut.bindClusterSlaveUpdateable(new DummySlave("slave0", new GetUpdateState.UpdateState.Updated("1.0.1")));
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave1", new GetUpdateState.UpdateState.Running(60, 5, Collections.emptyList())));
		this.sut.bindClusterSlaveUpdateable(
				new DummySlave("slave2", new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1")));

		assertEquals(new GetUpdateState.UpdateState.Running(53, 15, Collections.emptyList()),
				this.sut.getUpdateState(Language.DEFAULT));
	}

	@Test
	void transientUnknownDoesNotAdvanceQueue() {
		var master = new ClusterMasterSlavesUpdateable(null);
		List<GetUpdateState.UpdateState> slaveSeq = List.of(new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"),
				new GetUpdateState.UpdateState.Unknown(),
				new GetUpdateState.UpdateState.Running(10, ClusterSlaveUpdateable.ESTIMATED_UPDATE_MINUTES, List.of()));
		var slave = new TestExecutionSlave("slave1", slaveSeq);

		master.bindClusterSlaveUpdateable(slave);
		master.executeUpdate();

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave");
		assertEquals(slave, ReflectionUtils.<ClusterSlaveUpdateable>getValueViaReflection(master, "currentSlave"));

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave");
		var deadline = ReflectionUtils.<Instant>getValueViaReflection(master, "currentSlaveDeadline");
		assertEquals(Instant.MIN, deadline);

		assertEquals(1, slave.executeCount);
	}

	@Test
	void persistentUnknownAdvancesQueue() {
		var master = new ClusterMasterSlavesUpdateable(null);
		List<GetUpdateState.UpdateState> slaveSeq = List.of(new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"));
		var slave = new TestExecutionSlave("slave2", slaveSeq);

		master.bindClusterSlaveUpdateable(slave);
		master.executeUpdate();

		ReflectionUtils.setAttributeViaReflection(master, "currentSlaveDeadline", Instant.now().minusMillis(1)); // expired

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave");
		assertNull(ReflectionUtils.<ClusterSlaveUpdateable>getValueViaReflection(master, "currentSlave"));
	}

	@Test
	void noSecondUpdateRequestIsSent() {
		var master = new ClusterMasterSlavesUpdateable(null);
		List<GetUpdateState.UpdateState> slaveSeq = List.of(new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"),
				new GetUpdateState.UpdateState.Unknown(),
				new GetUpdateState.UpdateState.Running(1, ClusterSlaveUpdateable.ESTIMATED_UPDATE_MINUTES, List.of()));
		var slave = new TestExecutionSlave("slave3", slaveSeq);

		master.bindClusterSlaveUpdateable(slave);
		master.executeUpdate();

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave");

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave");

		assertEquals(1, slave.executeCount, "executeUpdate should only be called once for the slave");
	}

	@Test
	void regressionAfterRunningLogsEndedUnexpectedly() {
		var master = new ClusterMasterSlavesUpdateable(null);
		List<GetUpdateState.UpdateState> slaveSeq = List.of(new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"),
				new GetUpdateState.UpdateState.Running(50, ClusterSlaveUpdateable.ESTIMATED_UPDATE_MINUTES, List.of()),
				new GetUpdateState.UpdateState.Available("1.0.0", "1.0.1"));
		var slave = new TestExecutionSlave("slave4", slaveSeq);

		master.bindClusterSlaveUpdateable(slave);
		master.executeUpdate();

		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave"); // Running observed
		ReflectionUtils.invokeMethodWithoutArgumentsViaReflection(master, "pollCurrentSlave"); // regressed to Available

		assertNull(ReflectionUtils.<ClusterSlaveUpdateable>getValueViaReflection(master, "currentSlave"));
		var logs = ReflectionUtils.<List<String>>getValueViaReflection(master, "updateLogs");
		assertTrue(logs.stream().anyMatch(m -> m.equals("Slave slave4 update ended unexpectedly")),
				"Expected 'update ended unexpectedly' log, but was: " + logs);
		assertTrue(logs.stream().noneMatch(m -> m.contains("did not start update in time")),
				"Expected no 'did not start update in time' log, but was: " + logs);
	}

	private static final class DummySlave extends ClusterSlaveUpdateable {

		private final GetUpdateState.UpdateState updateState;

		private DummySlave(String name, GetUpdateState.UpdateState updateState) {
			super(name, new DummyComponentManager().getClock());
			this.updateState = updateState;
		}

		@Override
		public UpdateableMetaInfo getMetaInfo(Language language) {
			return new UpdateableMetaInfo(this.getName(), "description", Role.ADMIN);
		}

		@Override
		public GetUpdateState.UpdateState getUpdateState(Language lang) {
			return this.updateState;
		}
	}

	private static class TestExecutionSlave extends ClusterSlaveUpdateable {
		final List<GetUpdateState.UpdateState> responses = new ArrayList<>();
		private int executeCount = 0;

		TestExecutionSlave(String name, List<GetUpdateState.UpdateState> seq) {
			super(name, new DummyComponentManager().getClock());
			this.responses.addAll(seq);
		}

		@Override
		public void executeUpdate() {
			this.executeCount++;
		}

		@Override
		public GetUpdateState.UpdateState getUpdateState(io.openems.common.session.Language lang) {
			if (this.responses.isEmpty()) {
				return new GetUpdateState.UpdateState.Unknown();
			}
			return this.responses.removeFirst();
		}
	}
}
