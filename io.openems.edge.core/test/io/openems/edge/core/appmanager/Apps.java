package io.openems.edge.core.appmanager;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.app.TestADependencyToC;
import io.openems.edge.app.TestBDependencyToC;
import io.openems.edge.app.TestC;
import io.openems.edge.app.TestMultipleIds;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.ModbusTcpApiReadWrite;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.ess.PrepareBatteryExtension;
import io.openems.edge.app.evcs.HardyBarthEvcs;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.heat.HeatPump;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.app.timeofusetariff.Tibber;
import io.openems.edge.common.component.ComponentManager;

public class Apps {

	private Apps() {
		super();
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
	 * Test method for creating a {@link FeneconHome}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final FeneconHome feneconHome(AppManagerTestBundle t) {
		return app(t, FeneconHome::new, "App.FENECON.Home");
	}

	// Evcs

	/**
	 * Test method for creating a {@link KebaEvcs}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final KebaEvcs kebaEvcs(AppManagerTestBundle t) {
		return app(t, KebaEvcs::new, "App.Evcs.Keba");
	}

	// TimeOfUseTariff

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
	 * Test method for creating a {@link RestJsonApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final RestJsonApiReadOnly restJsonApiReadOnly(AppManagerTestBundle t) {
		return app(t, RestJsonApiReadOnly::new, "App.Api.RestJson.ReadOnly");
	}

	// Evcs

	/**
	 * Test method for creating a {@link RestJsonApiReadOnly}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final HardyBarthEvcs hardyBarthEvcs(AppManagerTestBundle t) {
		return app(t, HardyBarthEvcs::new, "App.Evcs.HardyBarth");
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

	// ess-controller

	/**
	 * Test method for creating a {@link PrepareBatteryExtension}.
	 * 
	 * @param t the {@link AppManagerTestBundle}
	 * @return the {@link OpenemsApp} instance
	 */
	public static final PrepareBatteryExtension prepareBatteryExtension(AppManagerTestBundle t) {
		return app(t, PrepareBatteryExtension::new, "App.Ess.PrepareBatteryExtension");
	}

	private static final <T> T app(AppManagerTestBundle t, DefaultAppConstructor<T> constructor, String appId) {
		return constructor.create(t.componentManger, AppManagerTestBundle.getComponentContext(appId), t.cm,
				t.componentUtil);
	}

	private static interface DefaultAppConstructor<A> {

		public A create(ComponentManager componentManager, ComponentContext componentContext, ConfigurationAdmin cm,
				ComponentUtil componentUtil);

	}

}
