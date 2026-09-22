package io.openems.edge.ess.stabl.statemachine;

import io.openems.edge.ess.stabl.EssStablImpl;

public class Context {

	static final int MAX_RESET_ATTEMPTS = 3;
	static final long RESET_WAIT_TIME_MS = 10_000;

	private final EssStablImpl parent;

	// Error recovery state
	int resetAttemptCount = 0;
	long lastResetTime = 0;
	boolean resetInProgress = false;

	public Context(EssStablImpl parent) {
		this.parent = parent;
	}

	public EssStablImpl getParent() {
		return this.parent;
	}

}
