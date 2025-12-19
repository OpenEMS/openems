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

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
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
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.meter.api.PhaseRotation;

@Component(name = "App.Test.TestForceUpdatingConfigProperties")
public class TestForceUpdatingConfigProperties extends
		AbstractOpenemsAppWithProps<TestForceUpdatingConfigProperties, TestForceUpdatingConfigProperties.Property, TestForceUpdatingConfigProperties.TestForceUpdatingConfigPropertiesParameter>
		implements OpenemsApp {

	public record TestForceUpdatingConfigPropertiesParameter(ResourceBundle bundle)
			implements Type.Parameter.BundleProvider {
	}

	public static enum Property
			implements Type<Property, TestForceUpdatingConfigProperties, TestForceUpdatingConfigPropertiesParameter> {
		ID(AppDef.componentId("test0")), MIN_POWER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setDefaultValue(-1000);
			def.setField(JsonFormlyUtil::buildInputFromNameable);
		})), //
		MAX_POWER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> {
			def.setDefaultValue(1000);
			def.setField(JsonFormlyUtil::buildInputFromNameable);
		})), //
		PHASE_ROTATION(CommonProps.phaseRotation());

		private final AppDef<? super TestForceUpdatingConfigProperties, ? super Property, ? super TestForceUpdatingConfigPropertiesParameter> def;

		private Property(
				AppDef<? super TestForceUpdatingConfigProperties, ? super Property, ? super TestForceUpdatingConfigPropertiesParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super TestForceUpdatingConfigProperties, ? super Property, ? super TestForceUpdatingConfigPropertiesParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestForceUpdatingConfigProperties>, TestForceUpdatingConfigPropertiesParameter> getParamter() {
			return t -> {
				return new TestForceUpdatingConfigPropertiesParameter(createResourceBundle(t.language));
			};
		}

		@Override
		public Type<Property, TestForceUpdatingConfigProperties, TestForceUpdatingConfigPropertiesParameter> self() {
			return this;
		}
	}

	@Activate
	public TestForceUpdatingConfigProperties(@Reference ComponentManager componentManager,
			ComponentContext componentContext, @Reference ConfigurationAdmin cm,
			@Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			var id = this.getId(t, p, Property.ID);
			var minPower = this.getInt(p, Property.MIN_POWER);
			var maxPower = this.getInt(p, Property.MAX_POWER);
			var phaseRotation = this.getEnum(p, PhaseRotation.class, Property.PHASE_ROTATION);

			final var component = new ComponentDef(id, "test", //
					"Test.Force.Updating.Config", //
					ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
							.addProperty("minPower", minPower) //
							.addProperty("maxPower", maxPower) //
							.addProperty("phaseRotation", phaseRotation) //
							.build(), //
							"minPower", "maxPower"), //
					ComponentDef.Configuration.defaultConfig());
			return AppConfiguration.create().addTask(Tasks.component(component)).build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected TestForceUpdatingConfigProperties getApp() {
		return this;
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
}
