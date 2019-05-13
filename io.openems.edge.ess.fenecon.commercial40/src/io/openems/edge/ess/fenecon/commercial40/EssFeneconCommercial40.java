package io.openems.edge.ess.fenecon.commercial40;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface EssFeneconCommercial40
		extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave {

	public Integer getUnitId();

	public String getModbusBridgeId();

}
