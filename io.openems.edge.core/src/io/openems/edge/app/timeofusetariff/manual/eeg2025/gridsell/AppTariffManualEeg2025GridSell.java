package io.openems.edge.app.timeofusetariff.manual.eeg2025.gridsell;

import java.util.Map;
import java.util.Set;
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
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.timeofusetariff.TimeOfUseProps;
import io.openems.edge.app.timeofusetariff.manual.eeg2025.gridsell.AppTariffManualEeg2025GridSell.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.types.CountryCode;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.validator.Checkables;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

@Component(name = "App.Tariff.Manual.EEG2025.GridSell")
public class AppTariffManualEeg2025GridSell
		extends AbstractOpenemsAppWithProps<AppTariffManualEeg2025GridSell, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property
			implements Type<Property, AppTariffManualEeg2025GridSell, Type.Parameter.BundleParameter>, Nameable {
		TARIFF_GRID_SELL_ID(AppDef.componentId("tariffGridSell0")), //
		FIXED_GRID_SELL_PRICE(TimeOfUseProps.price(".fixedGridSellPrice")), //
		;

		private final AppDef<? super AppTariffManualEeg2025GridSell, ? super Property, ? super Parameter.BundleParameter> def;

		Property(
				AppDef<? super AppTariffManualEeg2025GridSell, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super AppTariffManualEeg2025GridSell, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppTariffManualEeg2025GridSell>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public AppTariffManualEeg2025GridSell(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var tariffGridSellId = this.getId(t, p, Property.TARIFF_GRID_SELL_ID);
			final var fixedGridSellPrice = this.getDouble(p, Property.FIXED_GRID_SELL_PRICE);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(tariffGridSellId, this.getName(l), "Tariff.Manual.EEG2025.GridSell",
							JsonUtils.buildJsonObject() //
									.addProperty("fixedGridSellPrice", fixedGridSellPrice) //
									.build())//
			);

			return AppConfiguration.create()//
					.addTask(Tasks.component(components))//
					.build();
		};
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TARIFF_GRID_SELL };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create()//
				.setCompatibleCheckableConfigs(//
						Checkables.checkCountry(Set.of(CountryCode.DE)), //
						Checkables.checkEnergySchedulerV2());
	}

	@Override
	protected AppTariffManualEeg2025GridSell getApp() {
		return this;
	}
}
