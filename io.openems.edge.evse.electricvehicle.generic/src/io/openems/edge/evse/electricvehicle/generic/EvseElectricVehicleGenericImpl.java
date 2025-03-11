package io.openems.edge.evse.electricvehicle.generic;

import static io.openems.edge.evse.api.EvseConstants.MIN_CURRENT;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import com.google.common.collect.ImmutableList;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.Limit;
import io.openems.edge.evse.api.SingleThreePhase;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;
import io.openems.edge.evse.api.electricvehicle.Profile;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ElectricVehicle.Generic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvseElectricVehicleGenericImpl extends AbstractOpenemsComponent
		implements EvseElectricVehicleGeneric, EvseElectricVehicle, OpenemsComponent {

	private ChargeParams chargeParams = new ChargeParams(ImmutableList.of(), ImmutableList.of());

	public EvseElectricVehicleGenericImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				EvseElectricVehicle.ChannelId.values(), //
				EvseElectricVehicleGeneric.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		var limits = ImmutableList.<Limit>builder();
		if (config.maxCurrentSinglePhase() > MIN_CURRENT) {
			limits.add(new Limit(SingleThreePhase.SINGLE, MIN_CURRENT, config.maxCurrentSinglePhase()));
		}
		if (config.maxCurrentThreePhase() > MIN_CURRENT) {
			limits.add(new Limit(SingleThreePhase.THREE, MIN_CURRENT, config.maxCurrentThreePhase()));
		}

		var profiles = ImmutableList.<Profile>builder();
		if (!config.canInterrupt()) {
			profiles.add(Profile.NO_INTERRUPT);
		}
		this.chargeParams = new ChargeParams(limits.build(), profiles.build());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public ChargeParams getChargeParams() {
		return this.chargeParams;
	}
}