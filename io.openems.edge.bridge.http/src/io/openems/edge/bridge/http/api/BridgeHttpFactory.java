package io.openems.edge.bridge.http.api;

import org.osgi.service.component.ComponentServiceObjects;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Bridge factory to get an instance of an {@link BridgeHttp}.
 * 
 * <p>
 * Usage:
 * 
 * <pre>
   <code>@Reference</code>
   private BridgeHttpFactory httpBridgeFactory;
   private BridgeHttp httpBridge;
   
   <code>@Activate</code>
   private void activate() {
       this.httpBridge = this.httpBridgeFactory.get();
   }
   
   <code>@Deactivate</code>
   private void deactivate() {
       this.httpBridgeFactory.unget(this.httpBridge);
       this.httpBridge = null;
   }
 * </pre>
 */
@Component(service = BridgeHttpFactory.class)
public class BridgeHttpFactory {

	private final ComponentServiceObjects<BridgeHttp> csoBridgeHttp;

	@Activate
	public BridgeHttpFactory(@Reference ComponentServiceObjects<BridgeHttp> csoBridgeHttp) {
		this.csoBridgeHttp = csoBridgeHttp;
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
