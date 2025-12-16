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
import io.openems.edge.meter.api.PhaseRotation;

@Component(name = "App.Test.TestForceUpdatingConfigComponent")
public class TestForceUpdatingConfigComponent extends
		AbstractOpenemsAppWithProps<TestForceUpdatingConfigComponent, TestForceUpdatingConfigComponent.Property, TestForceUpdatingConfigComponent.TestForceUpdatingConfigComponentParameter>
		implements OpenemsApp {

	public record TestForceUpdatingConfigComponentParameter(ResourceBundle bundle)
			implements Type.Parameter.BundleProvider {
	}

	public static enum Property
			implements Type<Property, TestForceUpdatingConfigComponent, TestForceUpdatingConfigComponentParameter> {
		ID(AppDef.componentId("test0")), PHASE_ROTATION(CommonProps.phaseRotation());

		private final AppDef<? super TestForceUpdatingConfigComponent, ? super Property, ? super TestForceUpdatingConfigComponentParameter> def;

		private Property(
				AppDef<? super TestForceUpdatingConfigComponent, ? super Property, ? super TestForceUpdatingConfigComponentParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super TestForceUpdatingConfigComponent, ? super Property, ? super TestForceUpdatingConfigComponentParameter> def() {
			return def;
		}

		@Override
		public Function<GetParameterValues<TestForceUpdatingConfigComponent>, TestForceUpdatingConfigComponentParameter> getParamter() {
			return t -> {
				return new TestForceUpdatingConfigComponentParameter(createResourceBundle(t.language));
			};
		}

		@Override
		public Type<Property, TestForceUpdatingConfigComponent, TestForceUpdatingConfigComponentParameter> self() {
			return this;
		}
	}

	@Activate
	public TestForceUpdatingConfigComponent(@Reference ComponentManager componentManager,
			ComponentContext componentContext, @Reference ConfigurationAdmin cm,
			@Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			var id = this.getId(t, p, Property.ID);
			var phaseRotation = this.getEnum(p, PhaseRotation.class, Property.PHASE_ROTATION);

			final var component = new ComponentDef(id, "test", //
					"Test.Force.Updating.Config", //
					ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
							.addProperty("phaseRotation", phaseRotation) //
							.build()), //
					ComponentDef.Configuration.create() //
							.forceUpdateOrCreate(true) //
							.build());
			return AppConfiguration.create().addTask(Tasks.component(component)).build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected TestForceUpdatingConfigComponent getApp() {
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
