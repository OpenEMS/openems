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
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.meter.KdkMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

/**
 * Describes a App for a Kdk meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Kdk",
    "alias":"Kdk Meter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"TYPE": "PRODUCTION",
    	"MODBUS_ID": "modbus1",
    	"MODBUS_UNIT_ID": 1
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.Kdk")
public class KdkMeter extends AbstractOpenemsAppWithProps<KdkMeter, Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, KdkMeter, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter1")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type())), //
		MODBUS_ID(AppDef.copyOfGeneric(ComponentProps.pickModbusId(),
				def -> def.wrapField((app, property, l, parameter, field) -> {
					if (PropsUtil.isHomeInstalled(app.getAppManagerUtil())) {
						field.readonly(true);
					}
					field.isRequired(true);
				})).setAutoGenerateField(false)), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), def -> def.setDefaultValue(7) //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true))) //
				.setAutoGenerateField(false)), //
		MODBUS_GROUP(AppDef.copyOfGeneric(CommunicationProps.modbusGroup(//
				MODBUS_ID, MODBUS_ID.def(), MODBUS_UNIT_ID, MODBUS_UNIT_ID.def()))), //
		;

		private final AppDef<? super KdkMeter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KdkMeter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KdkMeter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KdkMeter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KdkMeter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public KdkMeter(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			final var alias = this.getString(p, l, Property.ALIAS);
			final var type = this.getString(p, Property.TYPE);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
			final var modbusId = this.getString(p, Property.MODBUS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(meterId, alias, "Meter.KDK.2PUCT", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("type", type) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
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
	protected KdkMeter getApp() {
		return this;
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

}
