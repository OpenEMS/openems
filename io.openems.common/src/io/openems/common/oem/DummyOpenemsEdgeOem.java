package io.openems.common.oem;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;

/**
 * A default {@link OpenemsEdgeOem} for OpenEMS Edge.
 */
public class DummyOpenemsEdgeOem implements OpenemsEdgeOem {

	private static final List<Language> REQUIRED_LANGUAGES = List.of(//
			Language.DE, //
			Language.EN //
	);

	private String openCageApiKey;
	private String openMeteoApiKey;

	public DummyOpenemsEdgeOem() {
	}

	/**
	 * Sets the Open-Meteo API key to be used by this {@link DummyOpenemsEdgeOem}
	 * instance.
	 *
	 * @param apiKey the Open-Meteo API key
	 * @return this {@link DummyOpenemsEdgeOem} instance for method chaining
	 */
	public DummyOpenemsEdgeOem withOpenMeteoApiKey(String apiKey) {
		this.openMeteoApiKey = apiKey;
		return this;
	}

	/**
	 * Sets the OpenCage API key to be used by this {@link DummyOpenemsEdgeOem}
	 * instance.
	 *
	 * @param apiKey the OpenCage API key
	 * @return this {@link DummyOpenemsEdgeOem} instance for method chaining
	 */
	public DummyOpenemsEdgeOem withOpenCageApiKey(String apiKey) {
		this.openCageApiKey = apiKey;
		return this;
	}

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

	private final Map<String, String> links = new ImmutableMap.Builder<String, String>()//
			.put("SwitchEvcsLink", "") //
			.build();

