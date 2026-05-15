package io.openems.edge.core.host;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.Objects;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a network route configuration, including destination, gateway,
 * gateway-on-link flag, and route metric. This class is used to model static
 * routes that can be serialized and de-serialized via JSON.
 */
public class Routes {

	private final String routeGateway;
	private final String routeDestination;
	private final boolean routeGatewayOnLink;
	private final Integer routeMetric;

	/**
	 * Creates a new {@code Routes} instance.
	 *
	 * @param routeGateway       the gateway address of the route (nullable)
	 * @param routeDestination   the destination network of the route (nullable)
	 * @param routeGatewayOnLink whether the gateway is directly reachable (on-link)
	 * @param routeMetric        the metric (priority) of the route, lower means
	 *                           higher priority (nullable)
	 */
	public Routes(String routeGateway, String routeDestination, boolean routeGatewayOnLink, Integer routeMetric) {
		this.routeGateway = routeGateway;
		this.routeDestination = routeDestination;
		this.routeGatewayOnLink = routeGatewayOnLink;
		this.routeMetric = routeMetric;
	}

	/**
	 * Gets the gateway address of the route.
	 *
	 * @return the route gateway, or {@code null} if not defined
	 */
	public String getRouteGateway() {
		return this.routeGateway;
	}

	/**
	 * Gets the destination network of the route.
	 *
	 * @return the route destination, or {@code null} if not defined
	 */
	public String getRouteDestination() {
		return this.routeDestination;
	}

	/**
	 * Returns whether the route gateway is on-link (directly reachable without an
	 * intermediate router).
	 *
	 * @return {@code true} if the gateway is on-link; {@code false} otherwise
	 */
	public boolean isRouteGatewayOnLink() {
		return this.routeGatewayOnLink;
	}

	/**
	 * Gets the route metric (priority). Lower values indicate higher priority.
	 *
	 * @return the route metric, or {@code null} if not defined
	 */
	public Integer geRouteMetric() {
		return this.routeMetric;
	}

	/**
	 * Returns a new {@link Builder} instance to construct a {@link Routes} object.
	 *
	 * @return a new {@link Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * Builder class for constructing {@link Routes} instances in a fluent style.
	 */
	public static final class Builder {
		private String routeGateway;
		private String routeDestination;
		private boolean routeGatewayOnLink;
		private Integer routeMetric;

		/**
		 * Sets the gateway address of the route.
		 *
		 * @param routeGateway the gateway address
		 * @return this builder instance
		 */
		public Builder setRouteGateway(String routeGateway) {
			this.routeGateway = routeGateway;
			return this;
		}

		/**
		 * Sets the destination network of the route.
		 *
		 * @param routeDestination the destination network
		 * @return this builder instance
		 */
		public Builder setRouteDestination(String routeDestination) {
			this.routeDestination = routeDestination;
			return this;
		}

		/**
		 * Sets whether the gateway is on-link (directly reachable).
		 *
		 * @param routeGatewayOnLink {@code true} if on-link, {@code false} otherwise
		 * @return this builder instance
		 */
		public Builder setRouteGatewayOnLink(boolean routeGatewayOnLink) {
			this.routeGatewayOnLink = routeGatewayOnLink;
			return this;
		}

		/**
		 * Sets the route metric (priority).
		 *
		 * @param routeMetric the route metric, where lower values indicate higher
		 *                    priority
		 * @return this builder instance
		 */
		public Builder setRouteMetric(Integer routeMetric) {
			this.routeMetric = routeMetric;
			return this;
		}

		/**
		 * Builds a new {@link Routes} instance using the configured parameters.
		 *
		 * @return a new {@link Routes} object
		 */
		public Routes build() {
			return new Routes(this.routeGateway, this.routeDestination, this.routeGatewayOnLink, this.routeMetric);
		}
	}

	/**
	 * Returns a {@link JsonSerializer} for serializing and de-serializing
	 * {@link Routes} objects to and from JSON.
	 *
	 * @return a {@link JsonSerializer} instance
	 */
	public static JsonSerializer<Routes> serializer() {
		return jsonObjectSerializer(Routes.class, json -> {
			return new Routes(//
					json.getStringOrNull("routeGateway"), //
					json.getStringOrNull("routeDestination"), //
					json.getBoolean("routeGatewayOnLink"), //
					json.getOptionalInt("metric").orElse(null)); //
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addPropertyIfNotNull("routeGateway", obj.getRouteGateway()) //
					.addPropertyIfNotNull("routeDestination", obj.getRouteDestination()) //
					.addProperty("routeGatewayOnLink", obj.isRouteGatewayOnLink()) //
					.addPropertyIfNotNull("metric", obj.geRouteMetric()) //
					.build();
		});
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.routeDestination, this.routeGateway, this.routeGatewayOnLink, this.routeMetric);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		Routes other = (Routes) obj;
		return Objects.equals(this.routeDestination, other.routeDestination)
				&& Objects.equals(this.routeGateway, other.routeGateway)
				&& this.routeGatewayOnLink == other.routeGatewayOnLink
				&& Objects.equals(this.routeMetric, other.routeMetric);
	}

	@Override
	public String toString() {
		return "Routes[" //
				+ "routeGateway=" + this.routeGateway //
				+ ", routeDestination=" + this.routeDestination //
				+ ", routeGatewayOnLink=" + this.routeGatewayOnLink //
				+ ", metric=" + (this.routeMetric != null ? this.routeMetric : "not set") //
				+ "]";
	}

}