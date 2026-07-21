package io.openems.edge.core.appmanager.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;

class CheckEvcsNotInstalledTest {

	private DummyComponentManager componentManager;
	private CheckEvcsNotInstalled sut;

	@BeforeEach
	void setUp() {
		this.componentManager = new DummyComponentManager();
		this.sut = new CheckEvcsNotInstalled(this.componentManager);
	}

	@Test
	void checkReturnsTrueIfNoEvcsIsInstalled() {
		assertTrue(this.sut.check());
	}

	@Test
	void checkReturnsFalseIfEvcsIsInstalled() {
		this.componentManager.addComponent(new DummyManagedEvcs("evcs0", new DummyEvcsPower()));

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
