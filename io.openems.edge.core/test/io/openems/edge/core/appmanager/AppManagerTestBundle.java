package io.openems.edge.core.appmanager;

import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;
import static io.openems.common.utils.ReflectionUtils.setStaticAttributeViaReflection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.DummyHost;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.DummyValidator.TestCheckable;
import io.openems.edge.core.appmanager.dependency.AppConfigValidator;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelper;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PersistencePredictorAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.PersistencePredictorAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.StaticIpAggregateTask;
import io.openems.edge.core.appmanager.dependency.aggregatetask.StaticIpAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.aggregatetask.TestScheduler;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.CheckOr;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.CheckableFactory;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.ValidatorImpl;

public class AppManagerTestBundle {

	public final DummyConfigurationAdmin cm;
	public final ComponentManager componentManger;
	public final ComponentUtil componentUtil;
	public final Validator validator;
	public final DummyHost host = new DummyHost();

	public final DummyAppManagerAppHelper appHelper;
	public final AppManagerImpl sut;
	public final AppManagerUtil appManagerUtil;
	public final AppCenterBackendUtil appCenterBackendUtil;

	private final AppValidateWorker appValidateWorker;

	public final TestScheduler scheduler;

	private final CheckableFactory checkableFactory = new CheckableFactory();
	private final CheckCardinality checkCardinality;

	public AppManagerTestBundle(//
			JsonObject initialComponentConfig, //
			MyConfig initialAppManagerConfig, //
			Function<AppManagerTestBundle, List<OpenemsApp>> availableAppsSupplier //
	) throws Exception {
		this(initialComponentConfig, initialAppManagerConfig, availableAppsSupplier, null,
				new DefaultComponentManagerFactory());
	}

	public <T extends ComponentManager> AppManagerTestBundle(//
			JsonObject initialComponentConfig, //
			MyConfig initialAppManagerConfig, //
			Function<AppManagerTestBundle, List<OpenemsApp>> availableAppsSupplier, //
			Consumer<JsonUtils.JsonObjectBuilder> additionalComponentConfig, //
			ComponentManagerFactory<T> componentManagerFactory //
	) throws Exception {
		this(initialComponentConfig, initialAppManagerConfig, availableAppsSupplier, additionalComponentConfig,
				componentManagerFactory, new AppManagerImplAutoUpdateOnConfigChange());
	}

