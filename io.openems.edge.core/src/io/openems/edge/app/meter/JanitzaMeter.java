package io.openems.edge.app.meter;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.JanitzaMeter.Property;
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
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for a Janitza meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Janitza",
    "alias":"Janitza Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"MODBUS_ID": "modbus2",
    	"TYPE": "PRODUCTION",
    	"MODEL": "Meter.Janitza.UMG96RME",
    	"IP": "10.4.0.12",
    	"MODBUS_UNIT_ID": 1
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.Janitza")
public class JanitzaMeter extends AbstractMeterApp<Property> implements OpenemsApp {

	public enum Property implements Nameable {
		// Component-IDs
		METER_ID, //
		MODBUS_ID, //
		// Properties
		ALIAS, //
		MODEL, //
		TYPE, //
		IP, //
		MODBUS_UNIT_ID, //
		;
	}

	@Activate
	public JanitzaMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			// TODO which modbus should be used(new or already existing from home) only one
			// meter installed so far.

			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus2");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var factorieId = this.getValueOrDefault(p, Property.MODEL, "Meter.Janitza.UMG96RME");
			var type = this.getValueOrDefault(p, Property.TYPE, "PRODUCTION");
			var ip = this.getValueOrDefault(p, Property.IP, "10.4.0.12");
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(meterId, alias, factorieId, //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("type", type) //
									.build()), //
					new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", //
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.MODEL) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".productModel")) //
								.isRequired(true) //
								.setOptions(this.buildFactorieIdOptions()) //
								.build()) //
						.add(JsonFormlyUtil.buildSelect(Property.TYPE) //
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Meter.mountType.label")) //
								.isRequired(true) //
								.setOptions(this.buildMeterOptions(language)) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "App.Meter.ip.description")) //
								.isRequired(true) //
								.setDefaultValue("10.4.0.12") //
								.setValidation(Validation.IP) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusUnitId")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.Meter.modbusUnitId.description")) //
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

	protected final Set<Entry<String, String>> buildFactorieIdOptions() {
		var values = new HashSet<Entry<String, String>>();
		values.add(Map.entry("Janitza Netzanalysator UMG 96RM-E", "Meter.Janitza.UMG96RME"));
		values.add(Map.entry("Janitza Netzanalysator UMG 604-PRO", "Meter.Janitza.UMG604"));
		values.add(Map.entry("Janitza Netzqualitätsanalysator UMG 511", "Meter.Janitza.UMG511"));
		return values;
	}

}
