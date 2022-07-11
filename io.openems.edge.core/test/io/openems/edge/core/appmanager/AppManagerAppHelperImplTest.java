package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.ReflectionUtils;
import io.openems.edge.app.TestADependencyToC;
import io.openems.edge.app.TestBDependencyToC;
import io.openems.edge.app.TestC;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.dependency.AppManagerAppHelperImpl;
import io.openems.edge.core.appmanager.dependency.ComponentAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.SchedulerAggregateTaskImpl;
import io.openems.edge.core.appmanager.dependency.StaticIpAggregateTaskImpl;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Validator;

public class AppManagerAppHelperImplTest {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private DummyConfigurationAdmin cm;
	private DummyComponentManager componentManger;
	private ComponentUtil componentUtil;
	private Validator validator;

	private FeneconHome homeApp;
	private KebaEvcs kebaEvcsApp;
	private AwattarHourly awattarApp;
	private StromdaoCorrently stromdao;

	private TestADependencyToC testAApp;
	private TestBDependencyToC testBApp;
	private TestC testCApp;

	private AppManagerImpl sut;

	@Before
	public void beforeEach() throws Exception {

		this.cm = new DummyConfigurationAdmin();
		this.cm.getOrCreateEmptyConfiguration(AppManager.SINGLETON_SERVICE_PID);

		this.componentManger = new DummyComponentManager();
		this.componentManger.setConfigJson(JsonUtils.buildJsonObject() //
				.add("components", JsonUtils.buildJsonObject() //
						.add("scheduler0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Scheduler.AllAlphabetically") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.add("controllers.ids", JsonUtils.buildJsonArray() //
												.add("ctrlGridOptimizedCharge0") //
												.add("ctrlEssSurplusFeedToGrid0") //
												.add("ctrlBalancing0") //
												.build()) //
										.build()) //
								.build()) //
						.build()) //
				.add("factories", JsonUtils.buildJsonObject() //
						.build()) //
				.build() //
		);
		this.componentUtil = new ComponentUtilImpl(this.componentManger, this.cm);

		this.homeApp = new FeneconHome(this.componentManger, getComponentContext("App.FENECON.Home"), this.cm,
				this.componentUtil);
		this.kebaEvcsApp = new KebaEvcs(this.componentManger, getComponentContext("App.Evcs.Keba"), this.cm,
				this.componentUtil);
		this.awattarApp = new AwattarHourly(this.componentManger, getComponentContext("App.TimeVariablePrice.Awattar"),
				this.cm, this.componentUtil);
		this.stromdao = new StromdaoCorrently(this.componentManger,
				getComponentContext("App.TimeVariablePrice.Stromdao"), this.cm, this.componentUtil);

		this.testAApp = new TestADependencyToC(this.componentManger, getComponentContext("App.Test.TestADependencyToC"),
				this.cm, this.componentUtil);

		this.testBApp = new TestBDependencyToC(this.componentManger, getComponentContext("App.Test.TestBDependencyToC"),
				this.cm, this.componentUtil);

		this.testCApp = new TestC(this.componentManger, getComponentContext("App.Test.TestC"), this.cm,
				this.componentUtil);

		final var componentTask = new ComponentAggregateTaskImpl(this.componentManger);
		final var schedulerTask = new SchedulerAggregateTaskImpl(componentTask, this.componentUtil);
		final var staticIpTask = new StaticIpAggregateTaskImpl(this.componentUtil);

		this.sut = new AppManagerImpl();
		this.componentManger.addComponent(this.sut);
		this.componentManger.setConfigurationAdmin(this.cm);

		var dummyValidator = new DummyValidator();
		dummyValidator.setCheckables(Lists
				.newArrayList(new CheckCardinality(this.sut, getComponentContext(CheckCardinality.COMPONENT_NAME))));
		this.validator = dummyValidator;

		var appManagerAppHelper = new AppManagerAppHelperImpl(this.componentManger, this.componentUtil, this.validator,
				componentTask, schedulerTask, staticIpTask);

		// use this so the appManagerAppHelper does not has to be a OpenemsComponent and
		// the attribute can still be private
		ReflectionUtils.setAttribute(appManagerAppHelper.getClass(), appManagerAppHelper, "appManager", this.sut);

		new ComponentTest(this.sut) //
				.addReference("cm", this.cm) //
				.addReference("componentManager", this.componentManger) //
				.addReference("appHelper", appManagerAppHelper) //
				.addReference("validator", this.validator) //
				.addReference("availableApps",
						ImmutableList.of(this.homeApp, this.kebaEvcsApp, this.awattarApp, this.stromdao, this.testAApp,
								this.testBApp, this.testCApp)) //
				.activate(MyConfig.create() //
						.setApps(JsonUtils.buildJsonArray() //
								.build().toString()) //
						.build());

	}

	@Test
	public void testCreatePolicyIfNotExisting() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING.name())
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING.name())
						.build()));

		assertEquals(3, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testCreatePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.ALWAYS.name()).build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.ALWAYS.name()).build()));

		assertEquals(4, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testCreatePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.NEVER.name()).build()));

		assertEquals(1, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("CREATE_POLICY", DependencyDeclaration.CreatePolicy.NEVER.name()).build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testUpdatePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.NEVER.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.NEVER.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(1, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testUpdatePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.ALWAYS.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.sut.getInstantiatedApps().size());

		this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.ALWAYS.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testUpdatePolicyIfMine() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
								.addProperty("NUMBER", 2) //
								.build()));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.sut.getInstantiatedApps().size());

		this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testAApp.getAppId()).instanceId, "",
						JsonUtils.buildJsonObject() //
								.addProperty("UPDATE_POLICY", DependencyDeclaration.UpdatePolicy.IF_MINE.name()) //
								.addProperty("NUMBER", 3) //
								.build()));

		instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(2, instance.properties.get(TestC.Property.NUMBER.name()).getAsInt());
	}

	@Test
	public void testDeletePolicyNever() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.NEVER.name()) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.sut.handleDeleteAppInstanceRequest(this.user, new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDeletePolicyAlways() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.ALWAYS.name()) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.sut.handleDeleteAppInstanceRequest(this.user, new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDeletePolicyIfMine() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DELETE_POLICY", DependencyDeclaration.DeletePolicy.IF_MINE.name()) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testBApp.getAppId(), "", //
				JsonUtils.buildJsonObject().build()));

		assertEquals(3, this.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testAApp.getAppId());
		this.sut.handleDeleteAppInstanceRequest(this.user, new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(2, this.sut.getInstantiatedApps().size());
	}

	@Test
	public void testDependencyDeletePolicyAllowed() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_DELETE_POLICY",
								DependencyDeclaration.DependencyDeletePolicy.ALLOWED.name()) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		this.sut.handleDeleteAppInstanceRequest(this.user, new DeleteAppInstance.Request(instance.instanceId));

		assertEquals(1, this.sut.getInstantiatedApps().size());
	}

	@Test(expected = OpenemsNamedException.class)
	public void testDependencyDeletePolicyNotAllowed() throws OpenemsNamedException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_DELETE_POLICY",
								DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED.name()) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		this.sut.handleDeleteAppInstanceRequest(this.user, new DeleteAppInstance.Request(instance.instanceId));
	}

	@Test
	public void testDependencyUpdatePolicyAllowAll()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_UPDATE_POLICY",
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		var completable = this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));

		var result = completable.get().getResult();
		assertTrue(!result.has("warnings") || (result.get("warnings").getAsJsonArray().size() == 0));

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(newAlias, instance.alias);
		assertEquals(2, instance.properties.get("NUMBER").getAsInt());
	}

	@Test(expected = OpenemsNamedException.class)
	public void testDependencyUpdatePolicyAllowNone()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_UPDATE_POLICY",
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_NONE.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));
	}

	@Test
	public void testDependencyUpdatePolicyAllowOnlyUnconfiguredProperties()
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		assertEquals(0, this.sut.getInstantiatedApps().size());

		this.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(this.testAApp.getAppId(), "", //
				JsonUtils.buildJsonObject() //
						.addProperty("DEPENDENCY_UPDATE_POLICY",
								DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES.name()) //
						.addProperty("NUMBER", 1) //
						.build()));

		assertEquals(2, this.sut.getInstantiatedApps().size());

		var newAlias = "newAppAlias";
		var completable = this.sut.handleJsonrpcRequest(this.user,
				new UpdateAppInstance.Request(this.getAppByAppId(this.testCApp.getAppId()).instanceId, newAlias,
						JsonUtils.buildJsonObject() //
								.addProperty("NUMBER", 2) //
								.build()));

		var result = completable.get().getResult();
		assertTrue(result.has("warnings"));
		assertTrue(result.get("warnings").getAsJsonArray().size() > 0);

		var instance = this.getAppByAppId(this.testCApp.getAppId());
		assertEquals(newAlias, instance.alias);
		assertEquals(1, instance.properties.get("NUMBER").getAsInt());

	}

	private OpenemsAppInstance getAppByAppId(String appId) {
		return this.sut.getInstantiatedApps().stream().filter(i -> i.appId.equals(appId)).findAny().get();
	}

	private static ComponentContext getComponentContext(String appId) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_NAME, appId);
		return new DummyComponentContext(properties);
	}

}