	public <T extends ComponentManager> AppManagerTestBundle(//
			JsonObject initialComponentConfig, //
			MyConfig initialAppManagerConfig, //
			Function<AppManagerTestBundle, List<OpenemsApp>> availableAppsSupplier, //
			Consumer<JsonUtils.JsonObjectBuilder> additionalComponentConfig, //
			ComponentManagerFactory<T> componentManagerFactory, //
			AppManagerImpl impl //
	) throws Exception {
		if (initialComponentConfig == null) {
			initialComponentConfig = JsonUtils.buildJsonObject() //
					.add("scheduler0", JsonUtils.buildJsonObject() //
							.addProperty("factoryId", "Scheduler.AllAlphabetically") //
							.add("properties", JsonUtils.buildJsonObject() //
									.addProperty("enabled", true) //
									.add("controllers.ids", JsonUtils.buildJsonArray() //
											.build()) //
									.build()) //
							.build()) //
					.add(Host.SINGLETON_COMPONENT_ID, JsonUtils.buildJsonObject() //
							.addProperty("factoryId", Host.SINGLETON_SERVICE_PID) //
							.addProperty("alias", "") //
							.add("properties", JsonUtils.buildJsonObject() //
									.addProperty("networkConfiguration", //
											"{\n" //
													+ "  \"interfaces\": {\n" //
													+ "    \"enx*\": {\n" //
													+ "      \"dhcp\": false,\n" //
													+ "      \"addresses\": [\n" //
													+ "        \"10.4.0.1/16\",\n" //
													+ "        \"192.168.1.9/29\"\n" //
													+ "      ]\n" //
													+ "    },\n" //
													+ "    \"eth0\": {\n" //
													+ "      \"dhcp\": true,\n" //
													+ "      \"linkLocalAddressing\": true,\n" //
													+ "      \"addresses\": [\n" //
													+ "        \"192.168.100.100/24\"\n" //
													+ "      ]\n" //
													+ "    }\n" //
													+ "  }\n" //
													+ "}") //
									.build()) //
							.build()) //
					.onlyIf(additionalComponentConfig != null, additionalComponentConfig) //
					.build();
		}

		if (initialAppManagerConfig == null) {
			initialAppManagerConfig = MyConfig.create() //
					.setApps(JsonUtils.buildJsonArray() //
							.build() //
							.toString())
					.setKey("0000-0000-0000-0000") //
					.build();
		}

		this.cm = new DummyConfigurationAdmin();
		this.cm.getOrCreateEmptyConfiguration(AppManager.SINGLETON_SERVICE_PID);

		this.componentManger = componentManagerFactory.createComponentManager(JsonUtils.buildJsonObject() //
				.add("components", initialComponentConfig) //
				.add("factories", JsonUtils.buildJsonObject() //
						.build()) //
				.build());

		// create config for scheduler
		this.cm.getOrCreateEmptyConfiguration(
				this.componentManger.getEdgeConfig().getComponent("scheduler0").get().getPid());

		this.componentUtil = new ComponentUtilImpl(this.componentManger);

		this.sut = impl;

		componentManagerFactory.afterInit(this.sut, this.cm);
		this.appManagerUtil = new AppManagerUtilImpl(this.componentManger);
		this.appCenterBackendUtil = new DummyAppCenterBackendUtil();

		this.addCheckable(TestCheckable.COMPONENT_NAME, t -> new TestCheckable());
		this.addCheckable(CheckOr.COMPONENT_NAME, t -> new CheckOr(t, this.checkableFactory));
		this.checkCardinality = this.addCheckable(CheckCardinality.COMPONENT_NAME,
				t -> new CheckCardinality(this.sut, this.appManagerUtil, t));

		this.validator = new ValidatorImpl(this.checkableFactory);

		this.appHelper = new DummyAppManagerAppHelper(this.componentManger, this.componentUtil, this.appManagerUtil);
		final var csoAppManagerAppHelper = cso((AppManagerAppHelper) this.appHelper);

		this.appValidateWorker = new AppValidateWorker();
		final var appConfigValidator = new AppConfigValidator();

		setAttributeViaReflection(this.appValidateWorker, "appManagerUtil", this.appManagerUtil);
		setAttributeViaReflection(this.appValidateWorker, "validator", appConfigValidator);

		setAttributeViaReflection(appConfigValidator, "appManagerUtil", this.appManagerUtil);
		setAttributeViaReflection(appConfigValidator, "tasks", this.appHelper.getTasks());

		setStaticAttributeViaReflection(DependencyUtil.class, "appHelper", this.appHelper);

		new ComponentTest(this.sut) //
				.addReference("cm", this.cm) //
				.addReference("componentManager", this.componentManger) //
				.addReference("csoAppManagerAppHelper", csoAppManagerAppHelper) //
				.addReference("validator", this.validator) //
				.addReference("appValidateWorker", this.appValidateWorker) //
				.addReference("backendUtil", this.appCenterBackendUtil) //
				.addReference("availableApps", availableAppsSupplier.apply(this)) //
				.activate(initialAppManagerConfig);

		this.scheduler = new TestScheduler(this.componentManger);
	}

	/**
	 * Adds a checkable to the current test bundle.
	 * 
	 * @param <T>              the type of the checkable to add
	 * @param componentName    the component name of the checkable
	 * @param checkableFactory the factory to get a instance of the checkable
	 * @return the created checkable
	 */
	public <T extends Checkable> T addCheckable(//
			final String componentName, //
			final Function<ComponentContext, T> checkableFactory //
	) {
		final var checkable = checkableFactory.apply(getComponentContext(componentName));
		this.checkableFactory.bindCso(cso(componentName, checkable));
		return checkable;
	}

	public CheckCardinality getCheckCardinality() {
		return this.checkCardinality;
	}

	/**
	 * Calls the modified method.
	 * 
	 * @param config the configuration to update
	 * @throws Exception on error
	 */
	public void modified(MyConfig config) throws Exception {
		this.sut.modified(DummyComponentContext.from(config), config);
	}

	/**
	 * Gets the first found instance of the given app.
	 * 
	 * @param appId the instance of which app
	 * @return the instance or null if not found
	 */
	public OpenemsAppInstance findFirst(String appId) {
		return this.sut.getInstantiatedApps().stream() //
				.filter(t -> t.appId.equals(appId)) //
				.findFirst() //
				.orElse(null);
	}

