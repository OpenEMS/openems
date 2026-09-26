package io.openems.edge.bridge.eebus.peer;

import io.openems.edge.bridge.eebus.api.EebusPeer;
import org.openmuc.jeebus.ship.api.ShipConnectionInfoSnapshot;

public interface InternalEebusPeer extends EebusPeer {

	void updateConnectionInfo(ShipConnectionInfoSnapshot connectionInfo);
}
