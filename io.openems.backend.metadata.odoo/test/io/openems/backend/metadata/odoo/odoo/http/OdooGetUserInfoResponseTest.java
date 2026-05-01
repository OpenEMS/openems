package io.openems.backend.metadata.odoo.odoo.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;

public class OdooGetUserInfoResponseTest {

	@Test
	public void testSettingsAsString() throws Exception {
		final var deviceData = OdooGetUserInfoResponse.serializer().deserialize("""
				        {
				            "user": {
				            	"id": 1,
				            	"login": "test@test.test",
				            	"name": "test",
				            	"language": "de",
				            	"global_role": "owner",
				            	"has_multiple_edges": false,
				            	"settings": "{ \\"example\\": \\"value\\" }"
				            }
				        }
				""".stripIndent());

		assertEquals(new OdooGetUserInfoResponse(//
				1, //
				"test@test.test", //
				"test", //
				Language.DE, //
				Role.OWNER, //
				false, //
				JsonUtils.buildJsonObject() //
						.addProperty("example", "value") //
						.build()),
				deviceData);
	}

	@Test
	public void testSettingsAsObject() throws Exception {
		final var deviceData = OdooGetUserInfoResponse.serializer().deserialize("""
				        {
				            "user": {
				            	"id": 1,
				            	"login": "test@test.test",
				            	"name": "test",
				            	"language": "de",
				            	"global_role": "owner",
				            	"has_multiple_edges": false,
				            	"settings": { "example": "value" }
				            }
				        }
				""".stripIndent());

		assertEquals(new OdooGetUserInfoResponse(//
				1, //
				"test@test.test", //
				"test", //
				Language.DE, //
				Role.OWNER, //
				false, //
				JsonUtils.buildJsonObject() //
						.addProperty("example", "value") //
						.build()),
				deviceData);
	}

}