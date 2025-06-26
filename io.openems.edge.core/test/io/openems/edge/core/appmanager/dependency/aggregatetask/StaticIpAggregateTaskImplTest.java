package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.session.Language;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;

public class StaticIpAggregateTaskImplTest {

	private StaticIpAggregateTask task;
	private DummyPseudoComponentManager componentManager;
	private DummyConfigurationAdmin cm;
	private ComponentUtilImpl componentUtil;

	@Before
	public void setUp() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.cm = new DummyConfigurationAdmin();
		this.componentUtil = new ComponentUtilImpl(this.componentManager);
		this.componentManager.setConfigurationAdmin(this.cm);
		this.task = new StaticIpAggregateTaskImpl(this.componentUtil);
		this.task.reset();
	}

	@Test
	public void testGetGeneralFailMessage() {
		final var dt = TranslationUtil.enableDebugMode();

		for (var l : Language.values()) {
			this.task.getGeneralFailMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

}
