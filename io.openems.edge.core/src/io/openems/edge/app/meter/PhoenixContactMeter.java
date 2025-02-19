package io.openems.edge.app.meter;

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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.meter.PhoenixContactMeter.Property;
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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a App for a PhoenixContact meter.
 */
@Component(name = "App.Meter.PhoenixContact")
public class PhoenixContactMeter extends
		AbstractOpenemsAppWithProps<PhoenixContactMeter, Property, Parameter.BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, PhoenixContactMeter, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter1")), //
		MODBUS_ID(AppDef.componentId("modbus2")), //
		// Properties
		ALIAS(CommonProps.alias()), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type(MeterType.GRID), def -> def //
				.setRequired(true))), //
		IP(MeterProps.ip() //
				.setRequired(true)), //
		PORT(MeterProps.port() //
				.setRequired(true)), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), def -> def //
				.setRequired(true) //
				.setDefaultValue(1))), //
		;

		private final AppDef<? super PhoenixContactMeter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super PhoenixContactMeter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, PhoenixContactMeter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super PhoenixContactMeter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<PhoenixContactMeter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public PhoenixContactMeter(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var meterId = this.getId(t, p, Property.METER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var type = this.getEnum(p, MeterType.class, Property.TYPE);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);

			final var ip = this.getString(p, Property.IP);
			final var port = this.getInt(p, Property.PORT);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(modbusId,
							TranslationUtil.translate(AbstractOpenemsApp.getTranslationBundle(l), "App.Meter.alias"),
							"Bridge.Modbus.Tcp", //
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.addProperty("port", port) //
									.build()), //
					new EdgeConfig.Component(meterId, alias, "Meter.PhoenixContact", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("type", type) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected PhoenixContactMeter getApp() {
		return this;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanSee(Role.ADMIN)//
				.setCanDelete(Role.ADMIN) //
				.build();
	}

}
