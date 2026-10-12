package io.openems.edge.bridge.eebus.usecase;

import io.openems.edge.bridge.eebus.api.BridgeEebus;
import org.openmuc.jeebus.spine.spi.UseCase;

public abstract class EebusUseCase {
	protected final BridgeEebus bridge;

	protected EebusUseCase(BridgeEebus bridge) {
		this.bridge = bridge;
	}

	public abstract boolean isInUse();
	
	public abstract UseCase createUseCase();

	public abstract boolean requiresReInit();
}
