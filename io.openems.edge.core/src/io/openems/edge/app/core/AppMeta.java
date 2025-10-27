package io.openems.edge.app.core;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.core.AppMeta.Property;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.flag.Flags;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;
import io.openems.edge.core.appmanager.formly.enums.InputType;

@Component(name = "App.Core.Meta")
public class AppMeta extends AbstractOpenemsAppWithProps<AppMeta, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, AppMeta, Parameter.BundleParameter> {
		ALIAS(AppDef.copyOfGeneric(alias())), //

		CURRENCY(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".currency.label")
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Stream.of(CurrencyConfig.values()).map(Enum::name).toList());
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "currency", ComponentManagerSupplier::getComponentManager))), //

		IS_ESS_CHARGE_FROM_GRID_ALLOWED(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridCharge.label") //
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					var bundle = parameter.bundle();
					field.setPopupInput(property, DisplayType.BOOLEAN);
					field.setFieldGroup(JsonUtils.buildJsonArray() //
							.add(JsonFormlyUtil.buildText() //
									.setText(TranslationUtil.getTranslation(bundle,
											"App.Core.Meta.gridCharge.description"))
									.build())
							.add(JsonFormlyUtil.buildCheckboxFromNameable(property) //
									.setLabel(TranslationUtil.getTranslation(bundle, "App.Core.Meta.gridCharge.label")) //
									.build())
							.build());
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.INSTALLER)) //
				.bidirectional("_meta", "isEssChargeFromGridAllowed", ComponentManagerSupplier::getComponentManager))), //
		GRID_CONNECTION_POINT_FUSE_LIMIT(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridConnectionPointFuseLimit.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.onlyPositiveNumbers();
					field.setUnit(Unit.AMPERE, l);
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.INSTALLER)) //
				.bidirectional("_meta", "gridConnectionPointFuseLimit",
						ComponentManagerSupplier::getComponentManager))), //
		SUBDIVISION_CODE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".subdivisionCode.label")
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(Stream.of(SubdivisionCode.values()).map(Enum::name).toList());
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "subdivisionCode", ComponentManagerSupplier::getComponentManager))), //
		PLACE_NAME(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".placeName.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.TEXT);
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "placeName", ComponentManagerSupplier::getComponentManager))), //

		GRID_FEED_IN_LIMITATION_TYPE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridFeedInLimitationType.label") //
				.setDefaultValue(GridFeedInLimitationType.NO_LIMITATION) //
				.setField(JsonFormlyUtil::buildSelect, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(GridFeedInLimitationType.class), l);
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.INSTALLER)) //
				.bidirectional("_meta", "gridFeedInLimitationType", ComponentManagerSupplier::getComponentManager))), //

		MAXIMUM_GRID_FEED_IN_LIMIT(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridFeedInLimit.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(GRID_FEED_IN_LIMITATION_TYPE) //
							.notEqual(Exp.staticValue(GridFeedInLimitationType.NO_LIMITATION)));
					field.setInputType(InputType.NUMBER);
					field.onlyPositiveNumbers();
					field.setUnit(Unit.WATT, l);
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.INSTALLER)) //
				.bidirectional("_meta", "maximumGridFeedInLimit", ComponentManagerSupplier::getComponentManager))), //
		POSTCODE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".postcode.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.TEXT);
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "postcode", ComponentManagerSupplier::getComponentManager))), //
		LATITUDE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".latitude.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.setStep(0.0000001);
					field.setUnit(Unit.DECIMAL_DEGREE, l);
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "latitude", ComponentManagerSupplier::getComponentManager))), //

		LONGITUDE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".longitude.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.setStep(0.0000001);
					field.setUnit(Unit.DECIMAL_DEGREE, l);
				}) //
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "longitude", ComponentManagerSupplier::getComponentManager))), //
		TIMEZONE(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".timezone.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.TEXT);
				})//
				.appendIsAllowedToSee(AppDef.ofLeastRole(Role.ADMIN)) //
				.bidirectional("_meta", "timezone", ComponentManagerSupplier::getComponentManager))), //
		;

		private final AppDef<? super AppMeta, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppMeta, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppMeta, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppMeta, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppMeta>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppMeta(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var currency = this.getEnum(p, CurrencyConfig.class, Property.CURRENCY);
			final var isEssChargeFromGridAllowed = this.getBoolean(p, Property.IS_ESS_CHARGE_FROM_GRID_ALLOWED);
			final var gridConnectionPointFuseLimit = this.getInt(p, Property.GRID_CONNECTION_POINT_FUSE_LIMIT);
			final var subdivisionCode = this.getEnum(p, SubdivisionCode.class, Property.SUBDIVISION_CODE);
			final var placeName = this.getString(p, Property.PLACE_NAME);
			final var postcode = this.getString(p, Property.POSTCODE);
			final var gridFeedInLimitationType = this.getEnum(p, GridFeedInLimitationType.class,
					Property.GRID_FEED_IN_LIMITATION_TYPE);
			final var maximumGridFeedInLimit = this.getInt(p, Property.MAXIMUM_GRID_FEED_IN_LIMIT);
			final var latitude = this.getDouble(p, Property.LATITUDE);
			final var longitude = this.getDouble(p, Property.LONGITUDE);
			final var timezone = this.getString(p, Property.TIMEZONE);

			final var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component("_meta", "", "Core.Meta", //
					JsonUtils.buildJsonObject()//
							.addProperty("currency", currency)//
							.addProperty("isEssChargeFromGridAllowed", isEssChargeFromGridAllowed)//
							.addProperty("gridConnectionPointFuseLimit", gridConnectionPointFuseLimit)//
							.addProperty("subdivisionCode", subdivisionCode)//
							.addProperty("placeName", placeName)//
							.addProperty("postcode", postcode)//
							.addProperty("gridFeedInLimitationType", gridFeedInLimitationType) //
							.addProperty("maximumGridFeedInLimit", maximumGridFeedInLimit) //
							.addProperty("latitude", latitude)//
							.addProperty("longitude", longitude)//
							.addProperty("timezone", timezone)//
							.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.CORE };
	}

	@Override
	protected AppMeta getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanDelete(Role.ADMIN) // TODO theoretically not even admin
				.setCanSee(Role.INSTALLER) //
				.build();
	}

	@Override
	public Flag[] flags() {
		final var flags = Lists.newArrayList(super.flags());
		flags.add(Flags.ALWAYS_INSTALLED);
		return flags.toArray(Flag[]::new);
	}

}
