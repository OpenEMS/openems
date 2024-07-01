package io.openems.edge.core.cycle;

import java.util.Comparator;
import java.util.TreeSet;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.scheduler.api.Scheduler;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = Cycle.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL, //
		property = { //
				"enabled=true" //
		})
public class CycleImpl extends AbstractOpenemsComponent implements OpenemsComponent, Cycle {

	private final CycleWorker worker = new CycleWorker(this);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected EventAdmin eventAdmin;

	@Reference
	protected Sum sumComponent;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Holds the Schedulers and their relative cycleTime. They are sorted ascending
	 * by their cycleTimes.
	 */
	protected final TreeSet<Scheduler> schedulers = new TreeSet<>(Comparator.comparing(Scheduler::id));

	private Config config = null;

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected void addScheduler(Scheduler newScheduler) {
		synchronized (this.schedulers) {
			this.schedulers.add(newScheduler);
		}
	}

	protected void removeScheduler(Scheduler scheduler) {
		synchronized (this.schedulers) {
			this.schedulers.remove(scheduler);
		}
	}

	public CycleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Cycle.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.config = config;
		this.worker.activate(this.id());

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.config = config;
		this.worker.modified(this.id());

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.worker.deactivate();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	public int getCycleTime() {
		var config = this.config;
		if (config != null) {
			return config.cycleTime();
		}
		return Cycle.DEFAULT_CYCLE_TIME;
	}

}
