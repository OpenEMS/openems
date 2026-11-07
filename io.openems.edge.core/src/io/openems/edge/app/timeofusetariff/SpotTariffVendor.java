package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.core.appmanager.formly.enums.InputType.PASSWORD;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

@Component(name = "App.Tariff.SpotTariff")
public class SpotTariffVendor
		extends AbstractOpenemsAppWithProps<SpotTariffVendor, SpotTariffVendor.Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, SpotTariffVendor, Type.Parameter.BundleParameter>, Nameable {
		// Component-IDs
		SPOT_TARIFF_ID(AppDef.componentId("spotTariff0")), //

		// Properties
		PRICE_SOURCE(SpotTariffVendorProps.vendorType()), //
		ACCESS_TOKEN(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setTranslatedLabelWithAppPrefix(".accessToken.label") //
				.setTranslatedDescriptionWithAppPrefix(".accessToken.description") //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInput, (app, prop, l, params, field) -> {
					field.setInputType(PASSWORD);
					field.onlyShowIf(Exp.currentModelValue(PRICE_SOURCE)
							.equal(Exp.staticValue(SpotTariffVendorProps.SpotTariffPriceSource.ENTSOE)));
				}) //
				.bidirectional(SPOT_TARIFF_ID, "securityToken", ComponentManagerSupplier::getComponentManager, t -> {
					return JsonUtils.getAsOptionalString(t) //
							.map(s -> {
								if (s.isEmpty()) {
									return null;
								}
								return new JsonPrimitive(CommonProps.MASKED_PASSWORD);
							})//
							.orElse(null);
				})));

		private final AppDef<? super SpotTariffVendor, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(
				AppDef<? super SpotTariffVendor, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super SpotTariffVendor, ? super Property, ? super Type.Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<SpotTariffVendor>, Type.Parameter.BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public SpotTariffVendor(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var spotTariffId = this.getId(t, p, Property.SPOT_TARIFF_ID);
			final var priceSource = this.getEnum(p, SpotTariffVendorProps.SpotTariffPriceSource.class,
					Property.PRICE_SOURCE);

			final var alias = this.getName(l);
			final var accessToken = this.getValueOrDefault(p, Property.ACCESS_TOKEN, null);

			EdgeConfig.Component component = switch (priceSource) {
			case BACKEND -> {
				yield new EdgeConfig.Component(spotTariffId, alias, "Tariff.SpotTariff.FromBackend",
						JsonUtils.buildJsonObject().build());
			}
			case ENTSOE -> {
				yield new EdgeConfig.Component(spotTariffId, alias, "Tariff.SpotTariff.ENTSO-E", JsonUtils
						.buildJsonObject() //
						.onlyIf(accessToken != null && !accessToken.equals(CommonProps.MASKED_PASSWORD),
								b -> {
									b.addProperty("securityToken", accessToken);
								}) //
						.build());
			}
			case SMARD -> {
				yield new EdgeConfig.Component(spotTariffId, alias, "Tariff.SpotTariff.SMARD",
						JsonUtils.buildJsonObject() //
								.build());
			}
			};

			return AppConfiguration.create() //
					.addTask(Tasks.component(component)) //
					.build();
		};
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.CORE };
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
	protected SpotTariffVendor getApp() {
		return this;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.setCanInstall(Role.ADMIN) //
				.build();
	}

	/**
	 * Creates a dependency for a {@link SpotTariffVendor}.
	 *
	 * @return a {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration dependency() {
		return new DependencyDeclaration("SPOT_TARIFF_VENDOR", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Tariff.SpotTariff") //
						.build() //
		);
	}
}
