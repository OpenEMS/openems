package io.openems.edge.ess.streetscooter;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.types.OpenemsType;
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

	String[] essIds = new String[0];
	// needs to be thread safe?
	private Collection<Ess> esses = new CopyOnWriteArrayList<>();
//	private final AverageInteger<Ess> essSoc;
//	private Map<String, OpenemsComponent> scooterEssMap = new ConcurrentHashMap<>();
	
	public EssCluster() {
		super();
		Utils.initializeClusterChannels(this).forEach(channel -> this.addChannel((Channel<?>) channel));
//		this.essSoc = new AverageInteger<Ess>(this, ChannelId.ESS_SOC, Ess.ChannelId.SOC);
	}

	@Activate
	protected
	void activate(ComponentContext context, ConfigCluster configCluster) {
		if (OpenemsComponent.updateReferenceFilter(this.cm, configCluster.service_pid(), "ess", configCluster.ess_ids())) {
			return;
		}

		this.essIds = configCluster.ess_ids();	
		super.activate(context, configCluster.service_pid(), configCluster.id(), configCluster.enabled());
		this.context = context;
	}
	
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	void addEss(Ess ess) {
		esses.add(ess);
//		essSoc.addComponent(ess);
	}

	void removeEss(Ess ess) {
		esses.remove(ess);
//		essSoc.removeComponent(ess);
	}
	

	@Override
	public String debugLog() {
		return "System SoC calculated:" + calculateSoC() + "; " + esses.size() + " single systems representing this cluster";// system health calculated: " + calculateSoH();
	}
	
	private int calculateSoC() {
		return getEssSoc().value().orElse(0);
	}
	
	public Channel<Integer> getEssSoc() {
		return this.channel(ChannelId.ESS_SOC);
	}
	
	
	
	@Override
	public Channel<Integer> getSoc() {
		return getEssSoc();
	}

	// TODO.. not fine...
	public GridMode readGridMode() {
		boolean onGrid = true;
		boolean offGrid = true;
		
		for (Ess e : esses) {
			if (e.getGridMode().getNextValue().equals(GridMode.OFF_GRID)) {
				onGrid = false;
			}
			if (e.getGridMode().getNextValue().equals(GridMode.ON_GRID)) {
				offGrid = false;
			}
		}
		if (onGrid) {
			return GridMode.ON_GRID;
		}
		if (offGrid) {
			return GridMode.OFF_GRID;
		}
		return GridMode.UNDEFINED;
	}


	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {		
		SYSTEM_STATE(new Doc().unit(Unit.NONE)), //
		CAPCACITY(new Doc().unit(Unit.WATT_HOURS)), // TODO Interface EssCluster?
		ESS_SOC(new Doc().type(OpenemsType.INTEGER).unit(Unit.PERCENT)),
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
