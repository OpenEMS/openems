package io.openems.edge.core;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.api.controller.ControllerInterface;
import io.openems.edge.api.scheduler.SchedulerInterface;

import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = ControllerExecutor.Config.class, factory = true)
@Component(name = "io.openems.edge.core.controllerexecutor", service = {}, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerExecutor implements Runnable {

	private Thread t;
	private boolean run;
	private long cycleStart = 0;
	private Config config;

	@ObjectClassDefinition
	@interface Config {
		String scheduler_target();

		int cycleTime();
	}

	@Reference(cardinality = ReferenceCardinality.MANDATORY)
	public SchedulerInterface scheduler;

	@Activate
	void activate(Config config) {
		this.config = config;
		t = new Thread(this);
		t.setName("Controller Executor [" + config.scheduler_target() + "]");
		run = true;
		t.start();
	}
	
	@Modified
	void modified(Config config) {
		this.config = config;
	}

	@Deactivate
	void deactivate() {
		run = false;
	}

	@Override
	public void run() {
		while (run) {
			try {
				cycleStart = System.currentTimeMillis();
				System.out.println(scheduler);
				for (ControllerInterface controller : scheduler.getController()) {
					controller.executeLogic();
				}
				try {
					long sleep = (cycleStart + config.cycleTime()) - System.currentTimeMillis();
					if (sleep > 0) {
						System.out.println("Sleep: " + sleep + " milis.");
						Thread.sleep(sleep);
					}
				} catch (InterruptedException e) {
					// sleep failed
				}
			} catch (Throwable t) {

			}
		}
	}

}
