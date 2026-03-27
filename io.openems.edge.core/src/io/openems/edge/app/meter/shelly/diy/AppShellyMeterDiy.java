package io.openems.edge.app.meter.shelly.diy;

import static io.openems.edge.app.common.props.CommonProps.alias;

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
import io.openems.common.session.Language;
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
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;

@Component(name = "App.Meter.Shelly")
public class AppShellyMeterDiy extends
		AbstractOpenemsAppWithProps<AppShellyMeterDiy, AppShellyMeterDiy.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppShellyMeterDiy, Type.Parameter.BundleParameter> {
		METER_ID(AppDef.componentId("io0")), //

		ALIAS(alias()), //
		DISCOVERY_TYPE(ShellyProps.discoveryType()), //

		DEVICE(ShellyProps.mdnsDevice(DISCOVERY_TYPE, ShellyDiscoveryDiy.ID)), //
		HARDWARE_TYPE(ShellyProps.hardwareType(DISCOVERY_TYPE, ShellyTypeDiy.class)), //

		IP(MeterProps.ip().wrapField((app, property, l, parameter, field) -> {
			field.onlyShowIf(Exp.currentModelValue(DISCOVERY_TYPE).equal(Exp.staticValue(DiscoveryType.STATIC)));
		})), //
		PHASE(MeterProps.singlePhase()), //
		TYPE(MeterProps.type(MeterType.GRID)//
				.setDefaultValue(MeterType.CONSUMPTION_METERED)), //
		;

		private final AppDef<? super AppShellyMeterDiy, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super AppShellyMeterDiy, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super AppShellyMeterDiy, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppShellyMeterDiy>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, AppShellyMeterDiy, Parameter.BundleParameter> self() {
			return this;
		}
	}

	@Activate
	public AppShellyMeterDiy(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected AppShellyMeterDiy getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var meterId = this.getId(t, p, Property.METER_ID);

			final var alias = this.getString(p, Property.ALIAS);
			final var discoveryType = this.getEnum(p, DiscoveryType.class, Property.DISCOVERY_TYPE);
			final var phase = this.getEnum(p, Phase.class, Property.PHASE);
			final var type = this.getEnum(p, MeterType.class, Property.TYPE);
			final var invert = type == MeterType.PRODUCTION;

			final var props = JsonUtils.buildJsonObject() //
					.addProperty("enabled", true) //
					.addProperty("phase", phase) //
					.addProperty("type", type) //
					.addProperty("invert", invert);

			String factoryId = null;
			switch (discoveryType) {
			case MDNS -> {
				final var mdns = this.getObject(p, l, Property.DEVICE, MdnsValueDiy.serializer());
				factoryId = mdns.type().getFactoryId();
				props.addProperty("mdnsName", mdns.name());
				props.addProperty("ip", "");
			}
			case STATIC -> {
				final var ip = this.getString(p, Property.IP);
				final var hardwareType = this.getEnum(p, ShellyTypeDiy.class, Property.HARDWARE_TYPE);

				factoryId = hardwareType.getFactoryId();
				props.addProperty("ip", ip);
				props.addProperty("mdnsName", "");
			}
			}

			if (t == ConfigurationTarget.ADD) {
				props.addProperty("validateDevice", true);
			}

			final var components = List.of(//
					new EdgeConfig.Component(meterId, alias, factoryId, props.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

}
