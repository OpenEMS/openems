package io.openems.edge.scheduler.simplescheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.api.controller.ControllerInterface;
import io.openems.edge.api.scheduler.SchedulerInterface;

import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;


@Designate( ocd=SimpleScheduler.Config.class, factory=true)
@Component(name="io.openems.edge.scheduler.simplescheduler",configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SimpleScheduler implements SchedulerInterface{

	private Map<String, ControllerInterface> controllers = new HashMap<>();
	private Config config;
	private List<ControllerInterface> orderedController = new ArrayList<>();
	
	@ObjectClassDefinition
	@interface Config {
		String id();
		String[] controllerOrder();
	}


	@Reference(policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.AT_LEAST_ONE, bind = "addController", unbind = "removeController")
	public void addController(ControllerInterface controller) {
		controllers.put(controller.getId(), controller);
		orderController();
	}

	public void removeController(ControllerInterface controller) {
		controllers.remove(controller.getId());
		orderController();
	}

	@Activate
	public void activate(Config config) {
		this.config = config;
		orderController();
	}

	@Modified
	public void update(Config config) {
		this.config = config;
		orderController();
	}

	@Override
	public List<ControllerInterface> getController() {
		return orderedController;
	}

	private void orderController() {
		orderedController.clear();
		Map<String, ControllerInterface> controllersLeft = new HashMap<>(controllers);
		for (String controllerId : config.controllerOrder()) {
			ControllerInterface controller = controllers.get(controllerId);
			if (controller != null) {
				orderedController.add(controllers.get(controllerId));
				controllersLeft.remove(controllerId);
			}
		}
		orderedController.addAll(controllersLeft.values());
	}

}
