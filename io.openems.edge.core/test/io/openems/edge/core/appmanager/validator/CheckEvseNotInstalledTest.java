package io.openems.edge.core.appmanager.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.evse.api.chargepoint.dummy.DummyEvseChargePoint;

class CheckEvseNotInstalledTest {

	private DummyComponentManager componentManager;
	private CheckEvseNotInstalled sut;

	@BeforeEach
	void setUp() {
		this.componentManager = new DummyComponentManager();
		this.sut = new CheckEvseNotInstalled(this.componentManager);
	}

	@Test
	void checkReturnsTrueIfNoEvseIsInstalled() {
		assertTrue(this.sut.check());
	}

	@Test
	void checkReturnsFalseIfEvseIsInstalled() {
		this.componentManager.addComponent(new DummyEvseChargePoint("evse0"));

		assertFalse(this.sut.check());
	}

	@Test
	void getErrorMessageHasTranslations() {
		final var dt = TranslationUtil.enableDebugMode();
		for (var l : Language.values()) {
			this.sut.getErrorMessage(l);
			this.sut.getInvertedErrorMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}
}
