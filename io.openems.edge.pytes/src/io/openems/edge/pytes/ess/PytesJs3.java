package io.openems.edge.pytes.ess;

import org.osgi.service.event.EventHandler;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.dccharger.PytesDcChargerImpl;

public interface PytesJs3 extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}


	/**
	 * Adds DC-charger to ESS hybrid system. Represents PV production
	 * 
	 * @param charger link to DC charger(s)
	 */
	public void addCharger(PytesDcCharger charger);

	/**
	 * Removes link to pv DC charger.
	 * 
	 * @param charger charger
	 */
	public void removeCharger(PytesDcCharger charger);

	/**
	 * returns ModbusBrdigeId from config.
	 * 
	 * @return ModbusBrdigeId from config
	 */
	public String getModbusBridgeId();

	/**
	 * returns UnitId for ESS from config.
	 * 
	 * @return UnitId for ESS from config
	 */
	public int getUnitId();

}
