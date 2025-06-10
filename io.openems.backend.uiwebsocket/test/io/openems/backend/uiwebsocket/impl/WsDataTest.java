package io.openems.backend.uiwebsocket.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Optional;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class WsDataTest {

	private static final String USER_ID = "user0";
	private static final String TOKEN = "token";

	@Test
	public void test() throws OpenemsNamedException {
		var sut = new WsData(null);
		assertEquals(Optional.empty(), sut.getUser(null));
		assertThrows(OpenemsNamedException.class, () -> sut.assertToken());
		assertEquals("UiWebsocket.WsData [userId=UNKNOWN, token=UNKNOWN]", sut.toLogString());

		sut.setUserId(USER_ID);
		sut.setToken(TOKEN);

		assertEquals(Optional.of(USER_ID), sut.getUserId());
		assertEquals(Optional.of(TOKEN), sut.getToken());
		assertEquals(TOKEN, sut.assertToken());
		assertEquals("UiWebsocket.WsData [userId=user0, token=token]", sut.toLogString());

		sut.logout();
	}

}
