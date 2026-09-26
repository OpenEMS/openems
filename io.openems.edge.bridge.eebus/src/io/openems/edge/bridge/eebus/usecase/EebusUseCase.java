package io.openems.edge.bridge.eebus.usecase;

import org.openmuc.jeebus.spine.spi.UseCase;

public abstract class EebusUseCase {
	public abstract boolean isInUse();
	
	public abstract UseCase createUseCase();

	public abstract boolean requiresReInit();
}
