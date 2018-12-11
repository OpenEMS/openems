package io.openems.edge.controller.pvinverter.fixpowerlimit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.PvInverter.FixPowerLimit", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PvInverterFixPowerLimit extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PvInverterFixPowerLimit.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * the configured Power Limit
	 */
	private int powerLimit = 0;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'pvInverter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "pvInverter", config.pvInverter_id())) {
			return;
		}

		this.powerLimit = config.powerLimit();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricPvInverter pvInverter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		try {
			this.pvInverter.getActivePowerLimit().setNextWriteValue(this.powerLimit);
		} catch (OpenemsException e) {
			this.logError(this.log, "Unable to set ActivePowerLimit on Inverter: " + e.getMessage());
		}
	}

}
