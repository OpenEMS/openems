package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.feedInType;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.maxFeedInPower;
import static io.openems.edge.app.integratedsystem.IntegratedSystemProps.safetyCountry;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.app.enums.FeedInType;
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
import io.openems.edge.app.integratedsystem.fenecon.commercial.FeneconCommercial92.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.InterfaceConfiguration;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

@Component(name = "App.FENECON.Commercial.92")
public class FeneconCommercial92 extends
		AbstractOpenemsAppWithProps<FeneconCommercial92, Property, Parameter.BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, FeneconCommercial92, Parameter.BundleParameter> {
		ALIAS(alias()), //

		SAFETY_COUNTRY(AppDef.copyOfGeneric(safetyCountry(), def -> def //
				.setRequired(true))), //

		FEED_IN_TYPE(feedInType(FeedInType.EXTERNAL_LIMITATION)), //
		MAX_FEED_IN_POWER(maxFeedInPower(FEED_IN_TYPE)), //
		;

		private final AppDef<? super FeneconCommercial92, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super FeneconCommercial92, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, FeneconCommercial92, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super FeneconCommercial92, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconCommercial92>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public FeneconCommercial92(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
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
	protected FeneconCommercial92 getApp() {
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
			final var modbusToGridMeterId = "modbus2";
			final var modbusToExternalDevicesId = "modbus3";
			final var gridMeterId = "meter0";
			final var essId = "ess0";

			final var feedInType = this.getEnum(p, FeedInType.class, Property.FEED_IN_TYPE);
			final var maxFeedInPower = feedInType == FeedInType.DYNAMIC_LIMITATION
					? this.getInt(p, Property.MAX_FEED_IN_POWER)
					: 0;

			final var components = Lists.newArrayList(//
					FeneconHomeComponents.battery(bundle, batteryId, modbusToBatteryId), //
					FeneconCommercialComponents.batteryInverter(bundle, batteryInverterId, modbusToBatteryInverterId), //
					FeneconHomeComponents.ess(bundle, essId, batteryId, batteryInverterId), //
					FeneconHomeComponents.io(bundle, modbusToBatteryId), //
					FeneconHomeComponents.modbusInternal(bundle, t, modbusToBatteryId), //
					FeneconHomeComponents.predictor(bundle, t), //
					FeneconCommercialComponents.modbusToBatteryInverter(bundle, t, modbusToBatteryInverterId), //
					FeneconCommercialComponents.modbusToGridMeter(bundle, t, modbusToGridMeterId), //
					FeneconHomeComponents.modbusForExternalMeters(bundle, t, modbusToExternalDevicesId) //
			);

			final var dependencies = Lists.newArrayList(//
					FeneconHomeComponents.selfConsumptionOptimization(t, essId, gridMeterId), //
					FeneconHomeComponents.gridOptimizedCharge(t, feedInType, maxFeedInPower), //
					FeneconHomeComponents.prepareBatteryExtension(), //
					FeneconCommercialComponents.gridMeter(bundle, gridMeterId, modbusToGridMeterId) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.staticIp(new InterfaceConfiguration("eth1") //
							.addIp("BatteryInverter", "172.16.0.99/24")))
					.addDependencies(dependencies) //
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

}
