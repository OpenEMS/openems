package io.openems.edge.app.api;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.MqttApi.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for MQTT Api.
 *
 * <pre>
  {
    "appId":"App.Api.Mqtt",
    "alias":"MQTT-Api",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlControllerApiMqtt0",
    	"USERNAME": "username",
    	"PASSWORD": "******",
    	"CLIENT_ID": "edge0",
    	"URI": "tcp://localhost:1883"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.Mqtt")
public class MqttApi extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements Nameable {
		// Components
		CONTROLLER_ID, //
		// User-Values
		USERNAME, //
		PASSWORD, //
		CLIENT_ID, //
		URI;
	}

	@Activate
	public MqttApi(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.USERNAME) //
								.setLabel(TranslationUtil.getTranslation(bundle, "username")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".Username.description")) //
								.isRequired(true) //
								.setMinLenght(3) //
								.setMaxLenght(18) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PASSWORD) //
								.setLabel(TranslationUtil.getTranslation(bundle, "password")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".Password.description")) //
								.isRequired(true) //
								.setInputType(Type.PASSWORD) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.CLIENT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".EdgeId.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".EdgeId.description")) //
								.setDefaultValue("edge0") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.URI) //
								.setLabel("Uri") //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".Uri.description")) //
								.setDefaultValue("tcp://localhost:1883") //
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
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var clientId = this.getValueOrDefault(p, Property.CLIENT_ID, "edge0");
			var uri = this.getValueOrDefault(p, Property.URI, "tcp://localhost:1883");

			var username = EnumUtils.getAsString(p, Property.USERNAME);
			var password = this.getValueOrDefault(p, Property.PASSWORD, "xxx");

			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlControllerApiMqtt0");

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.MQTT",
							JsonUtils.buildJsonObject() //
									.addProperty("clientId", clientId) //
									.addProperty("uri", uri) //
									.addProperty("username", username) //
									.onlyIf(t.isAddOrUpdate(), c -> c.addProperty("password", password)) //
									.build()));

			// remove password after use so it does not get save
			p.remove(Property.PASSWORD);

			return new AppConfiguration(components);
		};
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

}
