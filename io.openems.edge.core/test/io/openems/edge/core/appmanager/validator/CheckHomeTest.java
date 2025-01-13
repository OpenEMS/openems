package io.openems.edge.core.appmanager.validator;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.integratedsystem.TestFeneconHome10;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.app.integratedsystem.TestFeneconHome30;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class CheckHomeTest {

	private AppManagerTestBundle appManagerTestBundle;

	private CheckHome checkHome;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					Apps.feneconHome10(t), //
					Apps.feneconHome20(t), //
					Apps.feneconHome30(t) //
			);
		}, null, new PseudoComponentManagerFactory());
		this.checkHome = this.appManagerTestBundle.addCheckable(CheckHome.COMPONENT_NAME,
				t -> new CheckHome(t, new CheckAppsNotInstalled(this.appManagerTestBundle.sut,
						AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME))));
	}

	@Test
	public void testCheck() {
		assertFalse(this.checkHome.check());
		assertFalse(PropsUtil.isHomeInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledHome10() throws Exception {
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home", "key", "alias", TestFeneconHome10.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkHome.check());
		assertTrue(PropsUtil.isHomeInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledHome20() throws Exception {
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home.20", "key", "alias", TestFeneconHome20.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkHome.check());
		assertTrue(PropsUtil.isHomeInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledHome30() throws Exception {
		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home.30", "key", "alias", TestFeneconHome30.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkHome.check());
		assertTrue(PropsUtil.isHomeInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testGetErrorMessage() {
		final var dt = TranslationUtil.enableDebugMode();
		for (var l : Language.values()) {
			this.checkHome.getErrorMessage(l);
			this.checkHome.getInvertedErrorMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

}
