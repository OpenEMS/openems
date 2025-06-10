package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.edge.app.TestC;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class AppDefTest {

	private TestC testCApp;

	@Before
	public void beforeEach() throws Exception {
		new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.testCApp = Apps.testC(t) //
			);
		});
	}

	@Test
	public void testRequiredTrue() {
		final var def = AppDef.of() //
				.setField(JsonFormlyUtil::buildInputFromNameable) //
				.setRequired(true);

		final var field = def.getField().get(this.testCApp, Nameable.of("test"), Language.DEFAULT, new Object());
		final var jsonField = field.build();

		final var templateOptions = jsonField.get("templateOptions").getAsJsonObject();
		assertTrue(templateOptions.get("required").getAsBoolean());
	}

	@Test
	public void testRequiredFalse() {
		final var def = AppDef.of() //
				.setField(JsonFormlyUtil::buildInputFromNameable) //
				.setRequired(false);

		final var field = def.getField().get(this.testCApp, Nameable.of("test"), Language.DEFAULT, new Object());
		final var jsonField = field.build();

		final var templateOptions = jsonField.get("templateOptions").getAsJsonObject();
		assertFalse(templateOptions.has("required") && templateOptions.get("required").getAsBoolean());
	}

	@Test
	public void testRequiredNotSet() {
		final var def = AppDef.of() //
				.setField(JsonFormlyUtil::buildInputFromNameable);

		final var field = def.getField().get(this.testCApp, Nameable.of("test"), Language.DEFAULT, new Object());
		final var jsonField = field.build();

		final var templateOptions = jsonField.get("templateOptions").getAsJsonObject();
		assertFalse(templateOptions.has("required") && templateOptions.get("required").getAsBoolean());
	}

}
