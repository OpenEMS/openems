package io.openems.common.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public final class CancellationToken {
	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	/**
	 * Requests cancellation. Safe to call multiple times.
	 */
	public void cancel() {
		this.cancelled.set(true);
	}

	/**
	 * Returns true if cancellation has been requested.
	 * 
	 * @return true if cancellation has been requested
	 */
	public boolean isCancelled() {
		return this.cancelled.get();
	}

	/**
	 * Throws if cancellation has been requested.
	 * 
	 * @throws CancellationException if cancellation has been requested
	 */
	public void throwIfCancelled() throws CancellationException {
		if (this.isCancelled()) {
			throw new CancellationException();
		}
	}

	public static class CancellationException extends RuntimeException {
		private static final long serialVersionUID = -7669067388268558320L;

		public CancellationException() {
			super("Operation was cancelled.");
		}
	}
}
