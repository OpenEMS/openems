package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.DynamicCharge", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DynamicCharge extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);
	private final CalculateConsumption calculateTotalConsumption = new CalculateConsumption(this);
	private static TreeMap<LocalDateTime, Long> chargeSchedule = new TreeMap<LocalDateTime, Long>();
	boolean executed = false;

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DynamicCharge() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		
		
		LocalDateTime now = LocalDateTime.now();
		int hourOfDay = now.getHour();

		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter gridMeter = this.componentManager.getComponent(this.config.meter_id());
		Sum sum = this.componentManager.getComponent(OpenemsConstants.SUM_ID);

		log.info("Calculating the required consumption to charge ");
		this.calculateTotalConsumption.run(ess, gridMeter, config, sum);

		// Hours and Amount of energy to charge in the form of TreeMap
		if (CalculateConsumption.t0 != null) {
			if (!executed && hourOfDay == 17 && !CalculateConsumption.chargeSchedule.isEmpty()) {
				chargeSchedule = CalculateConsumption.chargeSchedule;
				executed = true;
			}
		}
		if (CalculateConsumption.t1 != null) {
			if (executed && hourOfDay == 9) {
				chargeSchedule = null;
				executed = false;
			}
		}

		// Charge Condition
		if (!chargeSchedule.isEmpty()) {
			for (Map.Entry<LocalDateTime, Long> entry : chargeSchedule.entrySet()) {
				if (now.getHour() == entry.getKey().getHour()) {

					/*
					 * TODO Actual condition to charge the ESS
					 */
				System.out.println("Charging");

					/*
					 * int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(ess, Phase.ALL,
					 * Pwr.ACTIVE, entry.getValue());
					 * 
					 * ess.addPowerConstraintAndValidate("SymmetricFixActivePower", Phase.ALL,
					 * Pwr.ACTIVE, Relationship.EQUALS, calculatedPower);
					 */
				}

			}
		}

	}

}
