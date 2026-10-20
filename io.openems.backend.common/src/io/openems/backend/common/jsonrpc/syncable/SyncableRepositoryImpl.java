package io.openems.backend.common.jsonrpc.syncable;

import static org.osgi.service.component.annotations.ServiceScope.SINGLETON;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.ImmutableList;

import io.openems.common.event.EventEmitter;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;

@Component(scope = SINGLETON)
public class SyncableRepositoryImpl implements SyncableRepository {

	private final Map<SyncableKey<?>, SyncableEntry<?>> staticEntries = new HashMap<>();
	private final ReadWriteLock staticEntriesLock = new ReentrantReadWriteLock();

	private final EventEmitter<SyncableRepository.ChangeEvent> changeEvent = new EventEmitter<>();

	@Override
	public ImmutableList<SyncableEntry<?>> getAllStaticEntries() {
		this.staticEntriesLock.readLock().lock();
		try {
			return ImmutableList.copyOf(this.staticEntries.values());
		} finally {
			this.staticEntriesLock.readLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <VALUE> SyncableEntry<VALUE> getStaticEntry(SyncableKey<VALUE> key) {
		this.staticEntriesLock.readLock().lock();
		try {
			return (SyncableEntry<VALUE>) this.staticEntries.get(key);
		} finally {
			this.staticEntriesLock.readLock().unlock();
		}
	}

	@Override
	public void setStaticEntry(SyncableEntry<?> entry) {
		this.staticEntriesLock.writeLock().lock();
		try {
			this.staticEntries.put(entry.key(), entry);
		} finally {
			this.staticEntriesLock.writeLock().unlock();
		}

		this.changeEvent.emit(new SyncableRepository.ChangeEvent.EntryUpdated(entry));
	}

	@Override
	public void removeStaticEntry(SyncableKey<?> key) {
		SyncableEntry<?> removedEntry;

		this.staticEntriesLock.writeLock().lock();
		try {
			removedEntry = this.staticEntries.remove(key);
		} finally {
			this.staticEntriesLock.writeLock().unlock();
		}

		if (removedEntry != null) {
			this.changeEvent.emit(new SyncableRepository.ChangeEvent.EntryRemoved(removedEntry));
		}
	}

	@Override
	public void replaceStaticEntries(List<SyncableEntry<?>> entries) {
		this.staticEntriesLock.writeLock().lock();
		try {
			this.staticEntries.clear();
			for (var entry : entries) {
				this.staticEntries.put(entry.key(), entry);
			}
		} finally {
			this.staticEntriesLock.writeLock().unlock();
		}

		this.changeEvent.emit(new SyncableRepository.ChangeEvent.AllEntriesReplaced());
	}

	@Override
	public EventEmitter<SyncableRepository.ChangeEvent> getChangeEvent() {
		return this.changeEvent;
	}
}
