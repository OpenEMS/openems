package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.alias;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.app.hardware.IoGpio;
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92ClusterSlave.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

@Component(name = "App.FENECON.Commercial.92.ClusterSlave")
public class FeneconCommercial92ClusterSlave
		extends AbstractOpenemsAppWithProps<FeneconCommercial92ClusterSlave, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, FeneconCommercial92ClusterSlave, Parameter.BundleParameter> {
		ALIAS(alias()), //

		BATTERY_TARGET(FeneconCommercialProps.batteryStartStopTarget()), //
		;

		private final AppDef<? super FeneconCommercial92ClusterSlave, ? super Property, ? super BundleParameter> def;

		private Property(
				AppDef<? super FeneconCommercial92ClusterSlave, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, FeneconCommercial92ClusterSlave, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconCommercial92ClusterSlave, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconCommercial92ClusterSlave>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconCommercial92ClusterSlave(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
	protected FeneconCommercial92ClusterSlave getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var batteryId = "battery0";
			final var batteryInverterId = "batteryInverter0";
			final var modbusToBatteryId = "modbus0";
			final var modbusToBatteryInverterId = "modbus1";
			final var essId = "ess0";

			final var batteryTarget = this.getString(p, Property.BATTERY_TARGET);

			final var deviceHardware = this.appManagerUtil
					.getFirstInstantiatedAppByCategories(OpenemsAppCategory.OPENEMS_DEVICE_HARDWARE);

			final var components = Lists.<EdgeConfig.Component>newArrayList(//
					FeneconHomeComponents.battery(bundle, batteryId, modbusToBatteryId, batteryTarget,
							getIoId(this.appManagerUtil, deviceHardware) + "/DigitalOutput4"), //
					FeneconCommercialComponents.batteryInverter(bundle, batteryInverterId, modbusToBatteryInverterId), //
					FeneconHomeComponents.ess(bundle, essId, batteryId, batteryInverterId), //
					FeneconHomeComponents.modbusInternal(bundle, t, modbusToBatteryId), //
					FeneconCommercialComponents.modbusToBatteryInverter(bundle, t, modbusToBatteryInverterId) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.staticIp(//
							new InterfaceConfiguration("eth1") //
									.addIp("BatteryInverter", "172.16.0.99/24"))) //
					// Gets the following ip with preconfigured router
					// new InterfaceConfiguration("eth0") //
					// .addIp("Communication to master", "10.5.0." + (10 + position) + "/24")))
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanDelete(Role.INSTALLER) //
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	private static String getIoId(//
			AppManagerUtil appManagerUtil, //
			OpenemsAppInstance deviceHardware //
	) throws OpenemsNamedException {
		if (deviceHardware == null) {
			throw new OpenemsException("Hardware 'null' not supported Commercial Slave.");
		}

		for (var dependency : deviceHardware.dependencies) {
			if (!"IO_GPIO".equals(dependency.key)) {
				continue;
			}
			final var instance = appManagerUtil.findInstanceByIdOrError(dependency.instanceId);
			return instance.properties.get(IoGpio.Property.IO_ID.name()).getAsString();
		}
		throw new OpenemsException("Unable to get io dependency for hardware '" + deviceHardware.appId + "'.");
	}

}
