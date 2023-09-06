package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.io.test.DummyInputOutput;

public class ComponentUtilImplTest {

	private DummyConfigurationAdmin cm;
	private DummyPseudoComponentManager componentManager;
	private ComponentUtil componentUtil;

	@Before
	public void before() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.cm = new DummyConfigurationAdmin();
		this.componentUtil = new ComponentUtilImpl(this.componentManager, this.cm);

		this.createTestRelay("io0");
	}

	@Test
	public void testGetAllRelayInfos() {
		assertEquals(1, this.componentUtil.getAllRelayInfos().size());
		assertEquals(10, this.componentUtil.getAllRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetAllRelayInfosWithExistingComponent() {
		this.createTestComponent("io0/InputOutput1");
		assertEquals(1, this.componentUtil.getAllRelayInfos().size());
		assertEquals(10, this.componentUtil.getAllRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetAvailableRelayInfos() {
		assertEquals(1, this.componentUtil.getAvailableRelayInfos().size());
		assertEquals(10, this.componentUtil.getAvailableRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetAvailableRelayInfosWithExistingComponent() {
		this.createTestComponent("io0/InputOutput1");
		assertEquals(1, this.componentUtil.getAvailableRelayInfos().size());
		assertEquals(9, this.componentUtil.getAvailableRelayInfos().get(0).channels().size());
	}

	@Test
	public void testGetPreferredRelays() {
		final var result = this.componentUtil.getPreferredRelays(2, new PreferredRelay(10, new int[] { 2, 3 }));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput1", result[0]);
		assertEquals("io0/InputOutput2", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithExistingComponent() {
		this.createTestRelay("io1");
		this.createTestComponent("io0/InputOutput2", "io0/InputOutput3");
		final var result = this.componentUtil.getPreferredRelays(2, new PreferredRelay(10, new int[] { 2, 3 }));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io1/InputOutput1", result[0]);
		assertEquals("io1/InputOutput2", result[1]);
	}

	private void createTestComponent(String... blockingRelayContacts) {
		this.componentManager.addComponent(new EdgeConfig.Component("dummyId0", "dummyAlias", "dummy.factory.id", //
				JsonUtils.buildJsonObject() //
						.onlyIf(blockingRelayContacts.length != 0, b -> {
							var cnt = 0;
							for (final var relayContact : blockingRelayContacts) {
								b.addProperty("someRelayConfig" + (cnt++), relayContact);
							}
						}).build()));
	}

	private void createTestRelay(String ioName) {
		final var dummyRelay = new DummyInputOutput(ioName);
		this.cm.getOrCreateEmptyConfiguration(ioName);
		this.componentManager.addComponent(dummyRelay);
	}
}
