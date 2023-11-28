package io.openems.edge.app.integratedsystem.fenecon.industrial.s;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.FeneconHomeComponents.selfConsumptionOptimization;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.batteryBmw;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.batteryInverter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.batteryf2bClusterParallel;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.batteryf2bClusterSerial;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.cycle;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.essGenericManagedSymmetric;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.gridMeter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.io;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.modbusInternal;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.modbusToBatteryInverter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.modbusToGridMeter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.power;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSComponents.system;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSProps.hasGridMeter;
import static io.openems.edge.app.integratedsystem.fenecon.industrial.s.FeneconIndustrialSProps.hasSelfConsumptionOptimization;
import static java.util.List.of;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.app.integratedsystem.fenecon.industrial.s.Isk011.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Tasks;

@Component(name = "App.FENECON.Industrial.S.ISK011")
public class Isk011 extends AbstractOpenemsAppWithProps<Isk011, Property, BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, Isk011, BundleParameter> {
		ALIAS(alias()), //
		HAS_GRID_METER(hasGridMeter()), //
		HAS_SELF_CONSUMPTION_OPTIMIZATION(hasSelfConsumptionOptimization(HAS_GRID_METER)), //
		;

		private final AppDef<? super Isk011, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super Isk011, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, Isk011, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super Isk011, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<Isk011>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public Isk011(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected Isk011 getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var essId = "ess0";
			final var modbusIdInternal = "modbus0";
			final var modbusIdToGridMeter = "modbus2";
			final var batteryInverterId = "batteryInverter1";
			final var batteryParallelId = "battery1";
			final var batterySeriallId1 = "battery10";
			final var batterySeriallId2 = "battery20";

			final var battery11Id = "battery11";
			final var battery12Id = "battery12";
			final var battery21Id = "battery21";
			final var battery22Id = "battery22";

			final var gridMeterId = "meter0";

			final var components = Lists.newArrayList(//
					// Core-Components
					power(), cycle(), //
					// Batteries
					batteryf2bClusterParallel(bundle, batteryParallelId, "1", of(batterySeriallId1, batterySeriallId2)), //
					batteryf2bClusterSerial(bundle, batterySeriallId1, "10", of(battery11Id, battery12Id)), //
					batteryBmw(bundle, battery11Id, "11", modbusIdInternal, 1), //
					batteryBmw(bundle, battery12Id, "12", modbusIdInternal, 2), //
					batteryf2bClusterSerial(bundle, batterySeriallId2, "20", of(battery21Id, battery22Id)), //
					batteryBmw(bundle, battery21Id, "21", modbusIdInternal, 3), //
					batteryBmw(bundle, battery22Id, "22", modbusIdInternal, 4), //
					// Battery-Inverter
					batteryInverter(bundle, 1), //
					// ESS
					essGenericManagedSymmetric(bundle, essId, batteryParallelId, batteryInverterId), //
					// Bridge
					modbusInternal(bundle, t, modbusIdInternal), //
					modbusToBatteryInverter(bundle, t, 1), //
					// IO
					io(bundle), //
					// Misc.
					system(t, "System Industrial S (ISK011)", of(battery11Id, battery12Id, battery21Id, battery22Id)) //
			);

			final var dependencies = new ArrayList<DependencyDeclaration>();
			if (this.getBoolean(p, Property.HAS_GRID_METER)) {
				components.add(modbusToGridMeter(bundle, t, modbusIdToGridMeter));
				dependencies.add(gridMeter(bundle, modbusIdToGridMeter, gridMeterId));
				if (this.getBoolean(p, Property.HAS_SELF_CONSUMPTION_OPTIMIZATION)) {
					dependencies.add(selfConsumptionOptimization(t, essId, gridMeterId));
				}
			}

			// TODO
			// final var ips = Lists.newArrayList(//
			// new InterfaceConfiguration("eth0") //
			// .setDhcpRouteMetric(216), //
			// new InterfaceConfiguration("eth1") //
			// .setRouteGateway("172.23.22.2") //
			// .setRouteMetric(512) //
			// );

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addDependencies(dependencies) //
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
