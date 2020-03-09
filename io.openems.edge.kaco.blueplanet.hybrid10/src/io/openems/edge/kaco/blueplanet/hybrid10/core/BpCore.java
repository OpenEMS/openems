package io.openems.edge.kaco.blueplanet.hybrid10.core;

import com.ed.data.BatteryData;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.VectisData;

import io.openems.edge.common.channel.Doc;

public interface BpCore {
	public BatteryData getBatteryData();

	public InverterData getInverterData();

	public Status getStatusData();

	public boolean isConnected();

	public Settings getSettings();

	public VectisData getVectis();

	public EnergyMeter getEnergyMeter();

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
}
