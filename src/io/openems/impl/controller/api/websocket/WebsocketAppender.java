package io.openems.impl.controller.api.websocket;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class WebsocketAppender extends AppenderBase<ILoggingEvent> {

	@Override
	protected void append(ILoggingEvent event) {
		WebsocketServer.broadcastLog(event.getTimeStamp(), event.getLevel().toString(), event.getLoggerName(),
				event.getFormattedMessage());
	}

}
