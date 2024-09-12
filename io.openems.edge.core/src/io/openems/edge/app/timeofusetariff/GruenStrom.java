package io.openems.edge.app.timeofusetariff;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.validator.Checkables.checkHome;

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
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.timeofusetariff.GruenStrom.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;
@Component(name = "App.GruenStrom")
public class GruenStrom extends AbstractOpenemsAppWithProps<GruenStrom, Property, Type.Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, GruenStrom, Type.Parameter.BundleParameter>, Nameable {
		// Components
		ID(AppDef.componentId("gstr0")), //

		// Properties
		ALIAS(alias()),

		POSTAL_CODE(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("Postal Code") //
				.setRequired(true)//
				.setDefaultValue("94667")//
				.setField(JsonFormlyUtil::buildInput)));

		private final AppDef<? super GruenStrom, ? super Property, ? super Type.Parameter.BundleParameter> def;

		private Property(AppDef<? super GruenStrom, ? super Property, ? super Type.Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super GruenStrom, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<GruenStrom>, BundleParameter> getParamter() {
			return Type.Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public GruenStrom(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var postalCode = this.getString(p, l, Property.POSTAL_CODE);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(id, alias, "GruenPower.Reader",
							JsonUtils.buildJsonObject() //
									.addProperty("plz", postalCode) //
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
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TIME_OF_USE_TARIFF };
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(checkHome());
	}

	@Override
	protected GruenStrom getApp() {
		return this;
	}	
}
