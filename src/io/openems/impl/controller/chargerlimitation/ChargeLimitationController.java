package io.openems.impl.controller.chargerlimitation;

import java.util.List;

import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "Limit battery charge from DC", description = "Limits the maximum charge of the battery from DC connected charger.")
public class ChargeLimitationController extends Controller {

	/*
	 * Constructors
	 */

	public ChargeLimitationController() {
		super();
	}

	public ChargeLimitationController(String thingId) {
		super(thingId);
	}

	/*
	 * Config
	 */
	@ConfigInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);

	@ConfigInfo(title = "Chargers", description = "Sets the chargers.", type = Charger.class, isArray = true)
	public ConfigChannel<List<Charger>> chargers = new ConfigChannel<>("chargers", this);

	/*
	 * Methods
	 */
	@Override
	public void run() {
		// try {
		// Ess ess = this.ess.value();
		// List<Charger> chargers = this.chargers.value();
		// // calculate maximal chargePower
		// float power = ess.allowedCharge.value() + ess.getWrittenActivePower();
		// if (power > 0) {
		// float maxCurrent = 0l;
		// for (Charger c : chargers) {
		// maxCurrent += c.nominalCurrent.value();
		// }
		// for (Charger c : chargers) {
		// c.setPower(power / maxCurrent * c.nominalCurrent.value());
		// }
		// ess.setMaxCharge(ess.allowedCharge.value() - power);
		// } else {
		// for (Charger c : chargers) {
		// c.setPower(0);
		// }
		// }
		// } catch (InvalidValueException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (WriteChannelException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

}
