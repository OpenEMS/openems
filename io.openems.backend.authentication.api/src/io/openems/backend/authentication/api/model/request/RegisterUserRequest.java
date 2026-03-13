package io.openems.backend.authentication.api.model.request;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public record RegisterUserRequest(//
		String firstname, //
		String lastname, //
		String phone, //
		String email, //
		String password, //
		boolean includePasswordInRegistrationEmail, //
		Address address, //
		Role role //
) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link RegisterUserRequest}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<RegisterUserRequest> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(RegisterUserRequest.class, //
				json -> new RegisterUserRequest(//
						json.getString("firstname"), //
						json.getString("lastname"), //
						json.getString("phone"), //
						json.getString("email"), //
						json.getStringOrNull("password"), //
						json.getOptionalBoolean("includePasswordInRegistrationEmail").orElse(false), //
						json.getObject("address", Address.serializer()), //
						json.getEnum("role", Role.class)), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("firstname", obj.firstname) //
						.addProperty("lastname", obj.lastname) //
						.addProperty("phone", obj.phone) //
						.addProperty("email", obj.email) //
						.addPropertyIfNotNull("password", obj.password) //
						.onlyIf(obj.includePasswordInRegistrationEmail(),
								b -> b.addProperty("includePasswordInRegistrationEmail",
										obj.includePasswordInRegistrationEmail())) //
						.add("address", Address.serializer().serialize(obj.address)) //
						.addProperty("role", obj.role) //
						.build());
	}

	public record Address(String street, String zip, String city, String country) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Address}.
		 *
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Address> serializer() {
			return JsonSerializerUtil.jsonObjectSerializer(Address.class, //
					json -> new Address(//
							json.getString("street"), //
							json.getString("zip"), //
							json.getString("city"), //
							json.getString("country")), //
					obj -> JsonUtils.buildJsonObject() //
							.addProperty("street", obj.street) //
							.addProperty("zip", obj.zip) //
							.addProperty("city", obj.city) //
							.addProperty("country", obj.country) //
							.build());
		}

	}

	/**
	 * Creates a new {@link RegisterUserRequest} with the given
	 * includePasswordInRegistrationEmail.
	 * 
	 * @param includePasswordInRegistrationEmail whether to include the password in
	 *                                           the registration email
	 * @return the created {@link RegisterUserRequest}
	 */
	public RegisterUserRequest withIncludePasswordInRegistrationEmail(boolean includePasswordInRegistrationEmail) {
		return new RegisterUserRequest(this.firstname, this.lastname, this.phone, this.email, this.password,
				includePasswordInRegistrationEmail, this.address, this.role);
	}

}