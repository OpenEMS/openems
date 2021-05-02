package io.openems.edge.ess.generic.symmetric;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.generic.common.GenericManagedEss;

public interface GenericManagedSymmetricEss extends GenericManagedEss, ManagedSymmetricEss, SymmetricEss,
		OpenemsComponent, EventHandler, StartStoppable, ModbusSlave {

}
