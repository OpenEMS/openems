package io.openems.edge.core.appmanager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestADependencyToC;
import io.openems.edge.app.TestBDependencyToC;
import io.openems.edge.app.TestC;
import io.openems.edge.app.TestComponentDefConfig;
import io.openems.edge.app.TestFilter;
import io.openems.edge.app.TestMapPropName;
import io.openems.edge.app.TestMultipleIds;
import io.openems.edge.app.TestPermissions;
import io.openems.edge.app.api.AppCleverPv;
import io.openems.edge.app.api.AppEnerixControl;
import io.openems.edge.app.api.ModbusRtuApiReadOnly;
import io.openems.edge.app.api.ModbusRtuApiReadWrite;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.ModbusTcpApiReadWrite;
import io.openems.edge.app.api.MqttApi;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.api.RestJsonApiReadWrite;
import io.openems.edge.app.api.TimedataInfluxDb;
import io.openems.edge.app.ess.FixActivePower;
import io.openems.edge.app.ess.FixStateOfCharge;
import io.openems.edge.app.ess.Limiter14a;
import io.openems.edge.app.ess.PowerPlantController;
import io.openems.edge.app.ess.PrepareBatteryExtension;
import io.openems.edge.app.evcs.AlpitronicEvcs;
import io.openems.edge.app.evcs.DezonyEvcs;
import io.openems.edge.app.evcs.EvcsCluster;
import io.openems.edge.app.evcs.HardyBarthEvcs;
import io.openems.edge.app.evcs.IesKeywattEvcs;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.evcs.WebastoNextEvcs;
import io.openems.edge.app.evcs.WebastoUniteEvcs;
import io.openems.edge.app.evcs.readonly.AblEvcsReadOnly;
import io.openems.edge.app.evcs.readonly.AppGoeEvcsReadOnly;
import io.openems.edge.app.evcs.readonly.AppHardyBarthReadOnly;
import io.openems.edge.app.evcs.readonly.HeidelbergEvcsReadOnly;
import io.openems.edge.app.evcs.readonly.KebaEvcsReadOnly;
import io.openems.edge.app.evcs.readonly.MennekesEvcsReadOnly;
import io.openems.edge.app.evse.AppEvseCluster;
import io.openems.edge.app.evse.AppKebaEvse;
import io.openems.edge.app.evse.vehicle.AppGenericVehicle;
import io.openems.edge.app.hardware.GpioHardwareType;
import io.openems.edge.app.hardware.IoGpio;
import io.openems.edge.app.hardware.KMtronic8Channel;
import io.openems.edge.app.heat.CombinedHeatAndPower;
import io.openems.edge.app.heat.HeatAskomaReadOnly;
import io.openems.edge.app.heat.HeatMyPvReadOnly;
import io.openems.edge.app.heat.HeatPump;
import io.openems.edge.app.heat.HeatingElement;
import io.openems.edge.app.integratedsystem.FeneconHome10;
import io.openems.edge.app.integratedsystem.FeneconHome10Gen2;
import io.openems.edge.app.integratedsystem.FeneconHome15;
import io.openems.edge.app.integratedsystem.FeneconHome20;
import io.openems.edge.app.integratedsystem.FeneconHome30;
import io.openems.edge.app.integratedsystem.FeneconHome6;
import io.openems.edge.app.integratedsystem.FeneconProHybrid10;
import io.openems.edge.app.integratedsystem.TestFeneconHome10;
import io.openems.edge.app.integratedsystem.TestFeneconHome10Gen2;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.app.integratedsystem.TestFeneconHome30;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial50Gen3;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92ClusterMaster;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92ClusterSlave;
import io.openems.edge.app.integratedsystem.fenecon.industrial.l.Ilk710;
import io.openems.edge.app.integratedsystem.fenecon.industrial.s.Isk010;
import io.openems.edge.app.integratedsystem.fenecon.industrial.s.Isk011;
import io.openems.edge.app.integratedsystem.fenecon.industrial.s.Isk110;
import io.openems.edge.app.loadcontrol.ManualRelayControl;
import io.openems.edge.app.loadcontrol.ThresholdControl;
import io.openems.edge.app.meter.CarloGavazziMeter;
import io.openems.edge.app.meter.DiscovergyMeter;
import io.openems.edge.app.meter.EastronMeter;
import io.openems.edge.app.meter.JanitzaMeter;
import io.openems.edge.app.meter.KdkMeter;
import io.openems.edge.app.meter.PhoenixContactMeter;
import io.openems.edge.app.meter.PqPlusMeter;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.meter.gridmeter.GridMeterJanitza;
import io.openems.edge.app.openemshardware.BeagleBoneBlack;
import io.openems.edge.app.openemshardware.Compulab;
import io.openems.edge.app.openemshardware.TechbaseCm3;
import io.openems.edge.app.openemshardware.TechbaseCm4;
import io.openems.edge.app.openemshardware.TechbaseCm4Max;
import io.openems.edge.app.openemshardware.TechbaseCm4s;
import io.openems.edge.app.openemshardware.TechbaseCm4sGen2;
import io.openems.edge.app.peakshaving.PeakShaving;
import io.openems.edge.app.peakshaving.PhaseAccuratePeakShaving;
import io.openems.edge.app.peakshaving.TimeSlotPeakShaving;
import io.openems.edge.app.pvinverter.FroniusPvInverter;
import io.openems.edge.app.pvinverter.KacoPvInverter;
import io.openems.edge.app.pvinverter.KostalPvInverter;
import io.openems.edge.app.pvinverter.SmaPvInverter;
import io.openems.edge.app.pvinverter.SolarEdgePvInverter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.app.timeofusetariff.AncillaryCosts;
import io.openems.edge.app.timeofusetariff.AppLuoxEnergy;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.EntsoE;
import io.openems.edge.app.timeofusetariff.Ews;
import io.openems.edge.app.timeofusetariff.GroupeE;
import io.openems.edge.app.timeofusetariff.RabotCharge;
import io.openems.edge.app.timeofusetariff.StadtwerkHassfurt;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.app.timeofusetariff.Swisspower;
import io.openems.edge.app.timeofusetariff.Tibber;
import io.openems.edge.app.timeofusetariff.manual.OctopusGo;
import io.openems.edge.app.timeofusetariff.manual.OctopusHeat;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.meta.Meta;