	private final Map<String, AppLink> appToWebsiteUrl = new ImmutableMap.Builder<String, AppLink>() //
			.put("App.FENECON.Home", AppLink.create() //
					.addLink(Language.DE, "https://docs.fenecon.de/de/home/home_10-1_overview.html")//
					.addLink(Language.EN, "https://docs.fenecon.de/en/home/home_10-1_overview.html")//
			) //
			.put("App.FENECON.Home.20", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-home-20-30/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-home-20-30/") //
			) //
			.put("App.FENECON.Home.30", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-home-20-30/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-home-20-30/") //
			) //
			.put("App.FENECON.Home6", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-home-6-10-15/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-home-6-10-15/") //
			) //
			.put("App.FENECON.Home10.Gen2", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-home-6-10-15/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-home-6-10-15/") //
			) //
			.put("App.FENECON.Home15", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-home-6-10-15/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-home-6-10-15/") //
			) //
			.put("App.FENECON.Commercial.50.Gen3", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-commercial-50/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-commercial-50/") //
			) //
			.put("App.FENECON.Commercial.92", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-commercial-92/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-commercial-92/") //
			) //
			.put("App.FENECON.Commercial.100", AppLink.create() //
					.addLink(Language.DE, "https://www.fenecon.de/fenecon-commercial-100/") //
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-commercial-100/")) //
			.put("App.FENECON.Commercial.92.ClusterMaster", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-commercial-92/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-commercial-92/") //
			) //
			.put("App.FENECON.Commercial.92.ClusterSlave", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-commercial-92/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-commercial-92/") //
			) //
			.put("App.FENECON.Industrial.L.ILK710", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-industrial-l/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-industrial-l/") //
			) //
			.put("App.FENECON.Industrial.S.ISK010", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-industrial-s/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-industrial-s/") //
			) //
			.put("App.FENECON.Industrial.S.ISK110", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-industrial-s/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-industrial-s/") //
			) //
			.put("App.FENECON.Industrial.S.ISK011", AppLink.create() //
					.addLink(Language.DE, "https://fenecon.de/fenecon-industrial-s/")
					.addLink(Language.EN, "https://www.fenecon.com/fenecon-industrial-s/") //
			) //

			.put("App.FENECON.ProHybrid.10", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.FENECON.ProHybrid.GW", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.FENECON.ProHybrid.9.10", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //

			.put("App.System.Fenecon.Home", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.AncillaryCosts", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.LuoxEnergy", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.Awattar", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.ENTSO-E", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.GroupeE", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.Hassfurt", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.OctopusGo", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.OctopusHeat", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.RabotCharge", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.Stromdao", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.Swisspower", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.TimeOfUseTariff.Tibber", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Cloud.EnerixControl", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Cloud.Clever-PV", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.ModbusTcp.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.ModbusTcp.ReadWrite", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.ModbusRtu.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.ModbusRtu.ReadWrite", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.RestJson.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Api.RestJson.ReadWrite", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Timedata.InfluxDb", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Abl.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Alpitronic", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Cluster", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.HardyBarth", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.HardyBarth.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.IesKeywatt", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Keba", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Keba.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Goe.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Heidelberg.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Mennekes.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Webasto.Next", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evcs.Webasto.Unite", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Hardware.IoGpio", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evse.ElectricVehicle.Generic", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evse.ChargePoint.Keba", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evse.ChargePoint.Mennekes", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Evse.Controller.Cluster", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Hardware.KMtronic8Channel", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Heat.HeatPump", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Heat.CHP", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Heat.HeatingElement", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Heat.Askoma.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Heat.MyPv.ReadOnly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvSelfConsumption.GridOptimizedCharge", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvSelfConsumption.SelfConsumptionOptimization", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.LoadControl.ManualRelayControl", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.LoadControl.ThresholdControl", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Shelly", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Shelly.Meter", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Socomec", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.CarloGavazzi", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.PqPlus", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Janitza", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.GridMeter.Janitza", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.GridMeter.GoodWe", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.GridMeter.Kdk", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Discovergy", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.PhoenixContact", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Eastron", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Meter.Kdk", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.BeagleBoneBlack", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.Compulab", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.CM3", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.CM4", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.CM4Max", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.CM4S", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.OpenemsHardware.CM4S.Gen2", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvInverter.Fronius", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvInverter.Kaco", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvInverter.Kostal", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvInverter.Sma", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PvInverter.SolarEdge", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PeakShaving.PeakShaving", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PeakShaving.PhaseAccuratePeakShaving", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.PeakShaving.TimeSlotPeakShaving", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.FixActivePower", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.FixStateOfCharge", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.PowerPlantController", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.PrepareBatteryExtension", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.Limiter14a", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Prediction.Default", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Prediction.UnmanagedConsumption", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.put("App.Ess.SohCycle", AppLink.create() //
					.emptyLink(Language.DE) //
					.emptyLink(Language.EN) //
			) //
			.build();

	@Override
	public String getLink(String key) {
		return this.links.get(key);
	}

	// NOTE: this will certainly get refactored in future, but it's a good start to
	// simplify creation of OpenEMS distributions.
	@Override
	public String getAppWebsiteUrl(String appId, Language language) {
		return OpenemsEdgeOem.getAppWebsiteUrlFromMap(this.appToWebsiteUrl, appId, language);
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

		for (var language : REQUIRED_LANGUAGES) {
			var missing = dummy.appToWebsiteUrl.keySet().stream()
					.filter(appId -> oem.getAppWebsiteUrl(appId, language) == null) //
					.toList();

			if (!missing.isEmpty()) {
				throw new OpenemsException(
						"Missing " + language + " Website-URLs in Edge-OEM for [" + String.join(", ", missing) + "]");
			}
		}

		// fallback test (e.g. unsupported language should fallback to english)
		var fallbackMissing = dummy.appToWebsiteUrl.keySet().stream()
				.filter(appId -> oem.getAppWebsiteUrl(appId, Language.CZ) == null) //
				.toList();

		if (!fallbackMissing.isEmpty()) {
			throw new OpenemsException("Fallback does not work for [" + String.join(", ", fallbackMissing) + "]");
		}
	}

	@Override
	public String getOpenCageApiKey() {
		return this.openCageApiKey;
	}

	@Override
	public String getOpenMeteoApiKey() {
		return this.openMeteoApiKey;
	}
}
