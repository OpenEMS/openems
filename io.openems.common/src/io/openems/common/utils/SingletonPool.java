package io.openems.common.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.function.Disposable;

/**
 * Thread-safe pool for lazily created singleton-like instances addressed by a
 * key.
 *
 * <p>
 * Each call to {@link #get(Object)} increases a usage counter and each
 * {@link #unget(SingletonPoolImplementation)} decreases it. Instances are
 * activated on first use and deactivated when no user remains.
 *
 * @param <KEY>            the key type used to identify pooled instances
 * @param <IMPLEMENTATION> the pooled implementation type
 */
public abstract class SingletonPool<KEY, IMPLEMENTATION extends SingletonPool.SingletonPoolImplementation>
		implements Disposable {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final Map<KEY, InitializedInstance> instances = new HashMap<>();

	/**
	 * Gets an existing instance for the specified key or creates a instance, if it
	 * does not exist.
	 * 
	 * <p>
	 * Must be returned with unget when not needed anymore!
	 *
	 * @param key Key to identify the instance
	 * @return Existing or newly created instance
	 */
	public synchronized IMPLEMENTATION get(KEY key) {
		var initializedProvider = this.instances.get(key);
		if (initializedProvider != null) {
			initializedProvider.usageCount++;
			return initializedProvider.provider;
		}

		var provider = this.createNewInstance(key);
		this.instances.put(key, new InitializedInstance(provider));

		try {
			provider.activate();
			this.onPostLoadInstance(provider);
		} catch (Exception ex) {
			this.instances.remove(key);
			throw new RuntimeException("Error while activating provider for key %s and implementation %s".formatted(key,
					provider.getClass().getSimpleName()), ex);
		}

		return provider;
	}

	/**
	 * Returns an instance to the pool. If no one is tracking the instance anymore,
	 * the instance is unloaded and removed.
	 *
	 * @param provider Provider to return
	 */
	public synchronized void unget(IMPLEMENTATION provider) {
		if (provider == null) {
			return;
		}

		var initializedEntry = this.instances.entrySet().stream() //
				.filter(x -> x.getValue().provider == provider) //
				.findAny() //
				.orElse(null);

		if (initializedEntry == null) {
			return;
		}

		if (initializedEntry.getValue().usageCount <= 1) {
			this.instances.remove(initializedEntry.getKey());

			try {
				initializedEntry.getValue().provider.deactivate();
			} catch (Exception ex) {
				this.logger.error("Error while deactivating provider for key {} and implementation {} ",
						initializedEntry.getKey(), initializedEntry.getValue().provider.getClass().getSimpleName(), ex);
			}
			this.onPostUnloadInstance(initializedEntry.getValue().provider);
		} else {
			initializedEntry.getValue().usageCount--;
		}
	}

	public synchronized List<IMPLEMENTATION> getAllInstances() {
		return this.instances.values().stream() //
				.map(x -> x.provider) //
				.toList();
	}

	@Override
	public synchronized void dispose() {
		for (var instance : this.instances.values()) {
			instance.provider.deactivate();
		}
		this.instances.clear();
	}

	protected abstract IMPLEMENTATION createNewInstance(KEY config);

	protected void onPostLoadInstance(IMPLEMENTATION instance) {
		// Optional hook for subclasses to implement
	}

	protected void onPostUnloadInstance(IMPLEMENTATION instance) {
		// Optional hook for subclasses to implement
	}

	private class InitializedInstance {
		private final IMPLEMENTATION provider;
		private int usageCount;

		public InitializedInstance(IMPLEMENTATION provider) {
			this.provider = provider;
			this.usageCount = 1;
		}
	}

	public static interface SingletonPoolImplementation {
		void activate();

		void deactivate();
	}

}
