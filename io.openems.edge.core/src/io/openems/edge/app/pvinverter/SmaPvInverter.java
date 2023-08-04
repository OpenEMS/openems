package io.openems.edge.app.pvinverter;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.app.pvinverter.SmaPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a App for SMA PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Sma",
    "alias":"SMA PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502",
    	"MODBUS_UNIT_ID": "126"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Sma")
public class SmaPvInverter extends AbstractOpenemsAppWithProps<SmaPvInverter, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, SmaPvInverter, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		PV_INVERTER_ID(AppDef.of(SmaPvInverter.class) //
				.setDefaultValue("pvInverter0")), //
		MODBUS_ID(AppDef.of(SmaPvInverter.class) //
				.setDefaultValue("modbus0")), //
		// Properties
		ALIAS(AppDef.of(SmaPvInverter.class) //
				.setDefaultValueToAppName()), //
		IP(AppDef.copyOfGeneric(CommonPvInverterConfiguration.ip(), def -> def //
				.wrapField((app, property, l, parameter, field) -> {
					field.isRequired(true);
				}))), //
		PORT(AppDef.copyOfGeneric(CommonPvInverterConfiguration.port(), def -> def //
				.wrapField((app, property, l, parameter, field) -> {
					field.isRequired(true);
				}))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(CommonPvInverterConfiguration.modbusUnitId(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".modbusUnitId.description") //
				.wrapField((app, property, l, parameter, field) -> {
					field.isRequired(true);
				}))), //
		PHASE(AppDef.of(SmaPvInverter.class) //
				.setTranslatedLabelWithAppPrefix(".phase.label") // )
				.setTranslatedDescriptionWithAppPrefix(".phase.description") //
				.setDefaultValue(Phase.ALL.name()) //
				.bidirectional(PV_INVERTER_ID, "phase", a -> a.componentManager) //
				.setField(JsonFormlyUtil::buildSelect, (app, property, l, parameter, field) -> //
				field.setOptions(OptionsFactory.of(Phase.class), l) //
						.isRequired(true)));

		private final AppDef<SmaPvInverter, Property, BundleParameter> def;

		private Property(AppDef<SmaPvInverter, Property, BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<SmaPvInverter, Property, BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SmaPvInverter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public SmaPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
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
			final var phase = this.getEnum(p, Phase.class, Property.PHASE);

			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID);

			final var factoryIdInverter = "PV-Inverter.SMA.SunnyTripower";
			final var components = CommonPvInverterConfiguration.getComponents(//
					factoryIdInverter, pvInverterId, modbusId, alias, ip, port,
					b -> b.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("phase", phase),
					null);
			return new AppConfiguration(components);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fenecon-fems/fems-app-pv-wechselrichter/") //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_INVERTER };
	}

	@Override
	protected SmaPvInverter getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
