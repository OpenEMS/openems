package io.openems.edge.bridge.http.api;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = BridgeHttpFactory.class)
public class BridgeHttpFactory {

	@Reference
	private ComponentServiceObjects<BridgeHttp> csoBridgeHttp;

	@Activate
	public BridgeHttpFactory() {

	}

	/**
	 * Returns a new {@link BridgeHttp} service object.
	 * 
	 * @return the created {@link BridgeHttp} object
	 * @see BridgeHttpFactory#unget(BridgeHttp)
	 */
	public BridgeHttp get() {
		return this.csoBridgeHttp.getService();
	}

	/**
	 * Releases the {@link BridgeHttp} service object.
	 * 
	 * @param bridge a {@link BridgeHttp} provided by this factory
	 * @see BridgeHttpFactory#unget(BridgeHttp)
	 */
	public void unget(BridgeHttp bridge) {
		if (bridge == null) {
			return;
		}
		this.csoBridgeHttp.ungetService(bridge);
	}

}
