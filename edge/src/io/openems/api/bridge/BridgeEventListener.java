package io.openems.api.bridge;

public abstract class BridgeEventListener {

	private long requiredTime = 0;

	public void executeNotify(BridgeEvent event) {
		long beforeExecute = System.currentTimeMillis();
		notify(event);
		requiredTime = System.currentTimeMillis() - beforeExecute;
	}

	public long getRequiredTime() {
		return requiredTime;
	}

	protected abstract void notify(BridgeEvent event);

}
