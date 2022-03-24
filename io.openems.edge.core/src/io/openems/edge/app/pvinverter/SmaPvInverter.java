package io.openems.edge.app.pvinverter;

import java.util.EnumMap;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.SmaPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.validator.Checkable;

/**
 * Describes a App for AwattarHourly.
 *
 * <pre>
  {
    "appId":"App.PvInverter.SmaPvInverter",
    "alias":"SMA PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502",
    	"MODBUS_UNIT_ID": "126"
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.SmaPvInverter")
public class SmaPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		PV_INVERTER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		IP, // the ip for the modbus
		PORT, //
		MODBUS_UNIT_ID;
	}

	@Activate
	public SmaPvInverter(@Reference ComponentManager componentManager, ComponentContext context) {
		super(componentManager, context);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName());
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var modbus0 = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverter0 = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryId = "PV-Inverter.SMA.SunnyTripower";
			var components = this.getComponents(factoryId, pvInverter0, modbus0, alias, ip, port);
			var inverter = this.getComponentWithFactoryId(components, factoryId);
			inverter.getProperties().put("modbusUnitId", JsonUtils.parse(Integer.toString(modbusUnitId)));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP, "192.168.178.85", true)) //
						.add(JsonFormlyUtil.buildInput(Property.PORT, "502", true, true)) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID, "126", true, true)) //
						.build())
				.build();
	}

	@Override
	public String getImage() {
		return super.getImage();
	}

	@Override
	public String getName() {
		return "SMA PV-Wechselrichter";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, List<Checkable>, OpenemsNamedException> installationValidation() {
		return (t, p) -> Lists.newArrayList();
	}

}
