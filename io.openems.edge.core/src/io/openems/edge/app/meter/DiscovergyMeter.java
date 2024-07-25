package io.openems.edge.app.meter;

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
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.meter.DiscovergyMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
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
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.Meter.Discovergy")
public class DiscovergyMeter extends AbstractOpenemsAppWithProps<DiscovergyMeter, Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, DiscovergyMeter, Parameter.BundleParameter> {
		// Component-IDs
		METER_ID(AppDef.componentId("meter0")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		TYPE(AppDef.copyOfGeneric(MeterProps.type(MeterType.GRID))), //
		EMAIL(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".email.label") //
				.setTranslatedDescriptionWithAppPrefix(".email.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable).setRequired(true))), //
		PASSWORD(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".password.label") //
				.setTranslatedDescriptionWithAppPrefix(".password.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, prop, l, params, field) -> {
					field.setInputType(io.openems.edge.core.appmanager.formly.enums.InputType.PASSWORD);
				}) //
				.bidirectional(METER_ID, "password", ComponentManagerSupplier::getComponentManager, t -> {
					return JsonUtils.getAsOptionalString(t) //
							.map(s -> {
								if (s.isEmpty()) {
									return null;
								}
								return new JsonPrimitive("xxx");
							}) //
							.orElse(null);
				}))), //
		SERIAL_NUMBER_TYPE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".serialType.label")
				.setField(JsonFormlyUtil::buildSelect, (app, prop, l, params, field) -> {
					final var options = new ArrayList<String>();
					options.add("SERIAL_NUMBER");
					options.add("FULL_SERIAL_NUMBER");
					options.add("METER_ID");
					field.setOptions(options);
				}) //
		)), //
		SERIAL_NUMBER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".serialNumber.label") //
				.setDefaultValue("")//
				.setTranslatedDescriptionWithAppPrefix(".serialNumber.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable) //
				.wrapField((app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(SERIAL_NUMBER_TYPE).equal(Exp.staticValue("SERIAL_NUMBER"))); //
				}))), //
		FULL_SERIAL_NUMBER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".fullSerialNumber.label") //
				.setDefaultValue("")//
				.setTranslatedDescriptionWithAppPrefix(".fullSerialNumber.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> { //
					field.onlyShowIf(
							Exp.currentModelValue(SERIAL_NUMBER_TYPE).equal(Exp.staticValue("FULL_SERIAL_NUMBER"))); //
				}))), //
		DISCOVERGY_METER_ID(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedDescriptionWithAppPrefix(".meterId.description") //
				.setTranslatedLabelWithAppPrefix(".meterId.label")//
				.setDefaultValue("")//
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> { //
					field.onlyShowIf(Exp.currentModelValue(SERIAL_NUMBER_TYPE).equal(Exp.staticValue("METER_ID"))); //
				}))),//
		;

		private final AppDef<? super DiscovergyMeter, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super DiscovergyMeter, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, DiscovergyMeter, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super DiscovergyMeter, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<DiscovergyMeter>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public DiscovergyMeter(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.METER_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var meterType = this.getString(p, Property.TYPE);
			final var email = this.getString(p, Property.EMAIL);
			final var password = this.getString(p, Property.PASSWORD);
			final var meterId = this.getString(p, Property.DISCOVERGY_METER_ID);
			final var serialNumber = this.getString(p, Property.SERIAL_NUMBER);
			final var fullSerialNumber = this.getString(p, Property.FULL_SERIAL_NUMBER);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(id, alias, "Meter.Discovergy", //
							JsonUtils.buildJsonObject() //
									.addProperty("type", meterType)//
									.addProperty("meterId", meterId) //
									.addProperty("email", email) //
									.addProperty("serialNumber", serialNumber) //
									.addProperty("fullSerialNumber", fullSerialNumber)//
									.onlyIf(password != null && !password.equals("xxx"), b -> {
										b.addProperty("password", password);
									}) //
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.METER };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected DiscovergyMeter getApp() {
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
