package io.openems.common.oem;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;

/**
 * A default {@link OpenemsEdgeOem} for OpenEMS Edge.
 */
public class DummyOpenemsEdgeOem implements OpenemsEdgeOem {

	@Override
	public String getManufacturer() {
		return "OpenEMS Association e.V.";
	}

	@Override
	public String getManufacturerModel() {
		return "OpenEMS";
	}

	@Override
	public String getManufacturerOptions() {
		return "";
	}

	@Override
	public String getManufacturerVersion() {
		return "";
	}

	@Override
	public String getManufacturerSerialNumber() {
		return "";
	}

	@Override
	public String getManufacturerEmsSerialNumber() {
		return "";
	}

	@Override
	public String getBackendApiUrl() {
		return "ws://localhost:8081";
	}

	@Override
	public String getInfluxdbTag() {
		return "edge";
	}

	@Override
	public SystemUpdateParams getSystemUpdateParams() {
		return new SystemUpdateParams(null, null, null, null);
	}

	private final Map<String, String> appToWebsiteUrl = new ImmutableMap.Builder<String, String>() //
			.put("App.FENECON.Home", "https://fenecon.de/fenecon-home-10/") //
			.put("App.FENECON.Home.20", "https://fenecon.de/fenecon-home-20-30/") //
			.put("App.FENECON.Home.30", "https://fenecon.de/fenecon-home-20-30/") //
			.put("App.FENECON.Industrial.S.ISK010", "https://fenecon.de/fenecon-industrial-s/") //
			.put("App.FENECON.Industrial.S.ISK110", "https://fenecon.de/fenecon-industrial-s/") //
			.put("App.FENECON.Industrial.S.ISK011", "https://fenecon.de/fenecon-industrial-s/") //
			.put("App.TimeOfUseTariff.Awattar", "") //
			.put("App.TimeOfUseTariff.ENTSO-E", "") //
			.put("App.TimeOfUseTariff.RabotCharge", "") //
			.put("App.TimeOfUseTariff.Stromdao", "") //
			.put("App.TimeOfUseTariff.Tibber", "") //
			.put("App.TimeOfUseTariff.RabotCharge", "") //
			.put("App.Api.ModbusTcp.ReadOnly", "") //
			.put("App.Api.ModbusTcp.ReadWrite", "") //
			.put("App.Api.RestJson.ReadOnly", "") //
			.put("App.Api.RestJson.ReadWrite", "") //
			.put("App.Evcs.HardyBarth", "") //
			.put("App.Evcs.Keba", "") //
			.put("App.Evcs.IesKeywatt", "") //
			.put("App.Evcs.Alpitronic", "") //
			.put("App.Evcs.Webasto.Next", "") //
			.put("App.Evcs.Webasto.Unite", "") //
			.put("App.Evcs.Cluster", "") //
			.put("App.Hardware.KMtronic8Channel", "") //
			.put("App.Heat.HeatPump", "") //
			.put("App.Heat.CHP", "") //
			.put("App.Heat.HeatingElement", "") //
			.put("App.PvSelfConsumption.GridOptimizedCharge", "") //
			.put("App.PvSelfConsumption.SelfConsumptionOptimization", "") //
			.put("App.LoadControl.ManualRelayControl", "") //
			.put("App.LoadControl.ThresholdControl", "") //
			.put("App.Meter.Socomec", "") //
			.put("App.Meter.CarloGavazzi", "") //
			.put("App.Meter.Janitza", "") //
			.put("App.PvInverter.Fronius", "") //
			.put("App.PvInverter.Kaco", "") //
			.put("App.PvInverter.Kostal", "") //
			.put("App.PvInverter.Sma", "") //
			.put("App.PvInverter.SolarEdge", "") //
			.put("App.PeakShaving.PeakShaving", "") //
			.put("App.PeakShaving.PhaseAccuratePeakShaving", "") //
			.put("App.Ess.FixActivePower", "") //
			.put("App.Ess.FixStateOfCharge", "") //
			.put("App.Ess.PowerPlantController", "") //
			.put("App.Ess.PrepareBatteryExtension", "") //
			.build();

	// NOTE: this will certainly get refactored in future, but it's a good start to
	// simplify creation of OpenEMS distributions.
	@Override
	public String getAppWebsiteUrl(String appId) {
		return this.appToWebsiteUrl.get(appId);
	}

	/**
	 * Helper method for JUnit tests. Tests if the given {@link OpenemsEdgeOem}
	 * provides the same Website-URLs as {@link DummyOpenemsEdgeOem} - (i.e. all are
	 * not-null. See {@link #getAppWebsiteUrl(String)}
	 * 
	 * @param oem the {@link OpenemsEdgeOem}
	 */
	public static void assertAllWebsiteUrlsSet(OpenemsEdgeOem oem) throws OpenemsException {
		var dummy = new DummyOpenemsEdgeOem();
		var missing = dummy.appToWebsiteUrl.keySet().stream() //
				.filter(appId -> oem.getAppWebsiteUrl(appId) == null) //
				.toList();
		if (!missing.isEmpty()) {
			throw new OpenemsException("Missing Website-URLs in Edge-OEM for [" + String.join(", ", missing) + "]");
		}
	}
}