package io.openems.common.exceptions;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

/**
 * This interface is similar to the java.util interface {@link Runnable}.
 * Difference is, that it allows the run() method to throw an
 * {@link OpenemsNamedException}.
 */
@FunctionalInterface
public interface CheckedRunnable {

	public void run() throws OpenemsNamedException;

}
