package io.openems.backend.oem.fenecon;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.User;
import io.openems.common.session.Language;
import io.openems.common.session.Role;

public class FeneconBackendOemImplTest {

	@Test
	public void testAnonymizeEdgeComment() {
		final var oem = new FeneconBackendOemImpl();

		oem.activate(MyConfig.create() //
				.addDemoUserId("demo.user") //
				.build());

		final var userToAnonymize = new User("demo.user", "Name", "token", Language.DEFAULT, Role.GUEST, true,
				new JsonObject());
		final var userToNotAnonymize = new User("other.user", "Name", "token", Language.DEFAULT, Role.GUEST, true,
				new JsonObject());

		final var anonymized = oem.anonymizeEdgeComment(userToAnonymize, "Edge commend", "edge4321");
		assertEquals("FEMS 4321", anonymized);

		final var notAnonymized = oem.anonymizeEdgeComment(userToNotAnonymize, "Edge commend", "edge4321");
		assertEquals("Edge commend", notAnonymized);
	}

}
