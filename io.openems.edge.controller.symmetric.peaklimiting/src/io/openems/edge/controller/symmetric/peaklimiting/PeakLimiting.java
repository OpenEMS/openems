package io.openems.edge.controller.symmetric.peaklimiting;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.PeakLimiting")
public class PeakLimiting extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PeakLimiting.class);

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

	public PeakLimiting() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	private Config config;

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
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		SymmetricMeter pvmeter = this.componentManager.getComponent(this.config.meter_id());

		this.method1(pvmeter, ess);

	}

	private boolean method1(SymmetricMeter pvmeter, ManagedSymmetricEss ess) throws OpenemsNamedException {

		int pvpower = pvmeter.getActivePower().value().orElse(0);
		int soc = ess.getSoc().value().orElse(0);
		int maxP = this.config.maxPower();
		int calculatedPower = 0;

		if (pvpower > 0) {
			IntegerReadChannel consumptionChannel = this.componentManager
					.getChannel(ChannelAddress.fromString("_sum/ConsumptionActivePower"));
			int consumption = consumptionChannel.value().orElse(0);

			int surplus = pvpower - consumption;

			if ((surplus <= maxP) && soc == this.config.maxSOC()) { //don't charge
				
				ess.addPowerConstraintAndValidate("SymmetricPeakLimiting", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
						0.0001);
				return true;
			}

			if ((surplus > maxP) && soc > this.config.maxSOC()) { // feed in maxP
				
				int chargePower = (surplus - maxP) * -1;
				
				calculatedPower = ess.getPower().fitValueIntoMinMaxPower(ess, Phase.ALL, Pwr.ACTIVE, chargePower);
				ess.addPowerConstraintAndValidate("SymmetricPeakLimiting", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
						calculatedPower);
				return true;
			}

		}

		ess.addPowerConstraintAndValidate("SymmetricPeakLimiting", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
				calculatedPower);
		return false;

	}
}
