package io.openems.edge.app.meter.shelly.meter;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.app.meter.MeterProps;
import io.openems.edge.app.meter.shelly.ShellyProps;
import io.openems.edge.app.meter.shelly.discovery.DiscoveryType;
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
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;

@Component(name = "App.Meter.Shelly.Meter")
public class AppShellyMeter
		extends AbstractOpenemsAppWithProps<AppShellyMeter, AppShellyMeter.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppShellyMeter, Type.Parameter.BundleParameter> {
		METER_ID(AppDef.componentId("meter0")), //
		METER_IO_ID(AppDef.componentId("io0")), //

		ALIAS(alias()), //
		DISCOVERY_TYPE(ShellyProps.discoveryType()), //

		DEVICE(ShellyProps.mdnsDevice(DISCOVERY_TYPE, ShellyDiscoveryMeter.ID)), //

		IP(MeterProps.ip().wrapField((app, property, l, parameter, field) -> {
			field.onlyShowIf(Exp.currentModelValue(DISCOVERY_TYPE).equal(Exp.staticValue(DiscoveryType.STATIC)));
		})), //
		HARDWARE_TYPE(ShellyProps.hardwareType(DISCOVERY_TYPE, ShellyTypeMeter.class)), //
		TYPE(MeterProps.type(MeterType.GRID)//
				.setDefaultValue(MeterType.CONSUMPTION_METERED) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(
							ShellyProps.buildShowConditionForShellyTypesWithNoTerminals(DEVICE, HARDWARE_TYPE));
				})), //

		PHASE(MeterProps.singlePhase().wrapField((app, property, l, parameter, field) -> {
			field.onlyShowIf(ShellyProps.buildShowConditionForShellyTypes(
					List.of(ShellyTypeMeter.PRO_1PM, ShellyTypeMeter.PRO_2PM, ShellyTypeMeter.PRO_4PM), DEVICE,
					HARDWARE_TYPE));
		})), //

		RELAY_1_ID(AppDef.componentId("io0")), //
		RELAY_1_ALIAS(ShellyProps.aliasForTerminal(1, DEVICE, HARDWARE_TYPE)), //
		RELAY_1_TYPE(ShellyProps.typeForTerminal(1, DEVICE, HARDWARE_TYPE)), //

		RELAY_2_ID(AppDef.componentId("io0")), //
		RELAY_2_ALIAS(ShellyProps.aliasForTerminal(2, DEVICE, HARDWARE_TYPE)), //
		RELAY_2_TYPE(ShellyProps.typeForTerminal(2, DEVICE, HARDWARE_TYPE)), //

		RELAY_3_ID(AppDef.componentId("io0")), //
		RELAY_3_ALIAS(ShellyProps.aliasForTerminal(3, DEVICE, HARDWARE_TYPE)), //
		RELAY_3_TYPE(ShellyProps.typeForTerminal(3, DEVICE, HARDWARE_TYPE)), //

		RELAY_4_ID(AppDef.componentId("io0")), //
		RELAY_4_ALIAS(ShellyProps.aliasForTerminal(4, DEVICE, HARDWARE_TYPE)), //
		RELAY_4_TYPE(ShellyProps.typeForTerminal(4, DEVICE, HARDWARE_TYPE)), //

		;

		private final AppDef<? super AppShellyMeter, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super AppShellyMeter, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super AppShellyMeter, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppShellyMeter>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, AppShellyMeter, Parameter.BundleParameter> self() {
			return this;
		}
	}

	@Activate
	public AppShellyMeter(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected AppShellyMeter getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getString(p, Property.ALIAS);
			final var discoveryType = this.getEnum(p, DiscoveryType.class, Property.DISCOVERY_TYPE);

			final var props = JsonUtils.buildJsonObject() //
					.addProperty("enabled", true);

			ShellyTypeMeter shellyType = null;
			switch (discoveryType) {
			case MDNS -> {
				final var mdns = this.getObject(p, l, Property.DEVICE, MdnsValueMeter.serializer());
				shellyType = mdns.type();
				props.addProperty("mdnsName", mdns.name());
				props.addProperty("ip", "");
			}
			case STATIC -> {
				final var ip = this.getString(p, Property.IP);
				shellyType = this.getEnum(p, ShellyTypeMeter.class, Property.HARDWARE_TYPE);

				props.addProperty("ip", ip);
				props.addProperty("mdnsName", "");
			}
			}

			final var components = new ArrayList<EdgeConfig.Component>();
			switch (shellyType) {
			case PRO_3EM -> {
				var type = this.getEnum(p, MeterType.class, Property.TYPE);
				props.addProperty("type", type);
				props.addProperty("invert", type == MeterType.PRODUCTION);

				if (t == ConfigurationTarget.ADD) {
					props.addProperty("validateDevice", true);
				}

				final var meterId = this.getId(t, p, Property.METER_ID);
				components.add(new EdgeConfig.Component(meterId, alias, shellyType.getFactoryId(), props.build()));
			}
			case GEN3_3EM -> {
				var type = this.getEnum(p, MeterType.class, Property.TYPE);
				props.addProperty("type", type);
				props.addProperty("invert", type == MeterType.PRODUCTION);

				final var meterId = this.getId(t, p, Property.METER_ID);
				components.add(new EdgeConfig.Component(meterId, alias, shellyType.getFactoryId(), props.build()));
			}
			case PRO_1PM -> {
				var type = this.getEnum(p, MeterType.class, Property.TYPE);
				props.addProperty("type", type);
				props.addProperty("invert", type == MeterType.PRODUCTION);
				props.addProperty("phase", this.getEnum(p, Phase.class, Property.PHASE));

				final var meterId = this.getId(t, p, Property.METER_IO_ID);
				components.add(new EdgeConfig.Component(meterId, alias, shellyType.getFactoryId(), props.build()));
			}
			case PRO_2PM -> {
				props.addProperty("phase", this.getEnum(p, Phase.class, Property.PHASE));

				final var meterId = this.getId(t, p, Property.METER_IO_ID);
				components.add(new EdgeConfig.Component(meterId, alias, shellyType.getFactoryId(), props.build()));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_1_ID), //
						this.getString(p, Property.RELAY_1_ALIAS), //
						"RELAY_1", //
						this.getEnum(p, MeterType.class, Property.RELAY_1_TYPE), //
						meterId, //
						"IO.Shelly.Pro2PM.Terminal"));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_2_ID), //
						this.getString(p, Property.RELAY_2_ALIAS), //
						"RELAY_2", //
						this.getEnum(p, MeterType.class, Property.RELAY_2_TYPE), //
						meterId, //
						"IO.Shelly.Pro2PM.Terminal"));
			}
			case PRO_4PM -> {
				props.addProperty("phase", this.getEnum(p, Phase.class, Property.PHASE));

				final var meterId = this.getId(t, p, Property.METER_IO_ID);
				components.add(new EdgeConfig.Component(meterId, alias, shellyType.getFactoryId(), props.build()));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_1_ID), //
						this.getString(p, Property.RELAY_1_ALIAS), //
						"RELAY_1", //
						this.getEnum(p, MeterType.class, Property.RELAY_1_TYPE), //
						meterId, //
						"IO.Shelly.Pro4PM.Terminal"));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_2_ID), //
						this.getString(p, Property.RELAY_2_ALIAS), //
						"RELAY_2", //
						this.getEnum(p, MeterType.class, Property.RELAY_2_TYPE), //
						meterId, //
						"IO.Shelly.Pro4PM.Terminal"));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_3_ID), //
						this.getString(p, Property.RELAY_3_ALIAS), //
						"RELAY_3", //
						this.getEnum(p, MeterType.class, Property.RELAY_3_TYPE), //
						meterId, //
						"IO.Shelly.Pro4PM.Terminal"));
				components.add(this.buildTerminalComponent(//
						this.getId(t, p, Property.RELAY_4_ID), //
						this.getString(p, Property.RELAY_4_ALIAS), //
						"RELAY_4", //
						this.getEnum(p, MeterType.class, Property.RELAY_4_TYPE), //
						meterId, //
						"IO.Shelly.Pro4PM.Terminal"));
			}
			}

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	private EdgeConfig.Component buildTerminalComponent(String componentId, String alias, String terminal,
			MeterType meterType, String deviceId, String factoryId) {
		var props = JsonUtils.buildJsonObject();
		props.addProperty("enabled", true);
		props.addProperty("terminal", terminal);
		props.addProperty("type", meterType);
		props.addProperty("invert", meterType == MeterType.PRODUCTION);
		props.addProperty("device.id", deviceId);

		return new EdgeConfig.Component(componentId, alias, factoryId, props.build());
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem, Language language) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanInstall(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.setCanSee(Role.ADMIN) //
				.build();
	}

}
