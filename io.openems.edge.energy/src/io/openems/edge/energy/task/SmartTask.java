package io.openems.edge.energy.task;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;

public class SmartTask extends AbstractEnergyTask {

	private final Logger log = LoggerFactory.getLogger(SmartTask.class);

	public SmartTask(ComponentManager componentManager, Consumer<Boolean> scheduleError) {
		super(componentManager, scheduleError);
	}

	@Override
	public void run() {
		this.log.info("To be implemented...");
	}

}
