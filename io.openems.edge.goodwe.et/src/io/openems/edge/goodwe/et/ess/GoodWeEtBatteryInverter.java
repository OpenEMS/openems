package io.openems.edge.goodwe.et.ess;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.goodwe.et.charger.AbstractGoodWeEtCharger;

public interface GoodWeEtBatteryInverter extends SymmetricEss, OpenemsComponent {

	public Integer getUnitId();

	public String getModbusBridgeId();

	public void addCharger(AbstractGoodWeEtCharger charger);

	public void removeCharger(AbstractGoodWeEtCharger charger);	

}
