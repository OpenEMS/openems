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
import io.openems.edge.common.component.ComponentManager;
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
import io.openems.edge.core.appmanager.OpenemsAppStatus;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.flag.Flags;
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
				}) //
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
				}) //
				.bidirectional("_meta", "isEssChargeFromGridAllowed", ComponentManagerSupplier::getComponentManager))), //
		GRID_CONNECTION_POINT_FUSE_LIMIT(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".gridConnectionPointFuseLimit.label")
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.onlyPositiveNumbers();
					field.setUnit(Unit.WATT, l);
				}).bidirectional("_meta", "gridConnectionPointFuseLimit", ComponentManagerSupplier::getComponentManager))), //
		;

		private AppDef<? super AppMeta, ? super Property, ? super BundleParameter> def;

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

			final var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component("_meta", "", "Core.Meta", //
					JsonUtils.buildJsonObject() //
							.addProperty("currency", currency) //
							.addProperty("isEssChargeFromGridAllowed", isEssChargeFromGridAllowed) //
							.addProperty("gridConnectionPointFuseLimit", gridConnectionPointFuseLimit) //
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
				.build();
	}

	@Override
	public Flag[] flags() {
		final var flags = new ArrayList<>();
		if (this.getStatus() == OpenemsAppStatus.BETA) {
			flags.add(Flags.SHOW_AFTER_KEY_REDEEM);
		}
		flags.add(Flags.ALWAYS_INSTALLED);
		return flags.toArray(Flag[]::new);
	}

}
