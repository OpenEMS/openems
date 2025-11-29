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
import io.openems.edge.app.TestMapPropName.Property;
import io.openems.edge.app.TestMapPropName.TestPermissionsParameter;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
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
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;

/**
 * Tests AppPropertyPermissions.
 */
@Component(name = "App.Test.TestMapPropName")
public class TestMapPropName extends AbstractOpenemsAppWithProps<TestMapPropName, Property, TestPermissionsParameter>
		implements OpenemsApp {

	public record TestPermissionsParameter(//
			ResourceBundle bundle //
	) implements BundleProvider {

	}

	public static enum Property implements Type<Property, TestMapPropName, TestPermissionsParameter> {
		ID(AppDef.componentId("id0")), //
		NOT_BIDIRECTIONAL(AppDef.copyOfGeneric(CommonProps.defaultDef())),
		BIDIRECTIONAL(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.bidirectional("test", "testProperty", ComponentManagerSupplier::getComponentManager))),
		BIDIRECTIONAL_SAME_NAME(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.bidirectional("test", "bidirectionalSameName", ComponentManagerSupplier::getComponentManager))), //
		;//

		private final AppDef<? super TestMapPropName, ? super Property, ? super TestPermissionsParameter> def;

		private Property(AppDef<? super TestMapPropName, ? super Property, ? super TestPermissionsParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TestMapPropName, TestPermissionsParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestMapPropName, ? super Property, ? super TestPermissionsParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestMapPropName>, TestPermissionsParameter> getParamter() {
			return t -> {
				return new TestPermissionsParameter(//
						createResourceBundle(t.language) //
				);
			};
		}

	}

	@Activate
	public TestMapPropName(//
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
			return AppConfiguration.create() //
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
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected TestMapPropName getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
