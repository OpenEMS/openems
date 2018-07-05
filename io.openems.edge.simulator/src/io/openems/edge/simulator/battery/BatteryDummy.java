package io.openems.edge.simulator.battery;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;



@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Bms.Simulator", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class BatteryDummy extends AbstractOpenemsComponent implements Battery, OpenemsComponent {

}
