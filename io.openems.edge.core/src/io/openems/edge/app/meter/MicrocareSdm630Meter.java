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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.meter.MicrocareSdm630Meter.Property;
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
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppStatus;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a Microcare SDM630 meter App.
 *
 * <pre>
  {
    "appId":"App.Meter.Microcare.Sdm630",
    "alias":"SDM630 Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "METER_ID": "meter0",
      "TYPE": "PRODUCTION",
      "MODBUS_ID":"modbus0",
      "MODBUS_UNIT_ID":"10"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.Microcare.Sdm630")
public class MicrocareSdm630Meter
		extends AbstractOpenemsAppWithProps<MicrocareSdm630Meter, Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, MicrocareSdm630Meter, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		METER_ID(AppDef.componentId("meter0")), //
		// Properties
		ALIAS(CommonProps.alias()), //
		TYPE(MeterProps.type(MeterType.GRID)), //
		MODBUS_ID(AppDef.copyOfGeneric(ComponentProps.pickModbusId(), def -> def //
				.setRequired(true) //
				.setAutoGenerateField(false))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), def -> def //
				.setRequired(true) //
				.setAutoGenerateField(false) //
				.setDefaultValue(10))), //
		MODBUS_GROUP(CommunicationProps.modbusGroup(MODBUS_ID, MODBUS_ID.def(), //
				MODBUS_UNIT_ID, MODBUS_UNIT_ID.def())), //
		UNOFFICIAL_APP_WARNING(CommonProps.installationHintOfUnofficialApp()), //
		;

		private final AppDef<? super MicrocareSdm630Meter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super MicrocareSdm630Meter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, MicrocareSdm630Meter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super MicrocareSdm630Meter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<MicrocareSdm630Meter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public MicrocareSdm630Meter(//
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
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			// values the user enters
			final var alias = this.getString(p, l, Property.ALIAS);
			final var type = this.getString(p, Property.TYPE);
			final var modbusId = this.getString(p, Property.MODBUS_ID);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);

			// values which are being auto generated by the appmanager
			final var meterId = this.getId(t, p, Property.METER_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(meterId, alias, "Meter.Microcare.SDM630", JsonUtils.buildJsonObject() //
							.addProperty("type", type) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("modbus.id", modbusId) //
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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	protected OpenemsAppStatus getStatus() {
		return OpenemsAppStatus.BETA;
	}

	@Override
	protected MicrocareSdm630Meter getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
