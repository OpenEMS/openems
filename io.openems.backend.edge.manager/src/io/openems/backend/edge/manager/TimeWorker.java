package io.openems.backend.edge.manager;

import java.time.Instant;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.worker.AbstractWorker;

@Component(service = TimeWorker.class, immediate = true)
public class TimeWorker extends AbstractWorker {
	private final Logger log = LoggerFactory.getLogger(TimeWorker.class);

	@Reference
	private SyncableRepository syncableRepository;

	@Activate
	protected synchronized void activate() {
		this.activate("TimeWorker", true);
	}

	@Deactivate
	@Override
	public synchronized void deactivate() {
		super.deactivate();
	}

	@Override
	protected void forever() throws Throwable {
		this.log.debug("Updating current time in unix timestamp");

		var key = new SyncableKey.CurrentTimeInUnixTimestamp();
		this.syncableRepository.setStaticEntry(new SyncableEntry<>(key, System.currentTimeMillis(), Instant.now()));
	}

	@Override
	protected int getCycleTime() {
		return 60_000;
	}
}
