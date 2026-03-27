package io.openems.edge.ess.core.power;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.OPTIONAL;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.Filter;
import io.openems.edge.common.filter.PT1Filter;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.v1.PowerDistributionHandlerV1;
import io.openems.edge.ess.core.power.v2.PowerDistributionHandlerV2;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EssPower.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = OPTIONAL, //
		property = { //
				"enabled=true" //
		})
@EventTopics({ //
		TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		TOPIC_CYCLE_BEFORE_WRITE, //
		TOPIC_CYCLE_AFTER_WRITE, //
})
public class EssPowerImpl extends AbstractOpenemsComponent implements EssPower, OpenemsComponent, EventHandler, Power {

	private final Logger log = LoggerFactory.getLogger(EssPowerImpl.class);
	private final List<ManagedSymmetricEss> esss = new CopyOnWriteArrayList<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, target = "(enabled=true)")
	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.esss.add(ess);
		if (this.powerDistributionHandler != null) {
			this.powerDistributionHandler.onUpdateEsss();
		}
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.esss.remove(ess);
		if (this.powerDistributionHandler != null) {
			this.powerDistributionHandler.onUpdateEsss();
		}
	}

	private Config config;
	private Filter filter; // nullable
	private PowerDistributionHandler powerDistributionHandler;

	public EssPowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssPower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.updateConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, SINGLETON_COMPONENT_ID, SINGLETON_SERVICE_PID, true);
		this.updateConfig(config);

		if (OpenemsComponent.validateSingleton(this.cm, SINGLETON_SERVICE_PID, SINGLETON_COMPONENT_ID)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.config = config;

		this.powerDistributionHandler = switch (config.strategy()) {
		case UNDEFINED, NONE, ALL_CONSTRAINTS, //
				OPTIMIZE_BY_MOVING_TOWARDS_TARGET, //
				OPTIMIZE_BY_KEEPING_TARGET_DIRECTION_AND_MAXIMIZING_IN_ORDER, //
				OPTIMIZE_BY_KEEPING_ALL_EQUAL, //
				OPTIMIZE_BY_KEEPING_ALL_NEAR_EQUAL, //
				OPTIMIZE_BY_PREFERRING_DC_POWER //
			-> new PowerDistributionHandlerV1(//
					config.strategy(), config.symmetricMode(), config.debugMode(), //
					() -> this.esss, //
					this::_setStaticConstraintsFailed, this::_setNotSolved, //
					this::_setSolveDuration, this::_setSolveStrategy);
		case BALANCE //
			-> new PowerDistributionHandlerV2(() -> this.esss);
		};
		this.powerDistributionHandler.onUpdateEsss();

		if (config.enablePid()) {
			// build a PidFilter instance with the configured P, I and D variables
			this.filter = new PidFilter(this.config.p(), this.config.i(), this.config.d());

		} else if (config.enablePT1Filter()) {
			// build a PT1Filter instance with the configured time constant parameter
			this.filter = new PT1Filter(this.componentManager.getClock(), config.pt1TimeConstant());

		} else {
			// unset filter if filters are disabled
			this.filter = null;
		}
	}

	@Override
	public synchronized Constraint addConstraint(Constraint constraint) {
		this.powerDistributionHandler.addConstraint(constraint);
		return constraint;
	}

	@Override
	public synchronized Constraint addConstraintAndValidate(Constraint constraint) throws OpenemsException {
		this.powerDistributionHandler.addConstraintAndValidate(constraint);
		return constraint;
	}

	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr)
			throws OpenemsException {
		return this.powerDistributionHandler.getCoefficient(ess, phase, pwr);
	}

	@Override
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, SingleOrAllPhase phase,
			Pwr pwr, Relationship relationship, int value) throws OpenemsException {
		return this.powerDistributionHandler.createSimpleConstraint(description, ess, phase, pwr, relationship, value);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		this.powerDistributionHandler.removeConstraint(constraint);
	}

	@Override
	public int getMaxPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
		return this.powerDistributionHandler.getPowerExtrema(ess, phase, pwr, GoalType.MAXIMIZE);
	}

	@Override
	public int getMinPower(ManagedSymmetricEss ess, SingleOrAllPhase phase, Pwr pwr) {
		return this.powerDistributionHandler.getPowerExtrema(ess, phase, pwr, GoalType.MINIMIZE);
	}

	@Override
	public void handleEvent(Event event) {
		try {
			switch (event.getTopic()) {
			case TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
				-> this.powerDistributionHandler.onAfterProcessImage();

			case TOPIC_CYCLE_BEFORE_WRITE //
				-> this.powerDistributionHandler.onBeforeWriteEvent();

			case TOPIC_CYCLE_AFTER_WRITE //
				-> this.powerDistributionHandler.onAfterWriteEvent();
			}

		} catch (Exception e) {
			this.logError(this.log,
					"Error during handleEvent(). " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public Filter getFilter() {
		return this.filter;
	}

	@Override
	public boolean isFilterEnabled() {
		return this.filter != null;
	}
}
