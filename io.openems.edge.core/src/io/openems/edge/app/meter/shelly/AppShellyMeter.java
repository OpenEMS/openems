package io.openems.edge.app.meter.shelly;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

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
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.app.meter.MeterProps;
import io.openems.edge.app.meter.shelly.jsonrpc.GetOptions;
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
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.Meter.Shelly")
public class AppShellyMeter
		extends AbstractOpenemsAppWithProps<AppShellyMeter, AppShellyMeter.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppShellyMeter, Type.Parameter.BundleParameter> {
		METER_ID(AppDef.componentId("io0")), //

		ALIAS(alias()), //
		DISCOVERY_TYPE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("communication.discoveryType.label") //
				.setDefaultValue(DiscoveryType.MDNS) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(DiscoveryType.class), l);
				}))), //

		DEVICE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".device.label") //
				.setField(JsonFormlyUtil::buildLazySelect, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(DISCOVERY_TYPE).equal(Exp.staticValue(DiscoveryType.MDNS)));
					field.setRequestParams(ShellyDiscovery.ID, GetOptions.METHOD);
					field.setLoadingText(translate(parameter.bundle(), "App.Meter.Shelly.device.search"));
					field.setRetryLoadingText(translate(parameter.bundle(), "App.Meter.Shelly.device.retrySearch"));
					field.setMissingOptionsText(
							translate(parameter.bundle(), "App.Meter.Shelly.device.noDevicesFound"));
				}))), //
		IP(MeterProps.ip().wrapField((app, property, l, parameter, field) -> {
			field.onlyShowIf(Exp.currentModelValue(DISCOVERY_TYPE).equal(Exp.staticValue(DiscoveryType.STATIC)));
		})), //
		HARDWARE_TYPE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".hardwareType") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(
							Exp.currentModelValue(DISCOVERY_TYPE).equal(Exp.staticValue(DiscoveryType.STATIC)));
					field.setOptions(OptionsFactory.of(ShellyType.class), l);
				}))),
		PHASE(MeterProps.singlePhase()), //
		TYPE(MeterProps.type(MeterType.GRID) //
				.setDefaultValue(MeterType.CONSUMPTION_METERED)), //
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
				final var mdns = this.getObject(p, l, Property.DEVICE, MdnsValue.serializer());
				factoryId = mdns.type().getFactoryId();
				props.addProperty("mdnsName", mdns.name());
			}
			case STATIC -> {
				final var ip = this.getString(p, Property.IP);
				final var hardwareType = this.getEnum(p, ShellyType.class, Property.HARDWARE_TYPE);

				factoryId = hardwareType.getFactoryId();
				props.addProperty("ip", ip);
			}
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
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanInstall(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
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

}
