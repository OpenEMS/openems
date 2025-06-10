package io.openems.backend.b2bwebsocket;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import java.util.Optional;

import org.junit.Test;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsDataTest {

	@Test
	public void test() throws OpenemsNamedException {
		var sut = new WsData(null, null);
		assertEquals("B2bWebsocket.WsData [user=UNDEFINED]", sut.toLogString());
		assertEquals(Optional.empty(), sut.getUserOpt());
		assertThrows(OpenemsNamedException.class, () -> sut.getUserWithTimeout(1, MILLISECONDS));
		assertEquals(null, sut.getUser().getNow(null));

		var user = new User("foo", null, null, null, null, false, null);
		sut.setUser(user);
		assertEquals("B2bWebsocket.WsData [user=foo]", sut.toLogString());
		assertEquals(Optional.of(user), sut.getUserOpt());
		assertEquals(user, sut.getUserWithTimeout(1, MILLISECONDS));
		assertNotNull(sut.getSubscribedChannelsWorker());

		sut.dispose();
	}

}
