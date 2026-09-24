package io.openems.edge.controller.api.websocket.externalauth;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.java_websocket.handshake.Handshakedata;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.websocket.WebsocketUtils;
import io.openems.edge.common.user.User;

/**
 * Resolves externally authenticated users from trusted WebSocket handshake
 * headers.
 *
 * <p>
 * The first implementation supports trusted reverse proxies. It intentionally
 * keeps user/role/session creation independent from the concrete external
 * provider so a native OIDC provider can reuse the same model later.
 */
public final class ExternalAuthentication {

	public record Config(//
			boolean enabled, //
			List<CidrBlock> trustedProxyCidrs, //
			String userIdHeader, //
			String userNameHeader, //
			String roleHeader, //
			Role defaultRole //
	) {
		/**
		 * Creates a {@link Config}.
		 *
		 * @param enabled                  whether external authentication is enabled
		 * @param trustedProxyCidrs        trusted proxy CIDR ranges
		 * @param userIdHeader             user id header
		 * @param userNameHeader           user name header
		 * @param roleHeader               role header
		 * @param defaultRole              default role
		 * @return the {@link Config}
		 */
		public static Config create(//
				boolean enabled, //
				String[] trustedProxyCidrs, //
				String userIdHeader, //
				String userNameHeader, //
				String roleHeader, //
				String defaultRole //
		) {
			return new Config(enabled, parseTrustedProxyCidrs(trustedProxyCidrs), userIdHeader.strip(),
					userNameHeader.strip(), roleHeader.strip(), Role.getRole(defaultRole));
		}
	}

	public record CidrBlock(Inet4Address address, int prefixLength) {

		private static final int IPV4_BIT_COUNT = 32;

		/**
		 * Parses a CIDR block.
		 *
		 * @param raw the raw CIDR block
		 * @return the {@link CidrBlock}
		 */
		public static Optional<CidrBlock> parse(String raw) {
			if (raw == null || raw.isBlank()) {
				return Optional.empty();
			}
			final var parts = raw.strip().split("/", 2);
			try {
				final var address = (Inet4Address) InetAddress.getByName(parts[0]);
				final var prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : IPV4_BIT_COUNT;
				if (prefixLength < 0 || prefixLength > IPV4_BIT_COUNT) {
					return Optional.empty();
				}
				return Optional.of(new CidrBlock(address, prefixLength));
			} catch (UnknownHostException | ClassCastException | NumberFormatException e) {
				return Optional.empty();
			}
		}

		/**
		 * Checks if the candidate address is contained in this block.
		 *
		 * @param candidate the candidate address
		 * @return true if contained
		 */
		public boolean contains(InetAddress candidate) {
			if (!(candidate instanceof Inet4Address candidate4)) {
				return false;
			}
			final var mask = this.prefixLength == 0 //
					? 0 //
					: -1 << (IPV4_BIT_COUNT - this.prefixLength);
			return (toInt(this.address) & mask) == (toInt(candidate4) & mask);
		}

		private static int toInt(Inet4Address address) {
			final var bytes = address.getAddress();
			return (bytes[0] & 0xff) << 24 //
					| (bytes[1] & 0xff) << 16 //
					| (bytes[2] & 0xff) << 8 //
					| bytes[3] & 0xff;
		}
	}

	private final Config config;

	public ExternalAuthentication(Config config) {
		this.config = Objects.requireNonNull(config);
	}

	/**
	 * Resolve the external user from the WebSocket handshake.
	 *
	 * @param remoteAddress the direct peer address; usually the reverse proxy
	 * @param handshake     the WebSocket handshake with HTTP headers
	 * @return the resolved user, if external authentication is configured and valid
	 */
	public Optional<User> resolveUser(InetAddress remoteAddress, Handshakedata handshake) {
		if (!this.config.enabled() || !this.isTrustedProxy(remoteAddress)) {
			return Optional.empty();
		}

		return WebsocketUtils.getAsOptionalString(handshake, this.config.userIdHeader()) //
				.filter(userId -> !userId.isBlank()) //
				.map(userId -> {
					final var name = WebsocketUtils.getAsOptionalString(handshake, this.config.userNameHeader()) //
							.filter(value -> !value.isBlank()) //
							.orElse(userId);
					final var role = WebsocketUtils.getAsOptionalString(handshake, this.config.roleHeader()) //
							.filter(value -> !value.isBlank()) //
							.map(Role::getRole) //
							.orElse(this.config.defaultRole());
					return new User("external:" + userId, name, Language.DEFAULT, role);
				});
	}

	private boolean isTrustedProxy(InetAddress remoteAddress) {
		if (remoteAddress == null || this.config.trustedProxyCidrs().isEmpty()) {
			return false;
		}
		return this.config.trustedProxyCidrs().stream().anyMatch(cidr -> cidr.contains(remoteAddress));
	}

	private static List<CidrBlock> parseTrustedProxyCidrs(String[] trustedProxyCidrs) {
		if (trustedProxyCidrs == null) {
			return List.of();
		}
		return Arrays.stream(trustedProxyCidrs) //
				.map(CidrBlock::parse) //
				.flatMap(Optional::stream) //
				.toList();
	}
}
