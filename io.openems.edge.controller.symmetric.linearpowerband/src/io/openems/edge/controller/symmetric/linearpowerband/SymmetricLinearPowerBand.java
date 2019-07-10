package io.openems.edge.controller.symmetric.linearpowerband;

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
@Component(name = "Controller.Symmetric.LinearPowerBand", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricLinearPowerBand extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	// private final Logger log =
	// LoggerFactory.getLogger(SymmetricLinearPowerBand.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config;
	private int currentPower = 0;
	private State state = State.DOWNWARDS;

	private enum State {
		DOWNWARDS, UPWARDS
	}

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

	public SymmetricLinearPowerBand() {
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
		switch (this.state) {
		case DOWNWARDS:
			// adjust Power
			this.currentPower -= this.config.adjustPower();
			if (this.currentPower < this.config.minPower()) {
				// switch to discharge
				this.state = State.UPWARDS;
			}
			break;
		case UPWARDS:
			// adjust Power
			this.currentPower += this.config.adjustPower();
			if (this.currentPower > this.config.maxPower()) {
				// switch to discharge
				this.state = State.DOWNWARDS;
			}
			break;
		}

		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		// adjust value so that it fits into Min/MaxActivePower
		int calculatedPower = ess.getPower().fitValueIntoMinMaxPower(ess, Phase.ALL, Pwr.ACTIVE, this.currentPower);

		/*
		 * set result
		 */
		ess.addPowerConstraintAndValidate("SymmetricLinearPowerBand", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
				calculatedPower); //
	}
}
