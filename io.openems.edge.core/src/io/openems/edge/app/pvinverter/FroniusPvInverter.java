package io.openems.edge.app.pvinverter;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.FroniusPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
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
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for Fronius PV-Inverter.
 *
 * <pre>
  {
    "appId":"App.PvInverter.Fronius",
    "alias":"Fronius PV-Wechselrichter",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"PV_INVERTER_ID": "pvInverter0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.178.85",
    	"PORT": "502",
    	"MODBUS_UNIT_ID": 1
    },
    "appDescriptor": {
    	"websiteUrl": URL
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.PvInverter.Fronius")
public class FroniusPvInverter extends AbstractPvInverter<Property> implements OpenemsApp {

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
	public FroniusPvInverter(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var ip = this.getValueOrDefault(p, Property.IP, "192.168.178.85");
			var port = EnumUtils.getAsInt(p, Property.PORT);
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus0");
			var pvInverterId = this.getId(t, p, Property.PV_INVERTER_ID, "pvInverter0");

			var factoryIdInverter = "PV-Inverter.Fronius";
			var components = this.getComponents(factoryIdInverter, pvInverterId, modbusId, alias, ip, port);
			var inverter = AbstractOpenemsApp.getComponentWithFactoryId(components, factoryIdInverter);
			inverter.getProperties().put("modbusUnitId", new JsonPrimitive(modbusUnitId));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.ip.description")) //
								.setDefaultValue("192.168.178.85") //
								.isRequired(true) //
								.setValidation(Validation.IP) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PORT) //
								.setLabel(TranslationUtil.getTranslation(bundle, "port")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.PvInverter.port.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(502) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusUnitId")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "modbusUnitId.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(1) //
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
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8Y"
				+ "QUAAAAJcEhZcwAALiIAAC4iAari3ZIAAAUtaVRYdFhNTDpjb20uYWRvYmUueG1wAAAAAAA8P3hwYWNrZXQgYmVnaW49Iu+7vyIga"
				+ "WQ9Ilc1TTBNcENlaGlIenJlU3pOVGN6a2M5ZCI/Pg0KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0a"
				+ "z0iQWRvYmUgWE1QIENvcmUgNy4yLWMwMDAgNzkuNTY2ZWJjNSwgMjAyMi8wNS8wOS0wNzoyMjoyOSAgICAgICAgIj4NCiAgPHJkZ"
				+ "jpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4NCiAgICA8cmRmOkRlc"
				+ "2NyaXB0aW9uIHJkZjphYm91dD0iIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtbG5zOmRjPSJod"
				+ "HRwOi8vcHVybC5vcmcvZGMvZWxlbWVudHMvMS4xLyIgeG1sbnM6cGhvdG9zaG9wPSJodHRwOi8vbnMuYWRvYmUuY29tL3Bob3Rvc"
				+ "2hvcC8xLjAvIiB4bWxuczp4bXBNTT0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wL21tLyIgeG1sbnM6c3RFdnQ9Imh0dHA6L"
				+ "y9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob"
				+ "3AgMjMuNCAoV2luZG93cykiIHhtcDpDcmVhdGVEYXRlPSIyMDIyLTA3LTA0VDEyOjEwOjE1KzAyOjAwIiB4bXA6TW9kaWZ5RGF0Z"
				+ "T0iMjAyMi0wNy0wNFQxMjoxMDozMyswMjowMCIgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMi0wNy0wNFQxMjoxMDozMyswMjowMCIgZ"
				+ "GM6Zm9ybWF0PSJpbWFnZS9wbmciIHBob3Rvc2hvcDpDb2xvck1vZGU9IjMiIHhtcE1NOkluc3RhbmNlSUQ9InhtcC5paWQ6ZjVhY"
				+ "zM4ZDUtMTJjYy05MTRmLWE0YWUtYjRlNjc2NzI5ODdjIiB4bXBNTTpEb2N1bWVudElEPSJ4bXAuZGlkOmY1YWMzOGQ1LTEyY2MtO"
				+ "TE0Zi1hNGFlLWI0ZTY3NjcyOTg3YyIgeG1wTU06T3JpZ2luYWxEb2N1bWVudElEPSJ4bXAuZGlkOmY1YWMzOGQ1LTEyY2MtOTE0Z"
				+ "i1hNGFlLWI0ZTY3NjcyOTg3YyI+DQogICAgICA8eG1wTU06SGlzdG9yeT4NCiAgICAgICAgPHJkZjpTZXE+DQogICAgICAgICAgP"
				+ "HJkZjpsaSBzdEV2dDphY3Rpb249ImNyZWF0ZWQiIHN0RXZ0Omluc3RhbmNlSUQ9InhtcC5paWQ6ZjVhYzM4ZDUtMTJjYy05MTRmL"
				+ "WE0YWUtYjRlNjc2NzI5ODdjIiBzdEV2dDp3aGVuPSIyMDIyLTA3LTA0VDEyOjEwOjE1KzAyOjAwIiBzdEV2dDpzb2Z0d2FyZUFnZ"
				+ "W50PSJBZG9iZSBQaG90b3Nob3AgMjMuNCAoV2luZG93cykiIC8+DQogICAgICAgIDwvcmRmOlNlcT4NCiAgICAgIDwveG1wTU06S"
				+ "GlzdG9yeT4NCiAgICA8L3JkZjpEZXNjcmlwdGlvbj4NCiAgPC9yZGY6UkRGPg0KPC94OnhtcG1ldGE+DQo8P3hwYWNrZXQgZW5kP"
				+ "SJyIj8+p3UaTgAAK0tJREFUeF7t3Qd8FFXXBvCzKaRC6L333osoCChFiiDYUAQUBQQ+FVFAmiLSfVWsiMhrwy6gCAqIgq+AilQBa"
				+ "SKICkqT3kKY7zx3Z4fdkJBCEjLJ8/d3nMzdzSbZ8nBn5t4ZISIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiI"
				+ "iIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiI"
				+ "iIiIiIiIiIiIiKidOaxl0RpIrLVsFK6mOxdkyEnv5rwu/010WVjYFGa0KCK0sUQrUexijZ1UutplAbXCdNCdBkYWHRZNKiCdHGb1"
				+ "iStkmhLwG6toVofaXCdNy1EqcDAolTTsKqvi2e1mpoGFZInl8Q0qWu+PrJsjZz796j52vad1iANrVXeVaKUYWBRimlQFdbFWK2eW"
				+ "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"
				+ "tLgqqwLbCaig4XpNFvQTkRERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERER"
				+ "ERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERERJmfyP8DwBV7u"
				+ "qWD4+kAAAAASUVORK5CYII=";
	}

}
