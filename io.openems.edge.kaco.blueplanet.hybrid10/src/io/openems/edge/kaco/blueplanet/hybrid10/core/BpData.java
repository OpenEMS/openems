package io.openems.edge.kaco.blueplanet.hybrid10.core;

import java.util.stream.Stream;

import com.ed.data.BatteryData;
import com.ed.data.DataSet;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.SystemInfo;
import com.ed.data.VectisData;
import com.ed.edcom.Client;

public class BpData {

	/**
	 * Build a {@link BpData} object initialized by {@link Client}.
	 * 
	 * @param client the {@link Client}
	 * @return the {@link BpData}
	 * @throws Exception on error
	 */
	public static BpData from(Client client) throws Exception {
		var battery = new BatteryData();
		var inverter = new InverterData();
		var status = new Status();
		var settings = new Settings();
		var energy = new EnergyMeter();
		var vectis = new VectisData();
		var systemInfo = new SystemInfo();

		return new BpData(client, battery, inverter, status, settings, vectis, energy, systemInfo);
	}

	private final DataSet[] all;
	public final BatteryData battery;
	public final InverterData inverter;
	public final Status status;
	public final Settings settings;
	public final VectisData vectis;
	public final EnergyMeter energy;
	public final SystemInfo systemInfo;

	private BpData(Client client, BatteryData battery, InverterData inverter, Status status, Settings settings,
			VectisData vectis, EnergyMeter energy, SystemInfo systemInfo) {
		this.battery = battery;
		this.inverter = inverter;
		this.status = status;
		this.settings = settings;
		this.vectis = vectis;
		this.energy = energy;
		this.systemInfo = systemInfo;

		// Prepare array of all DataSets for convenience
		this.all = new DataSet[] { battery, inverter, status, settings, energy, vectis, systemInfo };

		// Register DataSets with Client
		Stream.of(this.all) //
				.forEach(d -> d.registerData(client));
	}

	/**
	 * Refresh all {@link DataSet}s.
	 */
	protected void refreshAll() {
		Stream.of(this.all) //
				.forEach(d -> d.refresh());
	}
}
