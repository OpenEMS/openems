package io.openems.edge.braiinsos.api;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Ignore;
import org.junit.Test;

import io.openems.common.bridge.http.NetworkEndpointFetcher;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.FunctionUtils;

public class BraiinsApiTest {

	@Ignore
	@Test
	public void testGetTokenActually() throws InterruptedException, ExecutionException {
		final var factory = ofBridgeImpl(//
				() -> new NetworkEndpointFetcher(), //
				() -> dummyBridgeHttpExecutor(createDummyClock(), true));
		var sut = new BraiinsApi(factory, "localhost", "root", "", FunctionUtils::doNothing);
		System.out.println(sut.getToken().get());
		Thread.sleep(5000);
	}

	@Test
	public void testUpdateToken() throws InterruptedException, ExecutionException {
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			return switch (endpoint.url().substring("http://localhost/api/v1".length())) {
			case "/auth/login" -> {
				assertEquals(HttpMethod.POST, endpoint.method());
				yield HttpResponse.ok(AuthLogin.serializer().serialize(//
						new AuthLogin("my-token", 3600)) //
						.toString());
			}
			case "/miner/stats" -> {
				assertEquals(HttpMethod.GET, endpoint.method());
				yield HttpResponse.ok(MinerStats.serializer().serialize(//
						new MinerStats(6789., 1234, 56.)) //
						.toString());
			}
			case "/actions/resume" -> {
				assertEquals(HttpMethod.PUT, endpoint.method());
				yield HttpResponse.ok("true");
			}
			case "/actions/pause" -> {
				assertEquals(HttpMethod.PUT, endpoint.method());
				yield HttpResponse.ok("true");
			}
			default -> {
				System.err.println("Unhandled: " + endpoint);
				throw HttpError.ResponseError.notFound();
			}
			};
		});

		final var executor = dummyBridgeHttpExecutor(true);
		final var factory = ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> executor //
		);
		var sut = new BraiinsApi(factory, "localhost", "root", "", FunctionUtils::doNothing);
		// var future = sut.getMinerStats();
		// var ms = future.get();
		//
		// assertEquals(6789., ms.realHashRateLast15s(), 0.1);
		// assertEquals(1234, ms.approximatedConsumption());
		// assertEquals(56., ms.efficiency(), 0.1);

		sut.callActionResume().get();
		sut.callActionPause().get();
	}

	@Test
	public void testPollingHandlesStoppedMiner() {
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {
			return switch (endpoint.url().substring("http://localhost/api/v1".length())) {
			case "/auth/login" -> HttpResponse.ok(AuthLogin.serializer().serialize(//
					new AuthLogin("my-token", 3600)) //
					.toString());
			case "/miner/stats" -> throw new HttpError.ResponseError(HttpStatus.INTERNAL_SERVER_ERROR, null);
			default -> throw HttpError.ResponseError.notFound();
			};
		});

		final var executor = dummyBridgeHttpExecutor(createDummyClock(), false);
		final var factory = ofBridgeImpl(() -> endpointFetcher, () -> executor);
		final var polledStats = new AtomicReference<MinerStats>();
		var sut = new BraiinsApi(factory, "localhost", "root", "", polledStats::set);

		sut.activate();
		executor.update();
		executor.update();
		executor.update();
		executor.update();

		assertNotNull(polledStats.get());
		assertEquals(0., polledStats.get().realHashRateLast15s(), 0.1);
		assertEquals(0, polledStats.get().approximatedConsumption());
		assertEquals(0., polledStats.get().efficiency(), 0.1);

		sut.deactivate();
	}
}
