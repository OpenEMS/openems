package io.openems.edge.controller.symmetric.dynamiccharge;

import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.DynamicCharge", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class DynamicCharge extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(DynamicCharge.class);
	
	private final CalculateConsumption calculateTotalConsumption = new CalculateConsumption(this);
	

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
		super.activate(context, config.id(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		LocalDateTime now = LocalDateTime.now();
		int hourOfDay = now.getHour();
		
		
		
		try {
			ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
			
			SymmetricMeter gridMeter = this.componentManager.getComponent(this.config.meter_id());
			
			Integer essActivePower = ess.getActivePower().value().orElse(0);
			Integer gridActivePower = gridMeter.getActivePower().value().orElse(0);
			Integer production = essActivePower + gridActivePower;
			Integer consumption = 0;
			
			
			log.info("Calculating the required consumption to charge ");
			this.calculateTotalConsumption.run();
			
			
			
			
		} catch (OpenemsNamedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
}