	/**
	 * Checks if the {@link Validator} has any errors.
	 * 
	 * @throws Exception on error
	 */
	public void assertNoValidationErrors() throws Exception {
		this.appValidateWorker.validateApps();

		// should not have found defective Apps
		if (!this.appValidateWorker.defectiveApps.isEmpty()) {
			throw new Exception(this.appValidateWorker.defectiveApps.entrySet().stream() //
					.map(e -> e.getKey() + "[" + e.getValue() + "]") //
					.collect(Collectors.joining("|")));
		}
	}

	/**
	 * Prints out the instantiated {@link OpenemsAppInstance}s.
	 */
	public void printApps() {
		JsonUtils.prettyPrint(this.sut.getInstantiatedApps().stream().map(OpenemsAppInstance::toJsonObject)
				.collect(JsonUtils.toJsonArray()));
	}

	/**
	 * Gets the apps as a {@link JsonArray} from the "apps" property in the
	 * {@link ConfigurationAdmin}.
	 * 
	 * @return the apps
	 * @throws IOException           on IO error
	 * @throws OpenemsNamedException on parse error
	 */
	public JsonArray getAppsFromConfig() throws IOException, OpenemsNamedException {
		final var config = this.cm.getConfiguration(this.sut.servicePid());
		final var configObj = config.getProperties().get("apps");
		if (configObj instanceof JsonPrimitive json) {
			return JsonUtils.getAsJsonArray(JsonUtils.parse(JsonUtils.getAsString(json)));
		}
		return JsonUtils.getAsJsonArray(JsonUtils.parse(configObj.toString()));
	}

	/**
	 * Checks if the current installed app count matches the given count.
	 * 
	 * @param count the count of the apps
	 * @throws OpenemsNamedException on parse error
	 * @throws IOException           on IO error
	 */
	public void assertInstalledApps(int count) throws IOException, OpenemsNamedException {
		assertEquals(count, this.sut.getInstantiatedApps().size());
		assertEquals(count, this.getAppsFromConfig().size());
	}

	/**
	 * Checks if the given component exists.
	 * 
	 * @param component the component that should exist
	 * @throws OpenemsNamedException on error
	 */
	public void assertComponentExist(EdgeConfig.Component component) throws OpenemsNamedException {
		final var foundComponent = this.componentManger.getPossiblyDisabledComponent(component.getId());
		assertEquals(component.getFactoryId(), foundComponent.serviceFactoryPid());
		final var props = foundComponent.getComponentContext().getProperties();
		for (var entry : component.getProperties().entrySet()) {
			final var existingValue = props.get(entry.getKey());
			if (entry.getKey().equalsIgnoreCase("alias")) {
				if (component.getAlias() != null) {
					assertEquals(component.getAlias(), entry.getValue().getAsString());
				}
				continue;
			}
			assertNotNull(existingValue);
			// only string comparison
			assertEquals(entry.getValue().toString().replace("\"", ""), existingValue.toString());
		}
	}

	/**
	 * Checks if all the given components exist.
	 * 
	 * @param components the components that should exist
	 * @throws OpenemsNamedException on error
	 */
	public void assertComponentsExist(EdgeConfig.Component... components) throws OpenemsNamedException {
		for (var component : components) {
			this.assertComponentExist(component);
		}
	}

	/**
	 * Adds a {@link ComponentAggregateTask} to the current active tasks.
	 * 
	 * @return the created {@link ComponentAggregateTask}
	 */
	public ComponentAggregateTask addComponentAggregateTask() {
		final var componentAggregateTask = new ComponentAggregateTaskImpl(this.componentManger);
		this.appHelper.addAggregateTask(componentAggregateTask);
		return componentAggregateTask;
	}

	/**
	 * Adds a {@link SchedulerAggregateTask} to the current active tasks.
	 * 
	 * @param componentAggregateTask the {@link ComponentAggregateTask}
	 * @return the created {@link SchedulerAggregateTask}
	 */
	public SchedulerAggregateTask addSchedulerAggregateTask(ComponentAggregateTask componentAggregateTask) {
		final var schedulerAggregateTaskImpl = new SchedulerAggregateTaskImpl(componentAggregateTask,
				this.componentUtil);
		this.appHelper.addAggregateTask(schedulerAggregateTaskImpl);
		return schedulerAggregateTaskImpl;
	}

