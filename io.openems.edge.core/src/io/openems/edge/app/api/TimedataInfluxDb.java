package io.openems.edge.app.api;

import java.util.ArrayList;
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
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.TimedataInfluxDb.Property;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.OpenemsAppStatus;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

@Component(name = "App.Timedata.InfluxDb")
public class TimedataInfluxDb extends AbstractOpenemsAppWithProps<TimedataInfluxDb, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, TimedataInfluxDb, Parameter.BundleParameter> {
		// Component-IDs
		TIMEDATE_ID(AppDef.componentId("timedate0")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		QUERY_LANGUAGE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".queryLanguage.label") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, prop, l, params, field) -> {
					final var options = new ArrayList<String>();
					options.add("INFLUX_QL");
					options.add("FLUX");
					field.setOptions(options);
				}) //
				.setDefaultValue("INFLUX_QL"))), //
		URL(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".url.label") //
				.setTranslatedDescriptionWithAppPrefix(".url.description") //
				.setField(JsonFormlyUtil::buildInput) //
				.setDefaultValue("http://localhost:8086"))), //
		ORG(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".org.label") //
				.setTranslatedDescriptionWithAppPrefix(".org.description") //
				.setField(JsonFormlyUtil::buildInput) //
				.setDefaultValue("-"))), //
		API_KEY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".apiKey.label") //
				.setTranslatedLabelWithAppPrefix(".apiKey.description") //
				.setField(JsonFormlyUtil::buildInput)//
				.setRequired(true))),
		BUCKET(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".bucket.label") //
				.setTranslatedLabelWithAppPrefix(".bucket.description") //
				.setField(JsonFormlyUtil::buildInput)//
				.setRequired(true))),
		MEASUREMENT(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".measurement.label") //
				.setTranslatedLabelWithAppPrefix(".measurement.description") //
				.setField(JsonFormlyUtil::buildInput) //
				.setDefaultValue("data"))), //
		NO_OF_CYCLES(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".noOfCycles.description") //
				.setTranslatedLabelWithAppPrefix(".noOfCycles.label") //
				.setDefaultValue(1) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, first) -> {
					first.setInputType(InputType.NUMBER);
					first.setMin(1);
				}))), //
		MAX_QUEUE_SIZE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".maxQueueSize.description")//
				.setTranslatedLabelWithAppPrefix(".maxQueueSize.label") //
				.setDefaultValue(50) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, first) -> {
					first.setInputType(InputType.NUMBER);
				}))), //
		IS_READ_ONLY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".isReadOnly.description") //
				.setTranslatedLabelWithAppPrefix(".isReadOnly.label") //
				.setDefaultValue(false) //
				.setField(JsonFormlyUtil::buildCheckbox)));

		private final AppDef<? super TimedataInfluxDb, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super TimedataInfluxDb, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TimedataInfluxDb, BundleParameter> self() {
			return this;

		}

		@Override
		public AppDef<? super TimedataInfluxDb, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TimedataInfluxDb>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public TimedataInfluxDb(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);

	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.TIMEDATE_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var queryLanguage = this.getString(p, Property.QUERY_LANGUAGE);
			final var url = this.getString(p, Property.URL);
			final var apiKey = this.getString(p, Property.API_KEY);
			final var bucket = this.getString(p, Property.BUCKET);
			final var measuremtn = this.getString(p, Property.MEASUREMENT);
			final var noOfCycles = this.getInt(p, Property.NO_OF_CYCLES);
			final var maxQueueSize = this.getInt(p, Property.MAX_QUEUE_SIZE);
			final var isReadOnly = this.getBoolean(p, Property.IS_READ_ONLY);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(id, alias, "Timedata.InfluxDB", //
							JsonUtils.buildJsonObject() //
									.addProperty("queryLanguage", queryLanguage) //
									.addProperty("url", url) //
									.addProperty("apiKey", apiKey) //
									.addProperty("bucket", bucket) //
									.addProperty("measurement", measuremtn) //
									.addProperty("noOfCycles", noOfCycles) //
									.addProperty("maxQueueSize", maxQueueSize) //
									.addProperty("isReadOnly", isReadOnly)//
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
	public final OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIMEDATA };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected TimedataInfluxDb getApp() {
		return this;
	}

	@Override
	protected OpenemsAppStatus getStatus() {
		return OpenemsAppStatus.BETA;
	}
	
	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanSee(Role.ADMIN)//
				.build();
	}

}
