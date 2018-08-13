package com.ed.openems.centurio.datasource.api;

import com.ed.data.BatteryData;
import com.ed.data.EnergyMeter;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.VectisData;


public interface EdComData {
	BatteryData getBatteryData();
	InverterData getInverterData();
	Status getStatusData();
	boolean isConnected();
	Settings getSettings();
	VectisData getVectis();
	EnergyMeter getEnergyMeter();
}
