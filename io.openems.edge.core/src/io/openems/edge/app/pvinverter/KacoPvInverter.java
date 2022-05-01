package io.openems.edge.app.pvinverter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.KacoPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;

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
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/">https://fenecon.de/fems-2-2/fems-app-kaco-pv-wechselrichter/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Kaco")
public class KacoPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT;
	}

	@Activate
	public KacoPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName());
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "PV-Inverter.KACO.blueplanet";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel("IP-Address") //
								.setDescription("The IP address of the Pv-Inverter.") //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel("Port") //
								.setDescription("The port of the Pv-Inverter.") //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "KACO PV-Wechselrichter";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

}
