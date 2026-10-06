package io.openems.edge.controller.api.websocket.externalauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.java_websocket.handshake.Handshakedata;
import org.junit.Test;

import io.openems.common.session.Role;

public class ExternalAuthenticationTest {

	private static class TestHandshake implements Handshakedata {

		private final Map<String, String> headers;

		private TestHandshake(Map<String, String> headers) {
			this.headers = headers;
		}

		@Override
		public Iterator<String> iterateHttpFields() {
			return this.headers.keySet().iterator();
		}

		@Override
		public String getFieldValue(String name) {
			return this.headers.get(name);
		}

		@Override
		public boolean hasFieldValue(String name) {
			return this.headers.containsKey(name);
		}

		@Override
		public byte[] getContent() {
			return new byte[0];
		}

	}

	@Test
	public void testResolvesTrustedHeaderUser() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				new String[] { "127.0.0.1/32" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		final var user = externalAuth.resolveUser(InetAddress.getByName("127.0.0.1"), new TestHandshake(Map.of(//
				"x-openems-user", "alice", //
				"x-openems-name", "Alice Example", //
				"x-openems-role", "installer"))) //
				.orElseThrow();

		assertEquals("external:alice", user.getId());
		assertEquals("Alice Example", user.getName());
		assertEquals(Role.INSTALLER, user.getRole());
	}

	@Test
	public void testRejectsUntrustedProxy() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				new String[] { "127.0.0.1/32" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		assertTrue(externalAuth.resolveUser(InetAddress.getByName("192.168.1.10"),
				new TestHandshake(Map.of("X-OpenEMS-User", "alice"))).isEmpty());
	}

	@Test
	public void testUsesDefaultRole() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				new String[] { "127.0.0.0/8" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"owner"));

		final var user = externalAuth
				.resolveUser(InetAddress.getByName("127.0.0.10"), new TestHandshake(Map.of("X-OpenEMS-User", "alice")))
				.orElseThrow();

		assertEquals(Role.OWNER, user.getRole());
	}

	@Test
	public void testRejectsMissingUserIdHeader() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				new String[] { "127.0.0.1/32" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		assertEquals(Optional.empty(),
				externalAuth.resolveUser(InetAddress.getByName("127.0.0.1"), new TestHandshake(Map.of())));
	}

	@Test
	public void testRejectsWhenDisabled() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				false, //
				new String[] { "127.0.0.1/32" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		assertTrue(externalAuth.resolveUser(InetAddress.getByName("127.0.0.1"),
				new TestHandshake(Map.of("X-OpenEMS-User", "alice"))).isEmpty());
	}

	@Test
	public void testRejectsWithoutTrustedProxyCidrs() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				null, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		assertTrue(externalAuth.resolveUser(InetAddress.getByName("127.0.0.1"),
				new TestHandshake(Map.of("X-OpenEMS-User", "alice"))).isEmpty());
	}

	@Test
	public void testUsesUserIdAsNameWhenNameHeaderIsMissing() throws Exception {
		final var externalAuth = new ExternalAuthentication(ExternalAuthentication.Config.create(//
				true, //
				new String[] { "127.0.0.1/32" }, //
				"X-OpenEMS-User", //
				"X-OpenEMS-Name", //
				"X-OpenEMS-Role", //
				"guest"));

		final var user = externalAuth
				.resolveUser(InetAddress.getByName("127.0.0.1"), new TestHandshake(Map.of("X-OpenEMS-User", "alice")))
				.orElseThrow();

		assertEquals("alice", user.getName());
		assertEquals(Role.GUEST, user.getRole());
	}

	@Test
	public void testCidrBlockParsingAndMatching() throws Exception {
		final var loopback = ExternalAuthentication.CidrBlock.parse("127.0.0.1").orElseThrow();
		assertTrue(loopback.contains(InetAddress.getByName("127.0.0.1")));
		assertFalse(loopback.contains(InetAddress.getByName("127.0.0.2")));

		final var any = ExternalAuthentication.CidrBlock.parse("0.0.0.0/0").orElseThrow();
		assertTrue(any.contains(InetAddress.getByName("192.168.1.10")));

		final var network = ExternalAuthentication.CidrBlock.parse("192.168.1.0/24").orElseThrow();
		assertTrue(network.contains(InetAddress.getByName("192.168.1.42")));
		assertFalse(network.contains(InetAddress.getByName("192.168.2.42")));
		assertFalse(network.contains(InetAddress.getByName("::1")));
	}

	@Test
	public void testRejectsInvalidCidrBlocks() {
		assertTrue(ExternalAuthentication.CidrBlock.parse(null).isEmpty());
		assertTrue(ExternalAuthentication.CidrBlock.parse("").isEmpty());
		assertTrue(ExternalAuthentication.CidrBlock.parse("192.168.1.1/33").isEmpty());
		assertTrue(ExternalAuthentication.CidrBlock.parse("not-an-address").isEmpty());
		assertTrue(ExternalAuthentication.CidrBlock.parse("::1/128").isEmpty());
	}
}
