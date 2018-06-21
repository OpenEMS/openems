package io.openems.edge.ess.streetscooter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.ess.api.Ess;

@Designate(ocd = ConfigCluster.class, factory = true)
@Component(name = "Ess.Streetscooter.Cluster", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC
+ "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)
public class EssCluster extends AbstractOpenemsComponent implements Ess, OpenemsComponent {

	protected final Logger log = LoggerFactory.getLogger(EssCluster.class);
	
	@Reference
	private ConfigurationAdmin cm;
	private ComponentContext context;

	private Collection<AbstractEssStreetscooter> streetscooters = new ArrayList<>(); // TODO fill this list
	
	public EssCluster() {
		super();
		Utils.initializeClusterChannels(this).forEach(channel -> this.addChannel((Channel<?>) channel));
	}

	@Activate
	protected
	void activate(ComponentContext context, ConfigCluster configCluster) {
		super.activate(context, configCluster.service_pid(), configCluster.id(), configCluster.enabled());
		this.context = context;
		
		Enumeration<String> keys = this.context.getProperties().keys();
		StringBuffer sb = new StringBuffer();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			sb.append("Key: ");
			sb.append(key);
			sb.append("; value: ");
			sb.append(context.getProperties().get(key));
			sb.append("\n");
		}
		log.debug(sb.toString());
	}
	
	@Override
	public String debugLog() {
		return "System SoC:" + this.getSoc().value().asString() + "; " + streetscooters.size() + " single systems representing this cluster";
	}
	
	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		SYSTEM_STATE(new Doc().unit(Unit.NONE)), //
		CAPCACITY(new Doc().unit(Unit.WATT_HOURS)), // TODO Interface EssCluster?
		;
		
		private final Doc doc;

		@Override
		public Doc doc() {
			return this.doc;
		}

		private ChannelId(Doc doc) {
			this.doc = doc;
		}
	}
}
