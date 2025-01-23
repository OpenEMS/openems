package io.openems.edge.evcs.hardybarth;

import java.util.concurrent.CompletableFuture;

import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.WriteHandler;

public class HardyBarthWriteHandler extends WriteHandler {

	private CompletableFuture<Void> applyChargePowerTask = CompletableFuture.completedFuture(null);

	public HardyBarthWriteHandler(ManagedEvcs parent) {
		super(parent);
	}

	@Override
	protected synchronized void applyChargePower(int power) {
		if (!this.applyChargePowerTask.isDone()) {
			return;
		}
		this.applyChargePowerTask = CompletableFuture.runAsync(() -> {
			super.applyChargePower(power);
		});
	}

	@Override
	public synchronized void cancelChargePower() {
		this.applyChargePowerTask.cancel(true);
	}

}