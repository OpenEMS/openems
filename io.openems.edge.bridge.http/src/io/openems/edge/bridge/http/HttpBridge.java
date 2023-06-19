package io.openems.edge.bridge.http;

import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface HttpBridge extends OpenemsComponent {
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		REQUEST_PER_CYCLE(Doc.of(OpenemsType.INTEGER));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	/**
	 * Executes a restful HTTP GET request to the given URL.
	 * @param url where the request should be sent to.
	 * @return a response object.
	 */
	public HttpResponse get(URL url);
	
	/**
	 * Executes a restful HTTP POST request to the given URL. The body argument will be converted to JSON and sent.
	 * @param url where to send the request.
	 * @param body the body of the request.
	 * @return a response object.
	 */
	public HttpResponse post(URL url, Object body);

}
