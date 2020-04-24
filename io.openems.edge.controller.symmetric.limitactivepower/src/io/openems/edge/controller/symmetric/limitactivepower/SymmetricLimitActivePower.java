package io.openems.edge.controller.symmetric.limitactivepower;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.LimitActivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricLimitActivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(SymmetricLimitActivePower.class);

	@Reference
	protected ComponentManager componentManager;

	private String essId;

	/**
	 * the configured Max Charge ActivePower
	 * 
	 * value is zero or negative
	 */
	private int maxChargePower = 0;
	/**
	 * the configured Max Discharge ActivePower
	 * 
	 * value is zero or positive
	 */
	private int maxDischargePower = 0;

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

	public SymmetricLimitActivePower() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.essId = config.ess_id();
		this.maxChargePower = config.maxChargePower() * -1;
		this.maxDischargePower = config.maxDischargePower();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);

		// adjust value so that it fits into Min/MaxActivePower
		int maxPower = ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
		int minPower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int calculatedMaxDischargePower = fitIntoMinMax(minPower, maxPower, this.maxDischargePower);
		int calculatedMaxChargePower = fitIntoMinMax(minPower, maxPower, this.maxChargePower);

		// set result
		ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
				Relationship.GREATER_OR_EQUALS, calculatedMaxChargePower);
		ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
				Relationship.LESS_OR_EQUALS, calculatedMaxDischargePower);
	}

	private static int fitIntoMinMax(int min, int max, int value) {
		if (value > max) {
			value = max;
		}
		if (value < min) {
			value = min;
		}
		return value;
	}
}
