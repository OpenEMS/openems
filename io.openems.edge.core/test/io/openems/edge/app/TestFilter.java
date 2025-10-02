package io.openems.edge.app;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.app.TestFilter.Property;
import io.openems.edge.app.TestFilter.TestPermissionsParameter;
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

/**
 * Tests GetAppByFilter.
 */
@Component(name = "App.Test.TestFilter")
public class TestFilter extends AbstractOpenemsAppWithProps<TestFilter, Property, TestPermissionsParameter>
		implements OpenemsApp {

	public record TestPermissionsParameter(//
			ResourceBundle bundle //
	) implements BundleProvider {

	}

	public static enum Property implements Type<Property, TestFilter, TestPermissionsParameter> {
		ID(AppDef.componentId("id0")), //
		COMPONENT_FACTORY_ID(AppDef.copyOfGeneric(defaultDef()));//

		private final AppDef<? super TestFilter, ? super Property, ? super TestPermissionsParameter> def;

		private Property(AppDef<? super TestFilter, ? super Property, ? super TestPermissionsParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TestFilter, TestPermissionsParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestFilter, ? super Property, ? super TestPermissionsParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestFilter>, TestPermissionsParameter> getParamter() {
			return t -> {
				return new TestPermissionsParameter(//
						createResourceBundle(t.language) //
				);
			};
		}

	}

	@Activate
	public TestFilter(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var id = this.getId(t, p, Property.ID);
			final var factoryId = this.getString(p, Property.COMPONENT_FACTORY_ID);
			final var components = new ArrayList<EdgeConfig.Component>();
			components.add(new EdgeConfig.Component(id, "alias", factoryId, //
					new JsonObject()));
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.TEST };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected TestFilter getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
