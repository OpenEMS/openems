package io.openems.edge.scheduler.api.test;

import java.util.Arrays;
import java.util.LinkedHashSet;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.scheduler.api.Scheduler;

/**
 * Provides a simple, simulated {@link Scheduler} component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyScheduler extends AbstractDummyScheduler<DummyScheduler> implements Scheduler, OpenemsComponent {

	private LinkedHashSet<String> controllers = new LinkedHashSet<>();

	public DummyScheduler(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values() //
		);
	}

	@Override
	protected final DummyScheduler self() {
		return this;
	}

	/**
	 * Sets the {@link Controller}s.
	 * 
	 * @param controllerIds Component-IDs of the Controllers
	 * @return myself
	 */
	public final DummyScheduler setControllers(String... controllerIds) {
		var cs = new LinkedHashSet<String>();
		Arrays.stream(controllerIds).forEach(c -> cs.add(c));
		this.controllers = cs;
		return this.self();
	}

	@Override
	public LinkedHashSet<String> getControllers() {
		return this.controllers;
	}
}
