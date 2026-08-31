package io.openems.edge.app.ess;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.ess.SmaEssSunnyBoyStorage.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

/**
 * Describes an App for the SMA Sunny Boy Storage 2.5 ESS.
 *
 * <pre>
  {
    "appId":"App.Ess.Sma.SunnyBoyStorage",
    "alias":"SMA Sunny Boy Storage 2.5",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "ESS_ID": "ess0",
      "MODBUS_ID": "modbus0",
      "IP": "192.168.178.85",
      "PORT": "502",
      "MODBUS_UNIT_ID": "3",
      "CAPACITY": "2000",
      "READ_ONLY_MODE": "false"
    },
    "appDescriptor": {
      "websiteUrl": {@link io.openems.edge.core.appmanager.AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Ess.Sma.SunnyBoyStorage")
public class SmaEssSunnyBoyStorage extends
		AbstractOpenemsAppWithProps<SmaEssSunnyBoyStorage, Property, Parameter.BundleParameter> implements OpenemsApp {

	public static enum Property implements Type<Property, SmaEssSunnyBoyStorage, Parameter.BundleParameter>, Nameable {

		// Component-IDs
		ESS_ID(AppDef.componentId("ess0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //

		// User-visible properties
		ALIAS(CommonProps.alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.ip(), def -> def//
				.setDefaultValue("192.168.178.85")//
				.setRequired(true))), //
		PORT(AppDef.copyOfGeneric(CommunicationProps.port(), def -> def//
				.setRequired(true))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(CommunicationProps.modbusUnitId(), def -> def//
				.setDefaultValue(3)//
				.setRequired(true))), //
		CAPACITY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setLabel("Kapazität [Wh]") //
				.setDescription("Nutzbare Netto-Kapazität der Batterie in Wh. SBS 2.5 = 2000 Wh") //
				.setDefaultValue(2000) //
				.setField(JsonFormlyUtil::buildInputFromNameable,
						(app, prop, l, param, f) -> f.setInputType(InputType.NUMBER).setMin(0)))), //
		READ_ONLY_MODE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setLabel("Nur-Lesen-Modus")//
				.setDescription("Aktiviert den Nur-Lesen-Modus; keine Sollwerte werden an das Gerät geschrieben.")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		;

		private final AppDef<? super SmaEssSunnyBoyStorage, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super SmaEssSunnyBoyStorage, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super SmaEssSunnyBoyStorage, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SmaEssSunnyBoyStorage>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public SmaEssSunnyBoyStorage(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, l, Property.IP);
			final var port = this.getInt(p, Property.PORT);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);
			final var capacity = this.getInt(p, Property.CAPACITY);
			final var readOnlyMode = this.getBoolean(p, Property.READ_ONLY_MODE);

			final var essId = this.getId(t, p, Property.ESS_ID);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);

			final var components = List.of(//
					new EdgeConfig.Component(essId, alias, "Ess.Sma.SunnyBoyStorage", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("capacity", capacity) //
									.addProperty("readOnlyMode", readOnlyMode) //
									.build()), //
					new EdgeConfig.Component(modbusId, alias, "Bridge.Modbus.Tcp", //
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.addProperty("port", port) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.ESS };
	}

	@Override
	protected SmaEssSunnyBoyStorage getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}
}
