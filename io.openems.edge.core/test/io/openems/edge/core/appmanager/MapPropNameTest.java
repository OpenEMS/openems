package io.openems.edge.core.appmanager;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.app.TestMapPropName;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class MapPropNameTest {

	private AppManagerTestBundle appManagerTestBundle;
	private TestMapPropName testMapPropName;
	private OpenemsAppInstance appInstance;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.testMapPropName = Apps.testMapPropName(t) //
			);
		});
		this.appInstance = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.testMapPropName.getAppId(), "key", "alias", buildJsonObject() //
						.addProperty(TestMapPropName.Property.ID.name(), "id0") //
						.addProperty(TestMapPropName.Property.BIDIRECTIONAL.name(), "biTest") //
						.addProperty(TestMapPropName.Property.NOT_BIDIRECTIONAL.name(), "notBiTest") //
						.addProperty(TestMapPropName.Property.BIDIRECTIONAL_SAME_NAME.name(), "biSameTest") //
						.build()))
				.instance();
	}

	@Test
	public void testNotBidirectional() {
		var test = this.testMapPropName.mapPropName("notBidirectional", "test", this.appInstance);
		assertEquals(TestMapPropName.Property.NOT_BIDIRECTIONAL.name(), test);
	}

	@Test
	public void testNotAValidName() {
		var test = this.testMapPropName.mapPropName("notAValidName", "test", this.appInstance);
		assertNull(test);
	}

	@Test
	public void testBidirectional() {
		var test = this.testMapPropName.mapPropName("testProperty", "test", this.appInstance);
		assertEquals(TestMapPropName.Property.BIDIRECTIONAL.name(), test);
		test = this.testMapPropName.mapPropName("bidirectional", "test", this.appInstance);
		assertNull(test);
	}

	@Test
	public void testBidirectionalSame() {
		var test = this.testMapPropName.mapPropName("bidirectionalSameName", "test", this.appInstance);
		assertEquals(TestMapPropName.Property.BIDIRECTIONAL_SAME_NAME.name(), test);
	}

}
