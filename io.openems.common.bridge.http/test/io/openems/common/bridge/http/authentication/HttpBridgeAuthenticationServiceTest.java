package io.openems.common.bridge.http.authentication;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.openems.common.bridge.http.AsyncBridgeHttpExecutor;
import io.openems.common.bridge.http.BridgeHttpImpl;
import io.openems.common.bridge.http.NetworkEndpointFetcher;
import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpAuthorization;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpExecutor;
import io.openems.common.types.HttpStatus;

@SuppressWarnings("all")
class HttpBridgeAuthenticationServiceTest {

	@Test
	void testCreateServiceOncePerDefinition() {
		final var authService = new HttpBridgeAuthenticationService<>(new BridgeHttpImpl(mock(), mock()), mock());

		final HttpBridgeServiceDefinition<HttpBridgeService> dummyServiceDefinition = mock();
		when(dummyServiceDefinition.create(any(), any(), any())).thenReturn(mock());

		final var createdService = authService.createService(dummyServiceDefinition);

		assertEquals(createdService, authService.createService(dummyServiceDefinition));
		verify(dummyServiceDefinition, times(1)).create(any(), any(), any());
	}

	@Test
	void testAuthenticate() {
		final var executor = new DummyBridgeHttpExecutor(false);
		final var testBundle = DummyBridgeHttpBundle.of(executor);

		final Supplier<CompletableFuture<HttpHeader>> headerSupplier = mock();
		when(headerSupplier.get()).thenReturn(
				CompletableFuture.completedFuture(HttpHeader.authorization(HttpAuthorization.bearer("test-token"))));

		final var authService = new HttpBridgeAuthenticationService(testBundle.factory().get(),
				new HttpBridgeAuthenticationServiceDefinition.HttpBridgeAuthenticationServiceConfigHttpHeader(
						headerSupplier, HttpBridgeAuthenticationServiceDefinition.DEFAULT_SESSION_EXPIRED_PREDICATE
								.or((stringHttpResponse, throwable) -> false)));

		testBundle.forceNextSuccessfulResult(HttpResponse.ok(null));
		authService.get("http://test");

		verify(headerSupplier, times(1)).get();
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(null));
		authService.get("http://test");
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(null));
		authService.get("http://test");

		executor.update();
		verify(headerSupplier, times(1)).get();

		testBundle.forceNextFailedResult(new HttpError.ResponseError(HttpStatus.UNAUTHORIZED, null));
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(null));
		verify(headerSupplier, times(1)).get();
		authService.get("http://test");
		testBundle.forceNextSuccessfulResult(HttpResponse.ok(null));
		authService.get("http://test");
		executor.update();
		verify(headerSupplier, times(2)).get();
	}

	@RepeatedTest(100)
	@Disabled("Test needs a long time to run and uses an actual webserver.")
	void testSingleRefreshForParallelRequestsWithBackpressure() throws Exception {
		final var refreshCalls = new AtomicInteger(0);
		final var activeToken = new AtomicReference<>("token-1");

		final var server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/auth", exchange -> {
			final var refreshNumber = refreshCalls.incrementAndGet();
			LockSupport.parkNanos(Duration.ofSeconds(10).toNanos());
			final var nextToken = "token-" + refreshNumber;
			activeToken.set(nextToken);
			writeResponse(exchange, HttpStatus.OK, nextToken);
		});
		server.createContext("/data", exchange -> {
			final var expectedToken = HttpAuthorization.bearer(activeToken.get());
			final var actualToken = exchange.getRequestHeaders().getFirst(HttpHeader.HEADER_AUTHORIZATION);
			if (expectedToken.equals(actualToken)) {
				writeResponse(exchange, HttpStatus.OK, HttpStatus.OK.description());
				return;
			}
			writeResponse(exchange, HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.description());
		});
		server.start();

		final var endpoint = "http://127.0.0.1:" + server.getAddress().getPort();
		final var bridgeHttp = new BridgeHttpImpl(new NetworkEndpointFetcher(), new AsyncBridgeHttpExecutor());
		bridgeHttp.setMaximumPoolSize(2);

		final Supplier<CompletableFuture<HttpHeader>> authHeaderSupplier = () -> bridgeHttp
				.request(BridgeHttp.create(endpoint + "/auth") //
						.setMethod(HttpMethod.POST) //
						.setReadTimeout(20_000) //
						.build()) //
				.thenApply(response -> HttpHeader.authorization(HttpAuthorization.bearer(response.data())));

		try (final var authService = bridgeHttp
				.createService(HttpBridgeAuthenticationServiceDefinition.of(authHeaderSupplier))) {

			assertEquals(HttpStatus.OK.code(),
					authService.get(endpoint + "/data").get(20, TimeUnit.SECONDS).status().code());
			assertEquals(1, refreshCalls.get());

			// Invalidate the current session and force one shared refresh for all queued
			// calls.
			activeToken.set("token-2");

			final List<CompletableFuture<HttpResponse<String>>> requests = new ArrayList<>();
			for (var i = 0; i < 8; i++) {
				requests.add(authService.get(endpoint + "/data"));
			}

			final var startedAt = Instant.now();
			CompletableFuture.allOf(requests.toArray(new CompletableFuture[0])).get(40, TimeUnit.SECONDS);
			final var duration = Duration.between(startedAt, Instant.now());

			for (var request : requests) {
				assertEquals(HttpStatus.OK.code(), request.get().status().code());
			}

			assertTrue(duration.compareTo(Duration.ofSeconds(9)) >= 0,
					"Refresh should block parallel requests for about 10 seconds.");
			assertEquals(2, refreshCalls.get(), "Session refresh should only happen once for the parallel batch.");
		} finally {
			bridgeHttp.deactivate();
			server.stop(0);
		}
	}

	private static void writeResponse(HttpExchange exchange, HttpStatus status, String body) throws IOException {
		final var bytes = body.getBytes();
		exchange.sendResponseHeaders(status.code(), bytes.length);
		try (var outputStream = exchange.getResponseBody()) {
			outputStream.write(bytes);
		}
	}

}