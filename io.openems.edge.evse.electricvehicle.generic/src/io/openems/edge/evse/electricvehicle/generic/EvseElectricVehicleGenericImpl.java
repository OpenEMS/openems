package io.openems.edge.evse.electricvehicle.generic;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.electricvehicle.EvseElectricVehicle;
import io.openems.edge.evse.api.electricvehicle.Profile.ElectricVehicleAbilities;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ElectricVehicle.Generic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvseElectricVehicleGenericImpl extends AbstractOpenemsComponent
		implements EvseElectricVehicleGeneric, EvseElectricVehicle, OpenemsComponent {

	private ElectricVehicleAbilities electricVehicleAbilities;

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

		final var abilities = ElectricVehicleAbilities.create();

		if (config.maxPowerSinglePhase() >= config.minPowerSinglePhase()) {
			abilities.setSinglePhaseLimitInWatt(config.minPowerSinglePhase(), config.maxPowerSinglePhase());
		}
		if (config.maxPowerThreePhase() >= config.minPowerThreePhase()) {
			abilities.setThreePhaseLimitInWatt(config.minPowerThreePhase(), config.maxPowerThreePhase());
		}
		abilities.setCanInterrupt(config.canInterrupt());
		this.electricVehicleAbilities = abilities.build();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public ElectricVehicleAbilities getElectricVehicleAbilities() {
		return this.electricVehicleAbilities;
	}
}
