package io.openems.common.bridge.http.logging;

import java.net.ConnectException;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.annotations.VisibleForTesting;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.EndpointFetcherEvents;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.function.Disposable;
import io.openems.common.logger.ContextLogger;
import io.openems.common.types.DebugMode;

public class HttpBridgeLoggingService implements HttpBridgeService {

	private final Logger log;
	private final HttpBridgeLoggingServiceConfiguration config;

	private final Disposable disposable;

	public HttpBridgeLoggingService(//
			BridgeHttp bridgeHttp, //
			HttpBridgeLoggingServiceConfiguration config //
	) {
		this.log = new ContextLogger(HttpBridgeLoggingService.class,
				(config.contextId() != null ? "%s] [".formatted(config.contextId()) : "") + "HTTP");
		this.config = config;

		final var requestStartDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_START, eventData -> {
			if (!bridgeHttp.getDebugMode().isAtLeast(DebugMode.DETAILED)) {
				return;
			}
			this.log.info("Request[{}] started. {}", eventData.requestId(), this.toLogString(eventData.endpoint()));
		});
		final var requestSuccessDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_SUCCESS,
				eventData -> {
					if (!bridgeHttp.getDebugMode().isAtLeast(DebugMode.DETAILED)) {
						return;
					}
					this.log.info("Request[{}] success. {}", eventData.requestId(),
							this.toLogString(eventData.endpoint()));
				});
		final var requestFailedDisposable = bridgeHttp.subscribeEvent(EndpointFetcherEvents.REQUEST_FAILED,
				eventData -> {
					if (!bridgeHttp.getDebugMode().isAtLeast(DebugMode.SIMPLE)) {
						return;
					}
					final var exceptionLog = minimizeException(eventData.error());
					if (exceptionLog instanceof Exception e) {
						this.log.error("Request[{}] failed. {}", eventData.requestId(),
								this.toLogString(eventData.endpoint()), e);
					} else {
						this.log.error("Request[{}] failed. {} {}", eventData.requestId(),
								this.toLogString(eventData.endpoint()), exceptionLog);
					}
				});

		this.disposable = () -> {
			requestStartDisposable.dispose();
			requestSuccessDisposable.dispose();
			requestFailedDisposable.dispose();
		};
	}

	@Override
	public void close() throws Exception {
		this.disposable.dispose();
	}

	private String toLogString(BridgeHttp.Endpoint endpoint) {
		return "Endpoint{" //
				+ "method=" + endpoint.method() //
				+ ", url=" + endpoint.url() //
				+ ", header=" + sanitizeHeaders(this.config.sanitizeHeader(), endpoint.properties()) //
				+ "}";
	}

	@VisibleForTesting
	static Map<String, String> sanitizeHeaders(Predicate<HttpHeader> sanitizeHeader, Map<String, String> headers) {
		if (sanitizeHeader == null) {
			return headers;
		}
		return headers.entrySet().stream() //
				.map(e -> new HttpHeader(e.getKey(), e.getValue())) //
				.map(header -> {
					if (sanitizeHeader.test(header)) {
						return new HttpHeader(header.key(), "****");
					} else {
						return header;
					}
				}) //
				.collect(Collectors.toMap(HttpHeader::key, HttpHeader::value));
	}

	private static Object minimizeException(Exception e) {
		if (e instanceof HttpError.ResponseError) {
			return e.toString();
		}
		if (e instanceof HttpError.UnknownError unknownError) {
			if (unknownError.getCause() instanceof ConnectException) {
				return "ConnectException: " + e.getMessage();
			}
		}

		return e;
	}

}
