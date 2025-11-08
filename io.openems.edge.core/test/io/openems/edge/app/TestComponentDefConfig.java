package io.openems.edge.app;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestComponentDefConfig.Property;
import io.openems.edge.app.TestComponentDefConfig.TestComponentDefConfigParameter;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef.Configuration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Tests AppPropertyPermissions.
 */
@Component(name = "App.Test.TestComponentDefConfig")
public class TestComponentDefConfig
		extends AbstractOpenemsAppWithProps<TestComponentDefConfig, Property, TestComponentDefConfigParameter>
		implements OpenemsApp {
	public record TestComponentDefConfigParameter(ResourceBundle bundle) implements BundleProvider {

	}

	public static enum Property implements Type<Property, TestComponentDefConfig, TestComponentDefConfigParameter> {
		ID(AppDef.componentId("test0")), //
		MIN_POWER_SINGLE_PHASE(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setDefaultValue(1380);
			def.setField(JsonFormlyUtil::buildInputFromNameable);
		})), //
		;

		private final AppDef<? super TestComponentDefConfig, ? super Property, ? super TestComponentDefConfigParameter> def;

		private Property(
				AppDef<? super TestComponentDefConfig, ? super Property, ? super TestComponentDefConfigParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TestComponentDefConfig, TestComponentDefConfigParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestComponentDefConfig, ? super Property, ? super TestComponentDefConfigParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestComponentDefConfig>, TestComponentDefConfigParameter> getParamter() {
			return t -> {
				return new TestComponentDefConfigParameter(createResourceBundle(t.language));
			};
		}

	}

	@Activate
	public TestComponentDefConfig(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TEST };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected TestComponentDefConfig getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			var id = this.getId(t, p, Property.ID);
			var testValue = this.getInt(p, Property.MIN_POWER_SINGLE_PHASE);

			final var component = new ComponentDef(id, "test", "Evse.ElectricVehicle.Generic",
					ComponentProperties.fromJson(JsonUtils.buildJsonObject()//
							.addProperty("minPowerSinglePhase", testValue) //
							.addProperty("maxPowerSinglePhase", 3680) //
							.addProperty("minPowerThreePhase", 4140) //
							.addProperty("maxPowerThreePhase", 11040) //
							.addProperty("canInterrupt", true)//
							.build()), //
					Configuration.create()//
							.installAlways(true) //
							.build());
			return AppConfiguration.create() //
					.addTask(Tasks.component(component))//
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
