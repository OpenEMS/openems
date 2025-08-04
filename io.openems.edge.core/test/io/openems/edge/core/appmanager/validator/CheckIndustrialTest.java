package io.openems.edge.core.appmanager.validator;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonObject;
import io.openems.edge.core.appmanager.OpenemsApp;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.edge.app.common.props.PropsUtil;
import io.openems.edge.app.integratedsystem.TestFeneconIndustrialS;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class CheckIndustrialTest {

	private AppManagerTestBundle appManagerTestBundle;

	private CheckIndustrial checkIndustrial;

	private OpenemsApp hardwareApp;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, //
				t -> ImmutableList.of(//
						Apps.feneconIndustrialSIsk010(t), //
						Apps.feneconIndustrialSIsk011(t), //
						Apps.feneconIndustrialSIsk110(t), //
						Apps.feneconIndustrialLIlk710(t), //
						this.hardwareApp = Apps.techbaseCm4(t), //
						Apps.ioGpio(t) //
				), null, new AppManagerTestBundle.PseudoComponentManagerFactory());
		this.checkIndustrial = this.appManagerTestBundle.addCheckable(CheckIndustrial.COMPONENT_NAME,
				t -> new CheckIndustrial(t, new CheckAppsNotInstalled(this.appManagerTestBundle.sut,
						AppManagerTestBundle.getComponentContext(CheckAppsNotInstalled.COMPONENT_NAME))));
	}

	@Test
	public void testCheck() {
		assertFalse(this.checkIndustrial.check());
		assertFalse(PropsUtil.isIndustrialInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledIsk010() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardwareApp.getAppId(), "key", "alias", new JsonObject()));

		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Industrial.S.ISK010", "key", "alias",
						TestFeneconIndustrialS.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkIndustrial.check());
		assertTrue(PropsUtil.isIndustrialInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledIsk011() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardwareApp.getAppId(), "key", "alias", new JsonObject()));

		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Industrial.S.ISK011", "key", "alias",
						TestFeneconIndustrialS.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkIndustrial.check());
		assertTrue(PropsUtil.isIndustrialInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testCheckWithInstalledIsk110() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.hardwareApp.getAppId(), "key", "alias", new JsonObject()));

		final var response = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Industrial.S.ISK110", "key", "alias",
						TestFeneconIndustrialS.fullSettings()));

		assertTrue(response.warnings().isEmpty());
		assertTrue(this.checkIndustrial.check());
		assertTrue(PropsUtil.isIndustrialInstalled(this.appManagerTestBundle.appManagerUtil));
	}

	@Test
	public void testGetErrorMessage() {
		final var dt = TranslationUtil.enableDebugMode();
		for (var l : Language.values()) {
			this.checkIndustrial.getErrorMessage(l);
			this.checkIndustrial.getInvertedErrorMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

}
