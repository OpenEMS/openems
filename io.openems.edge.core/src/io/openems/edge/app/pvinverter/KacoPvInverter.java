package io.openems.edge.app.pvinverter;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.pvinverter.KacoPvInverter.Property;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes a App for Kaco PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Kaco",
    "alias":"KACO PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.PvInverter.Kaco")
public class KacoPvInverter extends AbstractOpenemsAppWithProps<KacoPvInverter, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, KacoPvInverter, Parameter.BundleParameter> {
		// Component-IDs
		PV_INVERTER_ID(AppDef.componentId("pvInverter0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(CommonProps.alias()), //
		IP(PvInverterProps.ip()), //
		PORT(PvInverterProps.port()), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(PvInverterProps.modbusUnitId(), def -> def //
				.setDefaultValue(1))), //
		;

		private final AppDef<? super KacoPvInverter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super KacoPvInverter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, KacoPvInverter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super KacoPvInverter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<KacoPvInverter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public KacoPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);
			final var port = this.getInt(p, Property.PORT);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);

			final var components = List.of(//
					new EdgeConfig.Component(pvInverterId, alias, "PV-Inverter.KACO.blueplanet", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.build()), //
					new EdgeConfig.Component(modbusId, alias, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.addProperty("port", port) //
							.build())//
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_INVERTER };
	}

	@Override
	protected KacoPvInverter getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