	/**
	 * Adds a {@link SchedulerByCentralOrderAggregateTask} to the current active
	 * tasks.
	 * 
	 * @param componentAggregateTask the {@link ComponentAggregateTask}
	 * @return the created {@link SchedulerByCentralOrderAggregateTask}
	 */
	public SchedulerByCentralOrderAggregateTask addSchedulerByCentralOrderAggregateTask(
			ComponentAggregateTask componentAggregateTask) {
		final var schedulerByCentralOrderAggregateTaskImpl = new SchedulerByCentralOrderAggregateTaskImpl(
				this.componentManger, this.componentUtil, this.appManagerUtil, componentAggregateTask,
				new SchedulerByCentralOrderAggregateTaskImpl.ProductionSchedulerOrderDefinition());
		this.appHelper.addAggregateTask(schedulerByCentralOrderAggregateTaskImpl);
		return schedulerByCentralOrderAggregateTaskImpl;
	}

	/**
	 * Adds a {@link StaticIpAggregateTask} to the current active tasks.
	 * 
	 * @return the created {@link StaticIpAggregateTask}
	 */
	public StaticIpAggregateTask addStaticIpAggregateTask() {
		var staticIpAggregateTaskImpl = new StaticIpAggregateTaskImpl(this.componentUtil);
		this.appHelper.addAggregateTask(staticIpAggregateTaskImpl);
		return staticIpAggregateTaskImpl;
	}

	/**
	 * Adds a {@link PersistencePredictorAggregateTask} to the current active tasks.
	 * 
	 * @return the created {@link PersistencePredictorAggregateTask}
	 */
	public PersistencePredictorAggregateTask addPersistencePredictorAggregateTask() {
		final var persistencePredictorAggregateTaskImpl = new PersistencePredictorAggregateTaskImpl(
				this.componentManger);
		this.appHelper.addAggregateTask(persistencePredictorAggregateTaskImpl);
		return persistencePredictorAggregateTaskImpl;
	}

