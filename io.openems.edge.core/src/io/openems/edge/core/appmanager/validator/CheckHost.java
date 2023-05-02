package io.openems.edge.core.appmanager.validator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.session.Language;
import io.openems.common.utils.InetAddressUtils;

@Component(//
		name = CheckHost.COMPONENT_NAME, //
		scope = ServiceScope.PROTOTYPE //
)
public class CheckHost extends AbstractCheckable implements Checkable {

	public static final String COMPONENT_NAME = "Validator.Checkable.CheckHost";

	private InetAddress host;
	private Integer port;

	@Activate
	public CheckHost(ComponentContext componentContext) {
		super(componentContext);
	}

	private void init(String host, Integer port) {
		this.host = InetAddressUtils.parseOrNull(host);
		this.port = port;
	}

	@Override
	public void setProperties(Map<String, ?> properties) {
		var host = (String) properties.get("host");
		var port = (Integer) properties.get("port");
		this.init(host, port);
	}

	@Override
	public boolean check() {
		if (this.host == null) {
			return false;
		}
		if (this.port == null) {
			try {
				return this.host.isReachable(1000);
			} catch (IOException e) {
				// not reachable
			}
		} else {
			try {
				// try socket connection on specific port
				var so = new Socket(this.host, this.port);
				so.close();
				return true;
			} catch (IOException e) {
				// not reachable
			}
		}
		return false;
	}

	@Override
	public String getErrorMessage(Language language) {
		var address = this.host.getHostAddress();
		if (this.port != null) {
			address += ":" + this.port;
		}
		if (this.host == null) {
			return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckHost.WrongIp", address);
		}
		return AbstractCheckable.getTranslation(language, "Validator.Checkable.CheckHost.NotReachable", address);
	}

}
