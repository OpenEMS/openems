package io.openems.core.utilities.websocket;

import java.util.Optional;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import io.openems.core.ThingRepository;
import io.openems.impl.persistence.fenecon.FeneconPersistence;
import io.openems.impl.persistence.fenecon.FeneconPersistenceWebsocketHandler;

public class WebsocketLogAppender extends AppenderBase<ILoggingEvent> {

	@Override
	protected void append(ILoggingEvent event) {
		long timestamp = event.getTimeStamp();
		String level = event.getLevel().toString();
		String source = event.getLoggerName();
		String message = event.getFormattedMessage();

		// send to websockets
		// TODO WebsocketServer.broadcastLog(timestamp, level, source, message);

		// send to fenecon persistence
		ThingRepository.getInstance().getPersistences().forEach((persistence) -> {
			if (persistence instanceof FeneconPersistence) {
				FeneconPersistence p = (FeneconPersistence) persistence;
				Optional<FeneconPersistenceWebsocketHandler> handler = p.getWebsocketHandler();
				if (handler.isPresent()) {
					handler.get().sendLog(timestamp, level, source, message);
				}
			}
		});
	}

}
