package io.openems.edge.app.evcs;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.CreateComponentConfigRequest;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.MyConfig;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.ResolveDependencies;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.DeleteAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestEvcsCluster {

	private AppManagerTestBundle appManagerTestBundle;

	private EvcsCluster cluster;
	private HardyBarthEvcs hardyBarthEvcs;
	private KebaEvcs kebaEvcs;
	private IesKeywattEvcs keywattEvcs;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.cluster = Apps.evcsCluster(t), //
					this.hardyBarthEvcs = Apps.hardyBarthEvcs(t), //
					this.keywattEvcs = Apps.iesKeywattEvcs(t), //
					this.kebaEvcs = Apps.kebaEvcs(t) //
			);
		}, null, new PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
		this.appManagerTestBundle.addStaticIpAggregateTask();
	}

	@Test
	public void testBasicInstallationAfterTwoKebas() throws Exception {
		this.installKeba("1.1.1.1");
		this.appManagerTestBundle.assertInstalledApps(1);

		this.installKeba("1.1.1.2");
		this.appManagerTestBundle.assertInstalledApps(3);

		assertIdsGotAdded("evcs0", "evcs1");

		this.installKeba("1.1.1.3");

		assertIdsGotAdded("evcs0", "evcs1", "evcs2");
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testAfterHardyBarthDouble() throws Exception {
		this.installHardyBarthDouble("2.1.1.1", "2.1.1.2");

		this.appManagerTestBundle.assertInstalledApps(2);

		assertIdsGotAdded("evcs0", "evcs1");
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testReinstallationWhenMoreOrEqualOfTwoEvcsExist() throws Exception {
		final var apps = JsonUtils.buildJsonArray() //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("appId", "App.Evcs.Keba") //
						.addProperty("alias", "alias1") //
						.addProperty("instanceId", UUID.randomUUID().toString()) //
						.add("properties", JsonUtils.buildJsonObject() //
								.addProperty(KebaEvcs.Property.EVCS_ID.name(), "evcs0") //
								.addProperty(KebaEvcs.Property.CTRL_EVCS_ID.name(), "ctrlEvcs0") //
								.addProperty(KebaEvcs.Property.IP.name(), "1.1.1.1") //
								.build()) //
						.build()) //
				.add(JsonUtils.buildJsonObject() //
						.addProperty("appId", "App.Evcs.Keba") //
						.addProperty("alias", "alias2") //
						.addProperty("instanceId", UUID.randomUUID().toString()) //
						.add("properties", JsonUtils.buildJsonObject() //
								.addProperty(KebaEvcs.Property.EVCS_ID.name(), "evcs1") //
								.addProperty(KebaEvcs.Property.CTRL_EVCS_ID.name(), "ctrlEvcs1") //
								.addProperty(KebaEvcs.Property.IP.name(), "1.1.1.2") //
								.build()) //
						.build())
				.build().toString();

		this.appManagerTestBundle.modified(MyConfig.create() //
				.setKey("0000-0000-0000-0000") //
				.setApps(apps) //
				.build());

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Evcs.Keba.KeContact", Lists.newArrayList(//
						new UpdateComponentConfigRequest.Property("id", "evcs0"), //
						new UpdateComponentConfigRequest.Property("ip", "1.1.1.1") //
				))).get();
		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Controller.Evcs", Lists.newArrayList(//
						new UpdateComponentConfigRequest.Property("id", "ctrlEvcs0"), //
						new UpdateComponentConfigRequest.Property("evcs.id", "evcs0") //
				))).get();
		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Evcs.Keba.KeContact", Lists.newArrayList(//
						new UpdateComponentConfigRequest.Property("id", "evcs1"), //
						new UpdateComponentConfigRequest.Property("ip", "1.1.1.2") //
				))).get();
		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Controller.Evcs", Lists.newArrayList(//
						new UpdateComponentConfigRequest.Property("id", "ctrlEvcs1"), //
						new UpdateComponentConfigRequest.Property("evcs.id", "evcs1") //
				))).get();

		ResolveDependencies.resolveDependencies(DUMMY_ADMIN, this.appManagerTestBundle.sut,
				this.appManagerTestBundle.appManagerUtil);

		this.appManagerTestBundle.assertInstalledApps(3);

		this.assertIdsGotAdded("evcs0", "evcs1");
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testAddingEvcsAfterDependencyWasCreated() throws Exception {
		this.installKeba("1.1.1.1");
		this.appManagerTestBundle.assertInstalledApps(1);

		this.installHardyBarthDouble("2.1.1.1", "2.1.1.2");
		this.appManagerTestBundle.assertInstalledApps(3);

		this.assertIdsGotAdded("evcs0", "evcs1", "evcs2");

		this.installIesKeywatt();
		this.appManagerTestBundle.assertInstalledApps(4);

		this.assertIdsGotAdded("evcs0", "evcs1", "evcs2", "evcs3");
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testClusterWasAlreadyExisting() throws Exception {
		this.installKeba("1.1.1.1");

		final var clusterId = "evcsCluster0";
		this.appManagerTestBundle.componentManger.handleJsonrpcRequest(DUMMY_ADMIN,
				new CreateComponentConfigRequest("Evcs.Cluster.PeakShaving", Lists.newArrayList(//
						new UpdateComponentConfigRequest.Property("id", clusterId), //
						new UpdateComponentConfigRequest.Property("enabled", false), //
						new UpdateComponentConfigRequest.Property("evcs.ids", JsonUtils.buildJsonArray() //
								.add("evcs0") //
								.build()), //
						new UpdateComponentConfigRequest.Property("hardwarePowerLimitPerPhase", 1234) //
				))).get();

		this.assertIdsGotAdded("evcs0");

		this.installKeba("1.1.1.2");

		this.assertSingleClusterApp();
		this.assertIdsGotAdded("evcs0", "evcs1");

		assertEquals(1, this.appManagerTestBundle.componentUtil.getEnabledComponentsOfStartingId("evcsCluster").size());
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testModifyDoubleToSingle() throws Exception {
		this.installKeba("1.1.1.1");
		final var response = this.installHardyBarthDouble("2.1.1.1", "2.1.1.2");

		this.assertIdsGotAdded("evcs0", "evcs1", "evcs2");

		this.appManagerTestBundle.assertInstalledApps(3);

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN,
				new UpdateAppInstance.Request(response.instance.instanceId, "alias", JsonUtils.buildJsonObject() //
						.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), 1) //
						.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), "2.1.1.1") //
						.build()))
				.get();

		final var evcsId = response.instance.properties.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString();

		this.assertIdsGotAdded("evcs0", evcsId);
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void setMaxHardwarePower() throws Exception {
		this.installKeba("1.1.1.1");
		final var hardwarePower = 9999;
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.kebaEvcs.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty(KebaEvcs.Property.IP.name(), "1.1.1.2") //
								.addProperty(KebaEvcs.Property.MAX_HARDWARE_POWER.name(), hardwarePower) //
								.build()))
				.get();
		final var clusterComponent = this.appManagerTestBundle.componentManger.getComponent("evcsCluster0");
		final var hardwarePowerPerPhase = (int) clusterComponent.getComponentContext().getProperties()
				.get("hardwarePowerLimitPerPhase");

		assertEquals(hardwarePower / 3, hardwarePowerPerPhase);

		this.installKeba("1.1.1.3");

		// not modified
		assertEquals(hardwarePower / 3, hardwarePowerPerPhase);
		this.assertClusterHasOnlyValidProps();
	}

	@Test
	public void testRemoveClusterWhenEvcsBelowTwo() throws Exception {
		this.installKeba("1.1.1.1");
		final var responseSecondEvcs = this.installKeba("1.1.1.2");

		this.appManagerTestBundle.assertInstalledApps(3);
		this.assertSingleClusterApp();

		this.appManagerTestBundle.sut.handleDeleteAppInstanceRequest(DUMMY_ADMIN,
				new DeleteAppInstance.Request(responseSecondEvcs.instance.instanceId)).get();

		this.appManagerTestBundle.assertInstalledApps(1);
	}

	private void assertClusterHasOnlyValidProps() {
		final var cluster = this.getClusterApps().get(0);
		assertFalse(cluster.properties.has(EvcsCluster.Property.EVCS_IDS.name()));
		assertFalse(cluster.properties.has(EvcsCluster.Property.MAX_HARDWARE_POWER_LIMIT_PER_PHASE.name()));
	}

	private List<OpenemsAppInstance> getClusterApps() {
		return this.appManagerTestBundle.sut.getInstantiatedApps().stream() //
				.filter(t -> t.appId.equals(this.cluster.getAppId())) //
				.collect(Collectors.toList());
	}

	private void assertSingleClusterApp() {
		assertEquals(1, this.getClusterApps().size());
	}

	private void assertIdsGotAdded(String... ids) throws OpenemsNamedException {
		final var cluster = this.appManagerTestBundle.componentManger.getPossiblyDisabledComponent("evcsCluster0");
		final var props = cluster.getComponentContext().getProperties();
		final var evcsIds = (String[]) props.get("evcs.ids");

		final var expectedIds = Arrays.stream(ids).collect(Collectors.toList());
		final var message = "actual: " + Arrays.stream(evcsIds).collect(Collectors.joining(", ")) //
				+ ", expected: " + expectedIds.stream().collect(Collectors.joining(", "));
		assertEquals(message, expectedIds.size(), evcsIds.length);
		assertTrue(message, //
				Arrays.stream(evcsIds) //
						.collect(Collectors.toList()) //
						.containsAll(expectedIds));
	}

	private AddAppInstance.Response installKeba(String ip)
			throws InterruptedException, ExecutionException, OpenemsNamedException {
		var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.kebaEvcs.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty(KebaEvcs.Property.IP.name(), ip) //
								.build()))
				.get();

		final var evcsId = response.instance.properties.get(KebaEvcs.Property.EVCS_ID.name()).getAsString();
		final var evcsCtrlId = response.instance.properties.get(KebaEvcs.Property.CTRL_EVCS_ID.name()).getAsString();
		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component(evcsId, null, "Evcs.Keba.KeContact", JsonUtils.buildJsonObject() //
						.addProperty("ip", ip) //
						.build()), //
				new EdgeConfig.Component(evcsCtrlId, null, "Controller.Evcs", JsonUtils.buildJsonObject() //
						.addProperty("evcs.id", evcsId) //
						.build()) //
		);
		return response;
	}

	private AddAppInstance.Response installHardyBarthDouble(String... ips)
			throws InterruptedException, ExecutionException, OpenemsNamedException {
		final var count = ips.length;
		final var fistIp = ips[0];
		final var secondIp = count == 2 ? ips[1] : null;

		var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardyBarthEvcs.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty(HardyBarthEvcs.Property.NUMBER_OF_CHARGING_STATIONS.name(), count) //
								.addProperty(HardyBarthEvcs.SubPropertyFirstChargepoint.IP.name(), fistIp) //
								.onlyIf(count == 2,
										b -> b.addProperty(HardyBarthEvcs.SubPropertySecondChargepoint.IP_CP_2.name(),
												secondIp)) //
								.build()))
				.get();

		final var evcsId = response.instance.properties.get(HardyBarthEvcs.Property.EVCS_ID.name()).getAsString();
		final var evcsCtrlId = response.instance.properties.get(HardyBarthEvcs.Property.CTRL_EVCS_ID.name())
				.getAsString();
		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component(evcsId, null, "Evcs.HardyBarth", JsonUtils.buildJsonObject() //
						.build()), //
				new EdgeConfig.Component(evcsCtrlId, null, "Controller.Evcs", JsonUtils.buildJsonObject() //
						.addProperty("evcs.id", evcsId) //
						.build()) //
		);
		if (count == 2) {
			final var evcsIdCp2 = response.instance.properties.get(HardyBarthEvcs.Property.EVCS_ID_CP_2.name())
					.getAsString();
			final var evcsCtrlIdCp2 = response.instance.properties.get(HardyBarthEvcs.Property.CTRL_EVCS_ID_CP_2.name())
					.getAsString();
			this.appManagerTestBundle.assertComponentsExist(//
					new EdgeConfig.Component(evcsIdCp2, null, "Evcs.HardyBarth", JsonUtils.buildJsonObject() //
							.build()), //
					new EdgeConfig.Component(evcsCtrlIdCp2, null, "Controller.Evcs", JsonUtils.buildJsonObject() //
							.addProperty("evcs.id", evcsIdCp2) //
							.build()) //
			);
		}
		return response;
	}

	private AddAppInstance.Response installIesKeywatt()
			throws InterruptedException, ExecutionException, OpenemsNamedException {
		var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.keywattEvcs.getAppId(), "key", "alias", //
						JsonUtils.buildJsonObject() //
								.addProperty(IesKeywattEvcs.Property.OCCP_CHARGE_POINT_IDENTIFIER.name(), "IES1") //
								.addProperty(IesKeywattEvcs.Property.OCCP_CONNECTOR_IDENTIFIER.name(), 1) //
								.build()))
				.get();

		final var evcsId = response.instance.properties.get(IesKeywattEvcs.Property.EVCS_ID.name()).getAsString();
		final var evcsCtrlId = response.instance.properties.get(IesKeywattEvcs.Property.CTRL_EVCS_ID.name())
				.getAsString();
		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component(evcsId, null, "Evcs.Ocpp.IesKeywattSingle", JsonUtils.buildJsonObject() //
						.build()), //
				new EdgeConfig.Component(evcsCtrlId, null, "Controller.Evcs", JsonUtils.buildJsonObject() //
						.addProperty("evcs.id", evcsId) //
						.build()) //
		);
		return response;
	}

}
