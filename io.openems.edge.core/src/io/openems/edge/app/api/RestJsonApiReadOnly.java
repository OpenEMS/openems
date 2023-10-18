package io.openems.edge.app.api;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.RestJsonApiReadOnly.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppAssistant;
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
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a App for ReadOnly Rest JSON Api.
 *
 * <pre>
  {
    "appId":"App.Api.RestJson.ReadOnly",
    "alias":"REST/JSON-Api Read-Only",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ACTIVE": true,
    	"CONTROLLER_ID": "ctrlApiRest0"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Api.RestJson.ReadOnly")
public class RestJsonApiReadOnly extends AbstractOpenemsAppWithProps<RestJsonApiReadOnly, Property, BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, RestJsonApiReadOnly, BundleParameter>, Nameable {
		// Components
		CONTROLLER_ID(AppDef.of(RestJsonApiReadOnly.class) //
				.setDefaultValue("ctrlApiRest0")), //
		// Properties
		ALIAS(alias()), //
		ACTIVE(AppDef.of(RestJsonApiReadOnly.class) //
				.setDefaultValue((app, prop, l, param) -> {
					var active = app.componentManager.getEdgeConfig()
							.getComponentIdsByFactory("Controller.Api.Rest.ReadWrite").isEmpty();
					return new JsonPrimitive(active);
				}) //
				.setField(JsonFormlyUtil::buildCheckbox)), //
		;

		private final AppDef<? super RestJsonApiReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super RestJsonApiReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super RestJsonApiReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public Function<GetParameterValues<RestJsonApiReadOnly>, BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public RestJsonApiReadOnly(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/produkte/fems/fems-app-rest-json-lesend/") //
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
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			if (!this.getBoolean(p, Property.ACTIVE)) {
				return new AppConfiguration();
			}
			var controllerId = this.getId(t, p, Property.CONTROLLER_ID);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.Rest.ReadOnly",
							JsonUtils.buildJsonObject() //
									.build()) //
			);

			return new AppConfiguration(components);
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected RestJsonApiReadOnly getApp() {
		return this;
	}

}
