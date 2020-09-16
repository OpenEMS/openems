package io.openems.edge.ess.core.power;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficient;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.EssType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.ess.power.api.SolverStrategy;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Ess.Power", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.OPTIONAL, //
		property = { //
				"id=_power", //
				"enabled=true", //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
		})
public class PowerComponent extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, Power {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * The duration needed for solving the Power.
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Integer
		 * <li>Unit: milliseconds
		 * <li>Range: positive
		 * </ul>
		 */
		SOLVE_DURATION(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS)),
		/**
		 * The eventually used solving strategy.
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Integer
		 * <li>Unit: milliseconds
		 * <li>Range: positive
		 * </ul>
		 */
		SOLVE_STRATEGY(Doc.of(SolverStrategy.values())),
		/**
		 * Whether the Power problem could be solved.
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Boolean
		 * </ul>
		 */
		NOT_SOLVED(Doc.of(Level.WARNING)),
		/**
		 * Gets set, when setting static Constraints failed.
		 * 
		 * <ul>
		 * <li>Interface: PowerComponent
		 * <li>Type: Boolean
		 * </ul>
		 */
		STATIC_CONSTRAINTS_FAILED(Doc.of(Level.FAULT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	private final Logger log = LoggerFactory.getLogger(PowerComponent.class);

	protected static final boolean DEFAULT_SYMMETRIC_MODE = true;
	protected static final boolean DEFAULT_DEBUG_MODE = false;
	protected static final SolverStrategy DEFAULT_SOLVER_STRATEGY = SolverStrategy.OPTIMIZE_BY_MOVING_TOWARDS_TARGET;

	/**
	 * Holds all managed Ess objects by their ID.
	 */
	protected final Map<String, ManagedSymmetricEss> esss = new HashMap<>();

	private final Data data;
	private final Solver solver;

	private boolean debugMode = PowerComponent.DEFAULT_DEBUG_MODE;

	private Config config;
	private PidFilter pidFilter;

	public PowerComponent() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
		this.data = new Data(this);
		this.solver = new Solver(data);

		this.solver.onSolved((isSolved, duration, strategy) -> {
			this.getNotSolvedChannel().setNextValue(!isSolved);
			this.getSolveDurationChannel().setNextValue(duration);
			this.getSolveStrategyChannel().setNextValue(strategy);
		});
	}

	@Activate
	void activate(ComponentContext context, Map<String, Object> properties, Config config) {
		super.activate(context, "_power", "Ess.Power", true);
		this.updateConfig(config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Modified
	void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, "_power", "Ess.Power", true);
		this.updateConfig(config);
	}

	private void updateConfig(Config config) {
		this.data.setSymmetricMode(config.symmetricMode());
		this.debugMode = config.debugMode();
		this.solver.setDebugMode(config.debugMode());
		this.solver.setStrategy(config.strategy());
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
		this.esss.put(ess.id(), ess);
		this.data.addEss(ess);
	}

	protected synchronized void removeEss(ManagedSymmetricEss ess) {
		Iterator<Entry<String, ManagedSymmetricEss>> i = this.esss.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, ManagedSymmetricEss> entry = i.next();
			if (Objects.equals(entry.getValue(), ess)) {
				this.data.removeEss(entry.getKey());
				i.remove();
				break;
			}
		}
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
				List<Constraint> allConstraints = this.data.getConstraintsForAllInverters();
				PowerComponent.debugLogConstraints(this.log, "Unable to validate with following constraints:",
						allConstraints);
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
		return this.data.createSimpleConstraint(description, ess.id(), phase, pwr, relationship, value);
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
		double power = this.solver.getActivePowerExtrema(ess.id(), phase, pwr, goal);
		if (power > Integer.MIN_VALUE && power < Integer.MAX_VALUE) {
			if (goal == GoalType.MAXIMIZE) {
				return (int) Math.floor(power);
			} else {
				return (int) Math.ceil(power);
			}
		} else {
			this.logError(this.log, goal.name() + " Power for [" + ess.toString() + "," + phase.toString() + ","
					+ pwr.toString() + "=" + power + "] is out of bounds. Returning '0'");
			return 0;
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE:
			this.solver.solve();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.data.initializeCycle();
			break;
		}
	}

	protected StateChannel getNotSolvedChannel() {
		return this.channel(ChannelId.NOT_SOLVED);
	}

	protected IntegerReadChannel getSolveDurationChannel() {
		return this.channel(ChannelId.SOLVE_DURATION);
	}

	protected EnumReadChannel getSolveStrategyChannel() {
		return this.channel(ChannelId.SOLVE_STRATEGY);
	}

	public boolean isDebugMode() {
		return debugMode;
	}

	/**
	 * Prints all Constraints to the system log.
	 * 
	 * @param log         a logger instance
	 * @param title       the log title
	 * @param constraints a list of Constraints
	 */
	public static void debugLogConstraints(Logger log, String title, List<Constraint> constraints) {
		log.info(title);
		for (Constraint c : constraints) {
			log.info("- " + c);
		}
	}

	/**
	 * Gets the Ess component with the given ID.
	 * 
	 * @param essId the component ID of Ess
	 * @return an Ess instance
	 */
	protected ManagedSymmetricEss getEss(String essId) {
		return this.esss.get(essId);
	}

	/**
	 * Gets the EssType for the given Ess-ID.
	 * 
	 * @param essId the component ID of Ess
	 * @return the EssType
	 */
	protected EssType getEssType(String essId) {
		ManagedSymmetricEss ess = this.getEss(essId);
		return EssType.getEssType(ess);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	@Override
	public PidFilter getPidFilter() {
		return this.pidFilter;
	}

}
