package io.openems.edge.ess.core.power;

import java.util.List;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.DisabledPidFilter;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.core.power.data.ConstraintUtil;
import io.openems.edge.ess.core.power.data.LogUtil;
import io.openems.edge.ess.core.power.solver.CalculatePowerExtrema;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = EssPower.SINGLETON_SERVICE_PID, //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL, //
		property = { //
				"enabled=true" //
		})
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
})
public class EssPowerImpl extends AbstractOpenemsComponent implements EssPower, OpenemsComponent, EventHandler, Power {

	private final Logger log = LoggerFactory.getLogger(EssPowerImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	private final Data data;
	private final Solver solver;

	private boolean debugMode = EssPowerImpl.DEFAULT_DEBUG_MODE;

	private Config config;
	private PidFilter pidFilter;

	public EssPowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EssPower.ChannelId.values() //
		);
		this.data = new Data();
		this.data.onStaticConstraintsFailed(this::_setStaticConstraintsFailed);

		this.solver = new Solver(this.data);
		this.solver.onSolved((isSolved, duration, strategy) -> {
			this._setNotSolved(!isSolved);
			this._setSolveDuration(duration);
			this._setSolveStrategy(strategy);
		});
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
		this.data.setSymmetricMode(config.symmetricMode());
		this.debugMode = config.debugMode();
		this.solver.setDebugMode(config.debugMode());
		this.config = config;

		if (config.enablePid()) {
			// build a PidFilter instance with the configured P, I and D variables
			this.pidFilter = new PidFilter(this.config.p(), this.config.i(), this.config.d());
			// use a DisabledPidFilter instance, that always just returns the unfiltered
			// target value
		} else {
			this.pidFilter = new DisabledPidFilter();
		}
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected synchronized void addEss(ManagedSymmetricEss ess) {
		this.data.addEss(ess);
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		this.data.removeEss(ess);
	}

	@Override
	public synchronized Constraint addConstraint(Constraint constraint) {
		this.data.addConstraint(constraint);
		return constraint;
	}

	@Override
	public synchronized Constraint addConstraintAndValidate(Constraint constraint) throws OpenemsException {
		this.data.addConstraint(constraint);
		try {
			this.solver.isSolvableOrError();
		} catch (OpenemsException e) {
			this.data.removeConstraint(constraint);
			if (this.debugMode) {
				var allConstraints = this.data.getConstraintsForAllInverters();
				LogUtil.debugLogConstraints(this.log, "Unable to validate with following constraints:", allConstraints);
				this.logWarn(this.log, "Failed to add Constraint: " + constraint);
			}
			if (e instanceof PowerException) {
				((PowerException) e).setReason(constraint);
			}
			throw e;
		}
		return constraint;
	}

	/*
	 * Helpers to create Constraints
	 */
	@Override
	public Coefficient getCoefficient(ManagedSymmetricEss ess, Phase phase, Pwr pwr) throws OpenemsException {
		return this.data.getCoefficient(ess.id(), phase, pwr);
	}

	@Override
	public Constraint createSimpleConstraint(String description, ManagedSymmetricEss ess, Phase phase, Pwr pwr,
			Relationship relationship, double value) throws OpenemsException {
		return ConstraintUtil.createSimpleConstraint(this.data.getCoefficients(), //
				description, ess.id(), phase, pwr, relationship, value);
	}

	@Override
	public void removeConstraint(Constraint constraint) {
		this.data.removeConstraint(constraint);
	}

	@Override
	public int getMaxPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.getActivePowerExtrema(ess, phase, pwr, GoalType.MAXIMIZE);
	}

	@Override
	public int getMinPower(ManagedSymmetricEss ess, Phase phase, Pwr pwr) {
		return this.getActivePowerExtrema(ess, phase, pwr, GoalType.MINIMIZE);
	}

	private int getActivePowerExtrema(ManagedSymmetricEss ess, Phase phase, Pwr pwr, GoalType goal) {
		final List<Constraint> allConstraints;
		try {
			allConstraints = this.data.getConstraintsForAllInverters();
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to get Constraints " + e.getMessage());
			return 0;
		}
		var power = CalculatePowerExtrema.from(this.data.getCoefficients(), allConstraints, ess.id(), phase, pwr, goal);
		if (power <= Integer.MIN_VALUE || power >= Integer.MAX_VALUE) {
			this.logError(this.log, goal.name() + " Power for [" + ess.toString() + "," + phase.toString() + ","
					+ pwr.toString() + "=" + power + "] is out of bounds. Returning '0'");
			return 0;
		}
		if (goal == GoalType.MAXIMIZE) {
			return (int) Math.floor(power);
		}
		return (int) Math.ceil(power);
	}

	@Override
	public void handleEvent(Event event) {
		try {
			switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE //
				-> this.solver.solve(this.config.strategy());

			case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
				-> this.data.initializeCycle();
			}

		} catch (Exception e) {
			this.logError(this.log,
					"Error during handleEvent(). " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Gets the Ess component with the given ID.
	 *
	 * @param essId the component ID of Ess
	 * @return an Ess instance
	 */
	protected ManagedSymmetricEss getEss(String essId) {
		return this.data.getEss(essId);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public PidFilter getPidFilter() {
		return this.pidFilter;
	}

	/**
	 * Is Debug-Mode activated?.
	 *
	 * @return true if is activated
	 */
	public boolean isDebugMode() {
		return this.debugMode;
	}

	public boolean isPidEnabled() {
		return this.config.enablePid();
	}

}
