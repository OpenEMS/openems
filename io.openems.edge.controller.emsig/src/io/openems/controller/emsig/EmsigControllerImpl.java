package io.openems.controller.emsig;

import static io.openems.controller.emsig.ojalgo.Constants.ESS_MAX_ENERGY;
import static io.openems.controller.emsig.ojalgo.Constants.GRID_SELL_LIMIT;
import static io.openems.controller.emsig.ojalgo.Constants.MINUTES_PER_PERIOD;
import static io.openems.controller.emsig.ojalgo.Constants.NO_OF_PERIODS;
import static java.math.BigDecimal.ONE;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import org.ojalgo.optimisation.Expression;
import org.ojalgo.optimisation.Variable;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.controller.emsig.ojalgo.EnergyModel;
import io.openems.controller.emsig.ojalgo.Period;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.EMSIG", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EmsigControllerImpl extends AbstractOpenemsComponent
		implements EmsigController, Controller, OpenemsComponent {

	@Reference
	private ClockProvider clockProvider;

	@Reference
	private ManagedSymmetricEss ess;

	private Config config = null;

	private Schedule schedule = null;

	public EmsigControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EmsigController.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		Instant now = Instant.now();

		// At least once very period, i.e. 15 minutes
		Flexibility flexibility = this.calculateFlexibility();
		flexibility.prettyPrint();

		// Create the actual optimal Schedule
		EnergyModel schedule = this.calculateSchedule();
		schedule.prettyPrint();
		this.schedule = new Schedule(LocalDateTime.now(this.clockProvider.getClock()), schedule);

		System.out.println("Time: " + Duration.between(now, Instant.now()).toMillis() + "ms");
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.schedule == null) {
			return;
		}

		LocalDateTime now = LocalDateTime.now(this.clockProvider.getClock());
		int periodIndex = (int) (Duration.between(this.schedule.startTime, now).getSeconds() / 60 / MINUTES_PER_PERIOD);
		if (periodIndex > schedule.energyModel.periods.length - 1) {
			return;
		}

		Period period = schedule.energyModel.periods[periodIndex];
		System.out.println(
				now + " - Period [" + periodIndex + "] Power [" + period.ess.power.getValue().intValue() + "]");
		ess.setActivePowerEquals(period.ess.power.getValue().intValue());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private static class Flexibility {
		protected final int[] maxGridBuy = new int[NO_OF_PERIODS];
		protected final int[] maxGridSell = new int[NO_OF_PERIODS];

		public void prettyPrint() {
			for (int i = 0; i < NO_OF_PERIODS; i++) {
				System.out.println(String.format("%2d | %s %5d | %s %5d", i, //
						"MaxBuy", maxGridBuy[i], //
						"MaxSell", maxGridSell[i] //
				));
			}
		}
	}

	private Flexibility calculateFlexibility() {
		Flexibility flexibility = new Flexibility();

		EnergyModel em = null;
		for (int i = 0; i < NO_OF_PERIODS; i++) {
			em = new EnergyModel();

			// Add Schedule-Constraints
			this.addScheduleConstraints(em);

			// Target function
			em.model.addExpression("Extreme Grid Power") //
					.set(em.periods[i].grid.power, ONE) //
					.weight(ONE);

			em.model.maximise();
			flexibility.maxGridBuy[i] = em.periods[i].grid.buy.power.getValue().intValue();
			em.model.minimise();
			flexibility.maxGridSell[i] = em.periods[i].grid.sell.power.getValue().intValue();
		}

		return flexibility;
	}

	private static class Schedule {
		protected final LocalDateTime startTime;
		protected final EnergyModel energyModel;

		public Schedule(LocalDateTime startTime, EnergyModel energyModel) {
			this.startTime = startTime;
			this.energyModel = energyModel;
		}
	}

	private EnergyModel calculateSchedule() {
		EnergyModel em = new EnergyModel();

		// Add Schedule-Constraints
		this.addScheduleConstraints(em);

		// Grid Buy Cost
		Variable gridBuyCostSum = em.model.addVariable("Grid_Buy_Cost_Sum");
		Expression gridBuyCostSumExpr = em.model.addExpression("Grid_Buy_Cost_Sum_Expr") //
				.set(gridBuyCostSum, ONE.negate());
		for (Period p : em.periods) {
			gridBuyCostSumExpr.set(p.grid.buy.power, p.grid.buy.cost);
		}
		gridBuyCostSumExpr.level(0);

		// Grid Sell Revenue
		Variable gridSellRevenueSum = em.model.addVariable("Grid_Sell_Revenue_Sum");
		Expression gridSellRevenueSumExpr = em.model.addExpression("Grid_Sell_Revenue_Sum_Expr") //
				.set(gridSellRevenueSum, ONE.negate()); //
		for (Period p : em.periods) {
			gridSellRevenueSumExpr.set(p.grid.sell.power, p.grid.sell.revenue);
		}
		gridSellRevenueSumExpr.level(0);

		// Target function: Grid Exchange Cost
		em.model.addExpression("Grid Exchange Cost Sum") //
				.set(gridBuyCostSum, ONE) //
				.set(gridSellRevenueSum, ONE.negate()) //
				.weight(ONE);

		// TODO additional target function: charge power should be evenly distributed;
		// i.e. minimise squared error between last period and current period power

		em.model.minimise();
		// Result result = em.model.minimise();

		return em;
	}

	/**
	 * Add Schedule-Constraints.
	 * 
	 * @param em
	 */
	private void addScheduleConstraints(EnergyModel em) {
		/*
		 * Schedule-Constraints for HLZF Controller
		 */

		// At end of HLZF battery is expected to be empty
		em.model.addExpression("End of 1st HLZF") //
				.set(em.periods[5].ess.energy, ONE) //
				.level(0);

		// At beginning of HLZF battery must be full
		em.model.addExpression("Beginning of 2nd HLZ") //
				.set(em.periods[18].ess.energy, ONE) //
				.level(ESS_MAX_ENERGY * 60);

		// At end of HLZF battery is expected to be empty
		em.model.addExpression("End of 2nd HLZ") //
				.set(em.periods[23].ess.energy, ONE) //
				.level(0);

		// TODO Grid-Sell can never be more than Production. This simple model assumes
		// no production, so Grid-Sell must be zero - at least outside of HLZF period.
		for (int i = 0; i < 6; i++) {
			em.periods[i].grid.sell.power.upper(GRID_SELL_LIMIT);
		}
		for (int i = 18; i < 23; i++) {
			em.periods[i].grid.sell.power.upper(GRID_SELL_LIMIT);
		}
	}

}
