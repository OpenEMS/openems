package io.openems.edge.ess.sinexcel;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.ess.generic.common.AbstractAllowedChargeDischargeHandler;

public class AllowedChargeDischargeHandler extends AbstractAllowedChargeDischargeHandler<EssSinexcel> {

	public AllowedChargeDischargeHandler(EssSinexcel parent) {
		super(parent);
	}

	@Override
	public void accept(ClockProvider clockProvider, Battery battery) {
		this.calculateAllowedChargeDischargePower(clockProvider, battery);

		// Apply AllowedChargePower and AllowedDischargePower
		this.parent
				._setAllowedChargePower(Math.round(this.lastBatteryAllowedChargePower * -1 /* invert charge power */));
		this.parent._setAllowedDischargePower(Math.round(this.lastBatteryAllowedDischargePower));
	}

}
