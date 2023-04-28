package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.DependencyUtil;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Request;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.CheckRelayCount;
import io.openems.edge.core.appmanager.validator.Checkable;
import io.openems.edge.core.appmanager.validator.Validator;

public class AppManagerTestBundle {

	public final DummyConfigurationAdmin cm;
	public final ComponentManager componentManger;
	public final ComponentUtil componentUtil;
	public final Validator validator;

	public final AppManagerImpl sut;
	public final AppManagerUtil appManagerUtil;
	public final AppCenterBackendUtil appCenterBackendUtil;

	public final CheckablesBundle checkablesBundle;

	public AppManagerTestBundle(JsonObject initialComponentConfig, MyConfig initialAppManagerConfig,
			Function<AppManagerTestBundle, List<OpenemsApp>> availableAppsSupplier) throws Exception {
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

		this.componentUtil = new ComponentUtilImpl(this.componentManger, this.cm);

		this.sut = new AppManagerImpl() {

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
			public CompletableFuture<AddAppInstance.Response> handleAddAppInstanceRequest(User user, Request request,
					boolean ignoreBackend) throws OpenemsNamedException {
				final var response = super.handleAddAppInstanceRequest(user, request, ignoreBackend);
				this.modifyWithCurrentConfig();
				return response;
			}

			@Override
			public CompletableFuture<? extends JsonrpcResponseSuccess> handleDeleteAppInstanceRequest(User user,
					DeleteAppInstance.Request request) throws OpenemsNamedException {
				final var response = super.handleDeleteAppInstanceRequest(user, request);
				this.modifyWithCurrentConfig();
				return response;
			}

			@Override
			public CompletableFuture<UpdateAppInstance.Response> handleUpdateAppInstanceRequest(User user,
					UpdateAppInstance.Request request) throws OpenemsNamedException {
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
				DummyComponentContext context;
				try {
					context = DummyComponentContext.from(config);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new OpenemsException(e);
				}
				this.modified(context, config);
			}

		};
		componentManagerFactory.afterInit(this.sut, this.cm);
		this.appManagerUtil = new AppManagerUtilImpl(this.componentManger);
		this.appCenterBackendUtil = new DummyAppCenterBackendUtil();

		ReflectionUtils.setAttribute(this.appManagerUtil.getClass(), this.appManagerUtil, "appManager", this.sut);

		this.checkablesBundle = new CheckablesBundle(
				new CheckCardinality(this.sut, this.appManagerUtil,
						getComponentContext(CheckCardinality.COMPONENT_NAME)), //
				new CheckRelayCount(this.componentUtil, getComponentContext(CheckRelayCount.COMPONENT_NAME), null) //
		);

		var dummyValidator = new DummyValidator();
		dummyValidator.setCheckables(this.checkablesBundle.all());
		this.validator = dummyValidator;

		final var csoAppManagerAppHelper = DummyAppManagerAppHelper.cso(this.componentManger, this.componentUtil,
				this.validator, this.appManagerUtil);

		final var appManagerAppHelper = csoAppManagerAppHelper.getService();

		// use this so the appManagerAppHelper does not has to be a OpenemsComponent and
		// the attribute can still be private
		ReflectionUtils.setAttribute(appManagerAppHelper.getClass(), appManagerAppHelper, "appManager", this.sut);
		ReflectionUtils.setAttribute(appManagerAppHelper.getClass(), appManagerAppHelper, "appManagerUtil",
				this.appManagerUtil);

		ReflectionUtils.setAttribute(DependencyUtil.class, null, "appHelper", appManagerAppHelper);

		new ComponentTest(this.sut) //
				.addReference("cm", this.cm) //
				.addReference("componentManager", this.componentManger) //
				.addReference("csoAppManagerAppHelper", csoAppManagerAppHelper) //
				.addReference("validator", this.validator) //
				.addReference("backendUtil", this.appCenterBackendUtil) //
				.addReference("availableApps", availableAppsSupplier.apply(this)) //
				.activate(initialAppManagerConfig);
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
		var worker = new AppValidateWorker(this.sut);
		worker.validateApps();

		// should not have found defective Apps
		if (!worker.defectiveApps.isEmpty()) {
			throw new Exception(worker.defectiveApps.entrySet().stream() //
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
		return JsonUtils.parse(((JsonPrimitive) config.getProperties().get("apps")).getAsString()).getAsJsonArray();
	}

	/**
	 * Checkst if the current installed app count matches the given count.
	 * 
	 * @param count the count of the apps
	 */
	public void assertInstalledApps(int count) {
		assertEquals(count, this.sut.getInstantiatedApps().size());
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

	public static class CheckablesBundle {

		public final CheckCardinality checkCardinality;
		public final CheckRelayCount checkRelayCount;

		public CheckablesBundle(CheckCardinality checkCardinality, CheckRelayCount checkRelayCount) {
			this.checkCardinality = checkCardinality;
			this.checkRelayCount = checkRelayCount;
		}

		/**
		 * Gets all {@link Checkable}.
		 * 
		 * @return the {@link Checkable}
		 */
		public final List<Checkable> all() {
			return Lists.newArrayList(this.checkCardinality, //
					this.checkRelayCount //
			);
		}
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

}