public final class Apps {

	private Apps() {
	}

	/**
	 * Helper method for easier creation of a list of the used {@link OpenemsApp
	 * OpenemsApps}.
	 * 
	 * @param t            the {@link AppManagerTestBundle}
	 * @param appFunctions the methods to create the {@link OpenemsApp OpenemsApps}
	 * @return the list of the {@link OpenemsApp OpenemsApps}
	 */
	@SafeVarargs
	public static final List<OpenemsApp> of(AppManagerTestBundle t,
			Function<AppManagerTestBundle, OpenemsApp>... appFunctions) {
		return Arrays.stream(appFunctions) //
				.map(f -> f.apply(t)) //
				.collect(Collectors.toUnmodifiableList());
	}

	// Integrated Systems

	/**
	 * Test method for creating a {@link FeneconHome10}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome10 feneconHome10(AppManagerTestBundle t) {
		return app(t, FeneconHome10::new, "App.FENECON.Home");
	}

	/**
	 * Test method for creating a {@link FeneconHome6}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome6 feneconHome6(AppManagerTestBundle t) {
		return app(t, FeneconHome6::new, "App.FENECON.Home6");
	}

	/**
	 * Test method for creating a {@link FeneconHome10Gen2}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome10Gen2 feneconHome10Gen2(AppManagerTestBundle t) {
		return app(t, FeneconHome10Gen2::new, "App.FENECON.Home10.Gen2");
	}

	/**
	 * Test method for creating a {@link FeneconHome15}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome15 feneconHome15(AppManagerTestBundle t) {
		return app(t, FeneconHome15::new, "App.FENECON.Home15");
	}

	/**
	 * Test method for creating a {@link FeneconHome20}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome20 feneconHome20(AppManagerTestBundle t) {
		return app(t, FeneconHome20::new, "App.FENECON.Home.20");
	}

	/**
	 * Test method for creating a {@link FeneconHome30}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome30 feneconHome30(AppManagerTestBundle t) {
		return app(t, FeneconHome30::new, "App.FENECON.Home.30");
	}

	/**
	 * Test method for creating a {@link FeneconCommercial50Gen3}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconCommercial50Gen3 feneconCommercial50Gen3(AppManagerTestBundle t) {
		return app(t, FeneconCommercial50Gen3::new, "App.FENECON.Commercial.50.Gen3");
	}

	/**
	 * Test method for creating a {@link FeneconCommercial92}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconCommercial92 feneconCommercial92(AppManagerTestBundle t) {
		return app(t, FeneconCommercial92::new, "App.FENECON.Commercial.92");
	}

	/**
	 * Test method for creating a {@link FeneconCommercial92ClusterMaster}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconCommercial92ClusterMaster feneconCommercial92ClusterMaster(AppManagerTestBundle t) {
		return app(t, FeneconCommercial92ClusterMaster::new, "App.FENECON.Commercial.92.ClusterMaster");
	}

	/**
	 * Test method for creating a {@link FeneconCommercial92ClusterSlave}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconCommercial92ClusterSlave feneconCommercial92ClusterSlave(AppManagerTestBundle t) {
		return app(t, FeneconCommercial92ClusterSlave::new, "App.FENECON.Commercial.92.ClusterSlave");
	}

	/**
	 * Test method for creating a {@link Ilk710}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Ilk710 feneconIndustrialLIlk710(AppManagerTestBundle t) {
		return app(t, Ilk710::new, "App.FENECON.Industrial.L.ILK710");
	}

	/**
	 * Test method for creating a {@link Isk110}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Isk110 feneconIndustrialSIsk110(AppManagerTestBundle t) {
		return app(t, Isk110::new, "App.FENECON.Industrial.S.ISK110");
	}

	/**
	 * Test method for creating a {@link Isk010}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Isk010 feneconIndustrialSIsk010(AppManagerTestBundle t) {
		return app(t, Isk010::new, "App.FENECON.Industrial.S.ISK010");
	}

	/**
	 * Test method for creating a {@link Isk011}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Isk011 feneconIndustrialSIsk011(AppManagerTestBundle t) {
		return app(t, Isk011::new, "App.FENECON.Industrial.S.ISK011");
	}

	/**
	 * Test method for creating a {@link FeneconProHybrid10}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconProHybrid10 feneconProHybrid10(AppManagerTestBundle t) {
		return app(t, FeneconProHybrid10::new, "App.FENECON.ProHybrid.10");
	}

	// TimeOfUseTariff

	/**
	 * Test method for creating a {@link AncillaryCosts}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AncillaryCosts ancillaryCosts(AppManagerTestBundle t) {
		return app(t, AncillaryCosts::new, "App.TimeOfUseTariff.AncillaryCosts");
	}

	/**
	 * Test method for creating a {@link AppLuoxEnergy}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppLuoxEnergy luoxEnergy(AppManagerTestBundle t) {
		return app(t, AppLuoxEnergy::new, "App.TimeOfUseTariff.LuoxEnergy");
	}

	/**
	 * Test method for creating a {@link AwattarHourly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AwattarHourly awattarHourly(AppManagerTestBundle t) {
		return app(t, AwattarHourly::new, "App.TimeOfUseTariff.Awattar");
	}

	/**
	 * Test method for creating a {@link EntsoE}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final EntsoE entsoE(AppManagerTestBundle t) {
		return app(t, EntsoE::new, "App.TimeOfUseTariff.ENTSO-E");
	}

	/**
	 * Test method for creating a {@link Ews}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Ews ews(AppManagerTestBundle t) {
		return app(t, Ews::new, "App.TimeOfUseTariff.Ews");
	}

	/**
	 * Test method for creating a {@link GroupeE}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final GroupeE groupeE(AppManagerTestBundle t) {
		return app(t, GroupeE::new, "App.TimeOfUseTariff.GroupeE");
	}

	/**
	 * Test method for creating a {@link OctopusGo}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final OctopusGo octopusGo(AppManagerTestBundle t) {
		return app(t, OctopusGo::new, "App.TimeOfUseTariff.OctopusGo");
	}

	/**
	 * Test method for creating a {@link OctopusHeat}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final OctopusHeat octopusHeat(AppManagerTestBundle t) {
		return app(t, OctopusHeat::new, "App.TimeOfUseTariff.OctopusHeat");
	}

	/**
	 * Test method for creating a {@link StadtwerkHassfurt}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final StadtwerkHassfurt stadtwerkHassfurt(AppManagerTestBundle t) {
		return app(t, StadtwerkHassfurt::new, "App.TimeOfUseTariff.Hassfurt");
	}

	/**
	 * Test method for creating a {@link Swisspower}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Swisspower swisspower(AppManagerTestBundle t) {
		return app(t, Swisspower::new, "App.TimeOfUseTariff.Swisspower");
	}

	/**
	 * Test method for creating a {@link RabotCharge}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final RabotCharge rabotCharge(AppManagerTestBundle t) {
		return app(t, RabotCharge::new, "App.TimeOfUseTariff.RabotCharge");
	}

	/**
	 * Test method for creating a {@link StromdaoCorrently}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final StromdaoCorrently stromdaoCorrently(AppManagerTestBundle t) {
		return app(t, StromdaoCorrently::new, "App.TimeOfUseTariff.Stromdao");
	}

	/**
	 * Test method for creating a {@link Tibber}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Tibber tibber(AppManagerTestBundle t) {
		return app(t, Tibber::new, "App.TimeOfUseTariff.Tibber");
	}

	/**
	 * Test method for creating a {@link BeagleBoneBlack}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final BeagleBoneBlack beagleBoneBlack(AppManagerTestBundle t) {
		return app(t, BeagleBoneBlack::new, "App.OpenemsHardware.BeagleBoneBlack");
	}

	/**
	 * Test method for creating a {@link Compulab}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Compulab compulab(AppManagerTestBundle t) {
		return app(t, Compulab::new, "App.OpenemsHardware.Compulab");
	}

	/**
	 * Test method for creating a {@link TechbaseCm3}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TechbaseCm3 techbaseCm3(AppManagerTestBundle t) {
		return app(t, TechbaseCm3::new, "App.OpenemsHardware.CM3");
	}

	/**
	 * Test method for creating a {@link TechbaseCm4}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TechbaseCm4 techbaseCm4(AppManagerTestBundle t) {
		return app(t, TechbaseCm4::new, "App.OpenemsHardware.CM4");
	}

	/**
	 * Test method for creating a {@link TechbaseCm4Max}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TechbaseCm4Max techbaseCm4Max(AppManagerTestBundle t) {
		return app(t, TechbaseCm4Max::new, "App.OpenemsHardware.CM4Max");
	}

	/**
	 * Test method for creating a {@link TechbaseCm4s}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TechbaseCm4s techbaseCm4s(AppManagerTestBundle t) {
		return app(t, TechbaseCm4s::new, "App.OpenemsHardware.CM4S");
	}

	/**
	 * Test method for creating a {@link TechbaseCm4sGen2}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TechbaseCm4sGen2 techbaseCm4sGen2(AppManagerTestBundle t) {
		return app(t, TechbaseCm4sGen2::new, "App.OpenemsHardware.CM4S.Gen2");
	}

	/**
	 * Test method for creating a {@link TestPermissions}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestPermissions testPermissions(AppManagerTestBundle t) {
		return app(t, TestPermissions::new, "App.Test.TestPermissions");
	}

	/**
	 * Test method for creating a {@link TestFilter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestFilter testFilter(AppManagerTestBundle t) {
		return app(t, TestFilter::new, "App.Test.TestFilter");
	}

	/**
	 * Test method for creating a {@link TestMapPropName}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestMapPropName testMapPropName(AppManagerTestBundle t) {
		return app(t, TestMapPropName::new, "App.Test.TestMapPropName");
	}

	/**
	 * Test method for creating a {@link TestComponentDefConfig}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestComponentDefConfig testComponentDefConfig(AppManagerTestBundle t) {
		return app(t, TestComponentDefConfig::new, "App.Test.TestComponentDefConfig");
	}

	// Test

	/**
	 * Test method for creating a {@link TestADependencyToC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestADependencyToC testADependencyToC(AppManagerTestBundle t) {
		return app(t, TestADependencyToC::new, "App.Test.TestADependencyToC");
	}

	/**
	 * Test method for creating a {@link TestBDependencyToC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestBDependencyToC testBDependencyToC(AppManagerTestBundle t) {
		return app(t, TestBDependencyToC::new, "App.Test.TestBDependencyToC");
	}

	/**
	 * Test method for creating a {@link TestC}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestC testC(AppManagerTestBundle t) {
		return app(t, TestC::new, "App.Test.TestC");
	}

	/**
	 * Test method for creating a {@link TestMultipleIds}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TestMultipleIds testMultipleIds(AppManagerTestBundle t) {
		return app(t, TestMultipleIds::new, "App.Test.TestMultipleIds");
	}

	// Api

	/**
	 * Test method for creating a {@link AppEnerixControl}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppEnerixControl enerixControl(AppManagerTestBundle t) {
		return app(t, AppEnerixControl::new, "App.Cloud.EnerixControl");
	}

	/**
	 * Test method for creating a {@link AppCleverPv}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppCleverPv cleverPv(AppManagerTestBundle t) {
		return app(t, AppCleverPv::new, "App.Cloud.Clever-PV");
	}

	/**
	 * Test method for creating a {@link ModbusTcpApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusTcpApiReadOnly modbusTcpApiReadOnly(AppManagerTestBundle t) {
		return app(t, ModbusTcpApiReadOnly::new, "App.Api.ModbusTcp.ReadOnly");
	}

	/**
	 * Test method for creating a {@link ModbusTcpApiReadWrite}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusTcpApiReadWrite modbusTcpApiReadWrite(AppManagerTestBundle t) {
		return app(t, ModbusTcpApiReadWrite::new, "App.Api.ModbusTcp.ReadWrite");
	}

	/**
	 * Test method for creating a {@link MqttApi}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final MqttApi mqttApi(AppManagerTestBundle t) {
		return app(t, MqttApi::new, "App.Api.Mqtt");
	}

	/**
	 * Test method for creating a {@link ModbusRtuApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusRtuApiReadOnly modbusRtuApiReadOnly(AppManagerTestBundle t) {
		return app(t, ModbusRtuApiReadOnly::new, "App.Api.ModbusRtu.ReadOnly");
	}

	/**
	 * Test method for creating a {@link ModbusRtuApiReadWrite}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ModbusRtuApiReadWrite modbusRtuApiReadWrite(AppManagerTestBundle t) {
		return app(t, ModbusRtuApiReadWrite::new, "App.Api.ModbusRtu.ReadWrite");
	}

	/**
	 * Test method for creating a {@link RestJsonApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final RestJsonApiReadOnly restJsonApiReadOnly(AppManagerTestBundle t) {
		return app(t, RestJsonApiReadOnly::new, "App.Api.RestJson.ReadOnly");
	}

	/**
	 * Test method for creating a {@link RestJsonApiReadWrite}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final RestJsonApiReadWrite restJsonApiReadWrite(AppManagerTestBundle t) {
		return app(t, RestJsonApiReadWrite::new, "App.Api.RestJson.ReadWrite");
	}

	// Evcs

	/**
	 * Test method for creating a {@link DezonyEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final DezonyEvcs dezony(AppManagerTestBundle t) {
		return app(t, DezonyEvcs::new, "App.Evcs.Dezony");
	}

	/**
	 * Test method for creating a {@link HardyBarthEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HardyBarthEvcs hardyBarthEvcs(AppManagerTestBundle t) {
		return app(t, HardyBarthEvcs::new, "App.Evcs.HardyBarth");
	}

	/**
	 * Test method for creating a {@link AppHardyBarthReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppHardyBarthReadOnly hardyBarthEvcsReadOnly(AppManagerTestBundle t) {
		return app(t, AppHardyBarthReadOnly::new, "App.Evcs.HardyBarth.ReadOnly");
	}

	/**
	 * Test method for creating a {@link KebaEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KebaEvcs kebaEvcs(AppManagerTestBundle t) {
		return app(t, KebaEvcs::new, "App.Evcs.Keba");
	}

	/**
	 * Test method for creating a {@link KebaEvcsReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KebaEvcsReadOnly kebaEvcsReadonly(AppManagerTestBundle t) {
		return app(t, KebaEvcsReadOnly::new, "App.Evcs.Keba.ReadOnly");
	}

	/**
	 * Test method for creating a {@link AppGoeEvcsReadOnly}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppGoeEvcsReadOnly goeEvcs(AppManagerTestBundle t) {
		return app(t, AppGoeEvcsReadOnly::new, "App.Evcs.Goe.ReadOnly");
	}

	/**
	 * Test method for creating a {@link MennekesEvcsReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final MennekesEvcsReadOnly mennekesEvcsReadOnlyEvcs(AppManagerTestBundle t) {
		return app(t, MennekesEvcsReadOnly::new, "App.Evcs.Mennekes.ReadOnly");
	}

	/**
	 * Test method for creating a {@link HeidelbergEvcsReadOnly}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeidelbergEvcsReadOnly heidelbergEvcsReadOnlyEvcs(AppManagerTestBundle t) {
		return app(t, HeidelbergEvcsReadOnly::new, "App.Evcs.Heidelberg.ReadOnly");
	}

	/**
	 * Test method for creating a {@link IesKeywattEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final IesKeywattEvcs iesKeywattEvcs(AppManagerTestBundle t) {
		return app(t, IesKeywattEvcs::new, "App.Evcs.IesKeywatt");
	}

	/**
	 * Test method for creating a {@link AppKebaEvse}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppKebaEvse kebaEvse(AppManagerTestBundle t) {
		return app(t, AppKebaEvse::new, "App.Evse.ChargePoint.Keba");
	}

	/**
	 * Test method for creating a {@link AppEvseCluster}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppEvseCluster clusterEvse(AppManagerTestBundle t) {
		return app(t, AppEvseCluster::new, "App.Evse.Controller.Cluster");
	}

	/**
	 * Test method for creating a {@link AppGenericVehicle}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AppGenericVehicle genericVehicle(AppManagerTestBundle t) {
		return app(t, AppGenericVehicle::new, "App.Evse.ElectricVehicle.Generic");
	}

	/**
	 * Test method for creating a {@link TimedataInfluxDb}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TimedataInfluxDb timedataInfluxDb(AppManagerTestBundle t) {
		return app(t, TimedataInfluxDb::new, "App.Timedata.InfluxDb");
	}

	/**
	 * Test method for creating a {@link AblEvcsReadOnly}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AblEvcsReadOnly ablEvcs(AppManagerTestBundle t) {
		return app(t, AblEvcsReadOnly::new, "App.Evcs.Abl.ReadOnly");
	}

	/**
	 * Test method for creating a {@link AlpitronicEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final AlpitronicEvcs alpitronicEvcs(AppManagerTestBundle t) {
		return app(t, AlpitronicEvcs::new, "App.Evcs.Alpitronic");
	}

	/**
	 * Test method for creating a {@link WebastoNextEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final WebastoNextEvcs webastoNext(AppManagerTestBundle t) {
		return app(t, WebastoNextEvcs::new, "App.Evcs.Webasto.Next");
	}

	/**
	 * Test method for creating a {@link WebastoUniteEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final WebastoUniteEvcs webastoUnite(AppManagerTestBundle t) {
		return app(t, WebastoUniteEvcs::new, "App.Evcs.Webasto.Unite");
	}

	/**
	 * Test method for creating a {@link EvcsCluster}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final EvcsCluster evcsCluster(AppManagerTestBundle t) {
		return app(t, EvcsCluster::new, "App.Evcs.Cluster");
	}

	// Hardware

	/**
	 * Test method for creating a {@link KMtronic8Channel}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KMtronic8Channel kmtronic8Channel(AppManagerTestBundle t) {
		return app(t, KMtronic8Channel::new, "App.Hardware.KMtronic8Channel");
	}

	/**
	 * Test method for creating a {@link IoGpio}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final IoGpio ioGpio(AppManagerTestBundle t) {
		return app(t, IoGpio::new, "App.Hardware.IoGpio");
	}

	// Heat

	/**
	 * Test method for creating a {@link HeatPump}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeatPump heatPump(AppManagerTestBundle t) {
		return app(t, HeatPump::new, "App.Heat.HeatPump");
	}

	/**
	 * Test method for creating a {@link PeakShaving}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final TimeSlotPeakShaving timeSlotPeakShaving(AppManagerTestBundle t) {
		return app(t, TimeSlotPeakShaving::new, "App.PeakShaving.TimeSlotPeakShaving");
	}

	/**
	 * Test method for creating a {@link CombinedHeatAndPower}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final CombinedHeatAndPower combinedHeatAndPower(AppManagerTestBundle t) {
		return app(t, CombinedHeatAndPower::new, "App.Heat.CHP");
	}

	/**
	 * Test method for creating a {@link HeatingElement}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeatingElement heatingElement(AppManagerTestBundle t) {
		return app(t, HeatingElement::new, "App.Heat.HeatingElement");
	}

	// PvSelfConsumption

	/**
	 * Test method for creating a {@link GridOptimizedCharge}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final GridOptimizedCharge gridOptimizedCharge(AppManagerTestBundle t) {
		return app(t, GridOptimizedCharge::new, "App.PvSelfConsumption.GridOptimizedCharge");
	}

	/**
	 * Test method for creating a {@link SelfConsumptionOptimization}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SelfConsumptionOptimization selfConsumptionOptimization(AppManagerTestBundle t) {
		return app(t, SelfConsumptionOptimization::new, "App.PvSelfConsumption.SelfConsumptionOptimization");
	}

	// Load-Control

	/**
	 * Test method for creating a {@link ManualRelayControl}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ManualRelayControl manualRelayControl(AppManagerTestBundle t) {
		return app(t, ManualRelayControl::new, "App.LoadControl.ManualRelayControl");
	}

	/**
	 * Test method for creating a {@link ThresholdControl}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final ThresholdControl thresholdControl(AppManagerTestBundle t) {
		return app(t, ThresholdControl::new, "App.LoadControl.ThresholdControl");
	}

	// Meter

	/**
	 * Test method for creating a {@link SocomecMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SocomecMeter socomecMeter(AppManagerTestBundle t) {
		return app(t, SocomecMeter::new, "App.Meter.Socomec");
	}

	/**
	 * Test method for creating a {@link KdkMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KdkMeter kdkMeter(AppManagerTestBundle t) {
		return app(t, KdkMeter::new, "App.Meter.Kdk");
	}

	/**
	 * Test method for creating a {@link DiscoveregyMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final DiscovergyMeter discovergyMeter(AppManagerTestBundle t) {
		return app(t, DiscovergyMeter::new, "App.Meter.Discovergy");
	}

	/**
	 * Test method for creating a {@link CarloGavazziMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final CarloGavazziMeter carloGavazziMeter(AppManagerTestBundle t) {
		return app(t, CarloGavazziMeter::new, "App.Meter.CarloGavazzi");
	}

	/**
	 * Test method for creating a {@link JanitzaMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final JanitzaMeter janitzaMeter(AppManagerTestBundle t) {
		return app(t, JanitzaMeter::new, "App.Meter.Janitza");
	}

	/**
	 * Test method for creating a {@link GridMeterJanitza}.
	 *
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final GridMeterJanitza janitzaGridMeter(AppManagerTestBundle t) {
		return app(t, GridMeterJanitza::new, "App.GridMeter.Janitza");
	}

	/**
	 * Test method for creating a {@link PqPlusMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PqPlusMeter pqPlusMeter(AppManagerTestBundle t) {
		return app(t, PqPlusMeter::new, "App.Meter.PqPlus");
	}

	/**
	 * Test method for creating a {@link EastronMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final EastronMeter eastronMeter(AppManagerTestBundle t) {
		return app(t, EastronMeter::new, "App.Meter.Eastron");
	}

	/**
	 * Test method for creating a {@link PhoenixContactMeter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PhoenixContactMeter phoenixContactMeter(AppManagerTestBundle t) {
		return app(t, PhoenixContactMeter::new, "App.Meter.PhoenixContact");
	}

	// PV-Inverter

	/**
	 * Test method for creating a {@link FroniusPvInverter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FroniusPvInverter froniusPvInverter(AppManagerTestBundle t) {
		return app(t, FroniusPvInverter::new, "App.PvInverter.Fronius");
	}

	/**
	 * Test method for creating a {@link KacoPvInverter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KacoPvInverter kacoPvInverter(AppManagerTestBundle t) {
		return app(t, KacoPvInverter::new, "App.PvInverter.Kaco");
	}

	/**
	 * Test method for creating a {@link KostalPvInverter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KostalPvInverter kostalPvInverter(AppManagerTestBundle t) {
		return app(t, KostalPvInverter::new, "App.PvInverter.Kostal");
	}

	/**
	 * Test method for creating a {@link SmaPvInverter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SmaPvInverter smaPvInverter(AppManagerTestBundle t) {
		return app(t, SmaPvInverter::new, "App.PvInverter.Sma");
	}

	/**
	 * Test method for creating a {@link SolarEdgePvInverter}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final SolarEdgePvInverter solarEdgePvInverter(AppManagerTestBundle t) {
		return app(t, SolarEdgePvInverter::new, "App.PvInverter.SolarEdge");
	}

	// PeakShaving

	/**
	 * Test method for creating a {@link PeakShaving}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PeakShaving peakShaving(AppManagerTestBundle t) {
		return app(t, PeakShaving::new, "App.PeakShaving.PeakShaving");
	}

	/**
	 * Test method for creating a {@link PhaseAccuratePeakShaving}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PhaseAccuratePeakShaving phaseAccuratePeakShaving(AppManagerTestBundle t) {
		return app(t, PhaseAccuratePeakShaving::new, "App.PeakShaving.PhaseAccuratePeakShaving");
	}

	// ess-controller

	/**
	 * Test method for creating a {@link FixActivePower}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FixActivePower fixActivePower(AppManagerTestBundle t) {
		return app(t, FixActivePower::new, "App.Ess.FixActivePower");
	}

	/**
	 * Test method for creating a {@link FixStateOfCharge}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FixStateOfCharge fixStateOfCharge(AppManagerTestBundle t) {
		return app(t, FixStateOfCharge::new, "App.Ess.FixStateOfCharge");
	}

	/**
	 * Test method for creating a {@link PrepareBatteryExtension}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PrepareBatteryExtension prepareBatteryExtension(AppManagerTestBundle t) {
		return app(t, PrepareBatteryExtension::new, "App.Ess.PrepareBatteryExtension");
	}

	/**
	 * Test method for creating a {@link PowerPlantController}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PowerPlantController powerPlantController(AppManagerTestBundle t) {
		return app(t, PowerPlantController::new, "App.Ess.PowerPlantController");
	}

	/**
	 * Test method for creating a {@link Limiter14a}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final Limiter14a limiter14a(AppManagerTestBundle t) {
		return app(t, Limiter14a::new, "App.Ess.Limiter14a");
	}

	/**
	 * Test method for creating a {@link HeatMyPvReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeatMyPvReadOnly heatMyPvReadOnly(AppManagerTestBundle t) {
		return app(t, HeatMyPvReadOnly::new, "App.Heat.MyPv.ReadOnly");
	}

	/**
	 * Test method for creating a {@link HeatMyPvReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HeatAskomaReadOnly heatAskoma(AppManagerTestBundle t) {
		return app(t, HeatAskomaReadOnly::new, "App.Heat.Askoma.ReadOnly");
	}

	/**
	 * Gets the minimum configuration of an app for easily creating instances in
	 * tests.
	 * 
	 * @param appId the id of the {@link OpenemsApp}
	 * @return the configuration to create an instance
	 */
	public static JsonObject getMinConfig(String appId) {
		return switch (appId) {
		case "App.FENECON.Home" -> TestFeneconHome10.minSettings();
		case "App.FENECON.Home.20" -> TestFeneconHome20.minSettings();
		case "App.FENECON.Home.30" -> TestFeneconHome30.minSettings();
		case "App.FENECON.Home6" -> TestFeneconHome10Gen2.minSettings();
		case "App.FENECON.Home10.Gen2" -> TestFeneconHome10Gen2.minSettings();
		case "App.FENECON.Home15" -> TestFeneconHome10Gen2.minSettings();
		case "App.Hardware.IoGpio" -> JsonUtils.buildJsonObject() //
				.addProperty("HARDWARE_TYPE", GpioHardwareType.MODBERRY_X500_M40804_WB) //
				.build();
		default -> new JsonObject();
		};
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructor<T> constructor, String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil);
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructorWithAppUtilAndHost<T> constructor,
			String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil, t.appManagerUtil, t.host);
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructorWithAppUtil<T> constructor,
			String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil, t.appManagerUtil);
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructorWithHostAndMeta<T> constructor,
			String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil, t.host, t.meta);
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructorWithHost<T> constructor, String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil, t.host);
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructorWithMeta<T> constructor, String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil, t.meta);
	}

	private static interface DefaultAppConstructor<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil);

	}

	private static interface DefaultAppConstructorWithAppUtil<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil, AppManagerUtil util);

	}

	private static interface DefaultAppConstructorWithAppUtilAndHost<A> {
		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil, AppManagerUtil util, Host host);
	}

	private static interface DefaultAppConstructorWithMeta<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil, Meta meta);

	}

	private static interface DefaultAppConstructorWithHost<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil, Host host);

	}

	private static interface DefaultAppConstructorWithHostAndMeta<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil, Host host, Meta meta);

	}

}
