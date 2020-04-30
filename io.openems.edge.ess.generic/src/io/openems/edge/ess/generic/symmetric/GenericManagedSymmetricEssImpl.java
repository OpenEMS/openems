package io.openems.edge.ess.generic.symmetric;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter.BatteryInverterConstraint;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Power;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Generic.ManagedSymmetric", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class GenericManagedSymmetricEssImpl extends AbstractOpenemsComponent
		implements ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler {

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricBatteryInverter batteryInverter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Battery battery;

	private Config config = null;

	public GenericManagedSymmetricEssImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				GenericManagedSymmetricEss.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		// update filter for 'BatteryInverter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "batteryInverter",
				config.batteryInverter_id())) {
			return;
		}

		// update filter for 'Battery'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "battery", config.battery_id())) {
			return;
		}
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			// TODO: fill channels
			break;
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value().asString() //
				+ "|L:" + this.getActivePower().value().asString() //
				+ "|Allowed:" //
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER).value().asStringWithoutUnit() + ";" //
				+ this.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER).value().asString();
// TODO				+ "|" + this.channel(ChannelId.CURRENT_STATE).value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		// TODO Auto-generated method stub
	}

	/**
	 * Retrieves PowerPrecision from {@link SymmetricBatteryInverter}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public int getPowerPrecision() {
		return this.batteryInverter.getPowerPrecision();
	}

	/**
	 * Retrieves StaticConstraints from {@link SymmetricBatteryInverter}.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public Constraint[] getStaticConstraints() throws OpenemsNamedException {
		OpenemsNamedException error = null;

		BatteryInverterConstraint[] constraints = this.batteryInverter.getStaticConstraints();
		Constraint[] result = new Constraint[constraints.length];
		for (int i = 0; i < constraints.length; i++) {
			BatteryInverterConstraint c = constraints[i];
			try {
				result[i] = this.power.createSimpleConstraint(c.description, this, c.phase, c.pwr, c.relationship,
						c.value);
			} catch (OpenemsException e) {
				// catch errors here, so that remaining Constraints are still applied;
				// exception is thrown again later
				e.printStackTrace();
				error = e;
			}
		}

		if (error != null) {
			throw error; // if any error happened, throw the last one
		}
		return result;
	}
}