	/**
	 * Gets the {@link ComponentContext} for an {@link OpenemsApp} of the given
	 * appId.
	 * 
	 * @param appId the {@link OpenemsApp#getAppId()} of the {@link OpenemsApp}
	 * @return the {@link ComponentContext}
	 */
	public static ComponentContext getComponentContext(String appId) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_NAME, appId);
		return new DummyComponentContext(properties);
	}

	public static interface ComponentManagerFactory<T extends ComponentManager> {

		/**
		 * Creates the {@link ComponentManager} and initializes it with the
		 * configuration.
		 * 
		 * @param config the initial configuration
		 * @return the {@link ComponentManager}
		 * @throws OpenemsNamedException on error
		 */
		public T createComponentManager(JsonObject config) throws OpenemsNamedException;

		/**
		 * Gets called after the passed components got initialized.
		 * 
		 * @param impl the {@link AppManagerImpl}
		 * @param cm   the {@link ConfigurationAdmin}
		 */
		public void afterInit(AppManagerImpl impl, ConfigurationAdmin cm);

	}

	public static class DefaultComponentManagerFactory implements ComponentManagerFactory<DummyComponentManager> {

		private final DummyComponentManager componentManager;

		public DefaultComponentManagerFactory() {
			super();
			this.componentManager = new DummyComponentManager();
		}

		@Override
		public DummyComponentManager createComponentManager(JsonObject config) {
			this.componentManager.setConfigJson(config);
			return this.componentManager;
		}

		public DummyComponentManager getComponentManager() {
			return this.componentManager;
		}

		@Override
		public void afterInit(AppManagerImpl impl, ConfigurationAdmin cm) {
			this.componentManager.addComponent(impl);
			this.componentManager.setConfigurationAdmin(cm);
		}

	}

	public static class PseudoComponentManagerFactory implements ComponentManagerFactory<DummyPseudoComponentManager> {

		private final DummyPseudoComponentManager componentManager;

		public PseudoComponentManagerFactory() {
			this.componentManager = new DummyPseudoComponentManager();
		}

		@Override
		public DummyPseudoComponentManager createComponentManager(JsonObject config) throws OpenemsNamedException {
			final var components = config.get("components").getAsJsonObject();
			for (var entry : components.entrySet()) {
				final var parsedComponent = EdgeConfig.Component.fromJson(entry.getKey(),
						entry.getValue().getAsJsonObject());
				this.componentManager.addComponent(parsedComponent);
			}

			return this.componentManager;
		}

		public DummyPseudoComponentManager getComponentManager() {
			return this.componentManager;
		}

		@Override
		public void afterInit(AppManagerImpl impl, ConfigurationAdmin cm) {
			this.componentManager.addComponent(impl);
			this.componentManager.setConfigurationAdmin(cm);
		}

	}

	/**
	 * Creates a {@link ComponentServiceObjects} of a service.
	 * 
	 * @param <T>     the type of the service
	 * @param service the service
	 * @return the {@link ComponentServiceObjects}
	 */
	public static <T> ComponentServiceObjects<T> cso(T service) {
		return cso(null, service);
	}

	/**
	 * Creates a {@link ComponentServiceObjects} of a service.
	 * 
	 * @param <T>           the type of the service
	 * @param componentName the name of the component
	 * @param service       the service
	 * @return the {@link ComponentServiceObjects}
	 */
	public static <T> ComponentServiceObjects<T> cso(String componentName, T service) {
		final var sr = new ServiceReference<T>() {

			private final Map<String, Object> properties = ImmutableMap.<String, Object>builder() //
					.put(OpenemsConstants.PROPERTY_OSGI_COMPONENT_NAME,
							componentName != null ? componentName : service.getClass().getCanonicalName()) //
					.build();

			@Override
			public Object getProperty(String key) {
				return this.properties.get(key);
			}

			@Override
			public String[] getPropertyKeys() {
				return this.properties.keySet().toArray(String[]::new);
			}

			@Override
			public Bundle getBundle() {
				return null;
			}

			@Override
			public Bundle[] getUsingBundles() {
				return null;
			}

			@Override
			public boolean isAssignableTo(Bundle bundle, String className) {
				return false;
			}

			@Override
			public int compareTo(Object reference) {
				return 0;
			}

			@Override
			public Dictionary<String, Object> getProperties() {
				return null;
			}

			@Override
			public <A> A adapt(Class<A> type) {
				return null;
			}
		};
		return new ComponentServiceObjects<T>() {

			@Override
			public T getService() {
				return service;
			}

			@Override
			public void ungetService(T service) {
				// empty for test
			}

			@Override
			public ServiceReference<T> getServiceReference() {
				return sr;
			}

		};
	}

	/**
	 * This implementation is used to automatically update the call the modified
	 * method when a changes happens thru a app change.
	 */
	private static class AppManagerImplAutoUpdateOnConfigChange extends AppManagerImpl {

		/**
		 * activate, modified, deactivate need to be overwritten because of reflection
		 * usage in tests.
		 */

		@Activate
		@Override
		protected void activate(ComponentContext componentContext, Config config) {
			super.activate(componentContext, config);
		}

		@Modified
		@Override
		protected void modified(ComponentContext componentContext, Config config) throws OpenemsNamedException {
			super.modified(componentContext, config);
		}

		@Deactivate
		@Override
		protected void deactivate() {
			super.deactivate();
		}

		@Override
		public AddAppInstance.Response handleAddAppInstanceRequest(User user, Request request, boolean ignoreBackend)
				throws OpenemsNamedException {
			final var response = super.handleAddAppInstanceRequest(user, request, ignoreBackend);
			this.modifyWithCurrentConfig();
			return response;
		}

		@Override
		public DeleteAppInstance.Response handleDeleteAppInstanceRequest(User user, DeleteAppInstance.Request request)
				throws OpenemsNamedException {
			final var response = super.handleDeleteAppInstanceRequest(user, request);
			this.modifyWithCurrentConfig();
			return response;
		}

		@Override
		public UpdateAppInstance.Response handleUpdateAppInstanceRequest(User user, UpdateAppInstance.Request request)
				throws OpenemsNamedException {
			final var response = super.handleUpdateAppInstanceRequest(user, request);
			this.modifyWithCurrentConfig();
			return response;
		}

		private final void modifyWithCurrentConfig() throws OpenemsNamedException {
			final var config = MyConfig.create() //
					.setApps(this.instantiatedApps.stream() //
							.map(OpenemsAppInstance::toJsonObject) //
							.collect(JsonUtils.toJsonArray()) //
							.toString())
					.setKey("0000-0000-0000-0000") //
					.build();
			try {
				this.modified(DummyComponentContext.from(config), config);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new OpenemsException(e);
			}
		}
	}

}
