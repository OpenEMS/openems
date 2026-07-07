package io.openems.edge.core.appmanager;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.app.TestFilter;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter.Filter;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter.Filter.ComponentFilter;
import io.openems.edge.core.appmanager.jsonrpc.QueryAppInstancesByFilter.Pagination;

public class QueryAppInstancesByFilterTest {

	private AppManagerTestBundle appManagerTestBundle;

	private TestFilter testFilter;

	@Before
	public void setUp() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.testFilter = Apps.testFilter(t) //
			);
		});
		this.appManagerTestBundle.sut
				.handleAddAppInstanceRequest(DUMMY_ADMIN,
						new AddAppInstance.Request(this.testFilter.getAppId(), "key", "alias", buildJsonObject() //
								.addProperty(TestFilter.Property.ID.name(), "id0")
								.addProperty(TestFilter.Property.COMPONENT_FACTORY_ID.name(), "testFactory0").build()))
				.instance();
		this.appManagerTestBundle.sut
				.handleAddAppInstanceRequest(DUMMY_ADMIN,
						new AddAppInstance.Request(this.testFilter.getAppId(), "key", "alias", buildJsonObject() //
								.addProperty(TestFilter.Property.ID.name(), "id1")
								.addProperty(TestFilter.Property.COMPONENT_FACTORY_ID.name(), "testFactory1").build()))
				.instance();
	}

	@Test
	public void testSerializer() {
		var json = this.requestJson(null, null);
		var obj = this.requestObject(json);
		assertEquals(obj, this.request(null, null));

		json = this.requestJson(null, Set.of(OpenemsAppCategory.TEST));
		obj = this.requestObject(json);
		assertEquals(obj, this.request(null, Set.of(OpenemsAppCategory.TEST)));

		json = this.requestJson(new ComponentFilter(Set.of("id0"), null), null);
		obj = this.requestObject(json);
		assertEquals(obj, this.request(new ComponentFilter(Set.of("id0"), null), null));

		json = this.requestJson(new ComponentFilter(null, Set.of("factory1")), null);
		obj = this.requestObject(json);
		assertEquals(obj, this.request(new ComponentFilter(null, Set.of("factory1")), null));

		json = this.requestJson(new ComponentFilter(Set.of("id0"), Set.of("factory1")),
				Set.of(OpenemsAppCategory.TEST));
		obj = this.requestObject(json);
		assertEquals(obj,
				this.request(new ComponentFilter(Set.of("id0"), Set.of("factory1")), Set.of(OpenemsAppCategory.TEST)));

	}

	@Test
	public void testFilterByCategory() throws OpenemsNamedException {
		var req = this.request(null, Set.of(OpenemsAppCategory.TEST));
		var resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(null, null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(null, Set.of(OpenemsAppCategory.API));
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 0);

	}

	@Test
	public void testFilterByFactoryId() throws OpenemsNamedException {
		var req = this.request(null, null);
		var resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(new ComponentFilter(null, null), null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(new ComponentFilter(null, Set.of("testFactory1")), null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 1);
	}

	@Test
	public void testFilterByComponentId() throws OpenemsNamedException {
		var req = this.request(null, null);
		var resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(new ComponentFilter(null, null), null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);

		req = this.request(new ComponentFilter(Set.of("id1"), null), null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 1);
	}

	@Test
	public void testLimit() throws OpenemsNamedException {
		var req = this.request(null, null, new Pagination(1));
		var resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 1);

		req = this.request(null, null, null);
		resp = this.appManagerTestBundle.sut.handleQueryAppInstancesByFilterRequest(req);
		assertEquals(resp.apps().size(), 2);
	}

	private JsonElement requestJson(ComponentFilter component, Set<OpenemsAppCategory> categorys) {
		final var filter = new Filter(component, categorys);
		final var req = new QueryAppInstancesByFilter.Request(filter, new Pagination(2));

		return QueryAppInstancesByFilter.Request.serializer().serialize(req);
	}

	private QueryAppInstancesByFilter.Request requestObject(JsonElement json) {
		return QueryAppInstancesByFilter.Request.serializer().deserialize(json);
	}

	private QueryAppInstancesByFilter.Request request(ComponentFilter component, Set<OpenemsAppCategory> categorys) {
		return new QueryAppInstancesByFilter.Request(new Filter(component, categorys), new Pagination(2));
	}

	private QueryAppInstancesByFilter.Request request(ComponentFilter component, Set<OpenemsAppCategory> categorys,
			Pagination pagination) {
		return new QueryAppInstancesByFilter.Request(new Filter(component, categorys), pagination);
	}

}
