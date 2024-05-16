package io.openems.edge.core.appmanager;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ComponentUtil.RelayContactInfo;
import io.openems.edge.core.appmanager.ComponentUtil.RelayInfo;
import io.openems.edge.io.test.DummyInputOutput;

public class ComponentUtilImplTest {

	private DummyConfigurationAdmin cm;
	private DummyPseudoComponentManager componentManager;
	private ComponentUtil componentUtil;

	@Before
	public void before() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.cm = new DummyConfigurationAdmin();
		this.componentUtil = new ComponentUtilImpl(this.componentManager);

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
	public void testGetPreferredRelays() {
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput1", result[0]);
		assertEquals("io0/InputOutput2", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithExistingComponent() {
		this.createTestRelay("io1");
		this.createTestComponent("io0/InputOutput2", "io0/InputOutput3");
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io1/InputOutput1", result[0]);
		assertEquals("io1/InputOutput2", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithSpecialConstraints() {
		final var constraints = new PreferredRelay(t -> t.id().equals("io0"), new int[] { 1, 2 });
		final var result = this.componentUtil.getPreferredRelays(this.componentUtil.getAllRelayInfos(), 2,
				List.of(constraints, PreferredRelay.of(10, new int[] { 2, 3 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput0", result[0]);
		assertEquals("io0/InputOutput1", result[1]);
	}

	@Test
	public void testGetPreferredRelaysWithMissingRelayContacts() {
		final var relayInfo = new RelayInfo("io0", "alias", 10, List.of(//
				new RelayContactInfo("io0/InputOutput0", null, 0, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput1", null, 1, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput2", null, 2, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput3", null, 3, emptyList(), emptyList()), //
				// skip 4
				new RelayContactInfo("io0/InputOutput5", null, 5, emptyList(), emptyList()), //
				new RelayContactInfo("io0/InputOutput6", null, 6, emptyList(), emptyList()), //
				// skip 7-8
				new RelayContactInfo("io0/InputOutput9", null, 9, emptyList(), emptyList()) //
		));
		final var result = this.componentUtil.getPreferredRelays(List.of(relayInfo), 2,
				List.of(PreferredRelay.of(10, new int[] { 6, 7 })));
		assertNotNull(result);
		assertEquals(2, result.length);
		assertEquals("io0/InputOutput5", result[0]);
		assertEquals("io0/InputOutput6", result[1]);
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
