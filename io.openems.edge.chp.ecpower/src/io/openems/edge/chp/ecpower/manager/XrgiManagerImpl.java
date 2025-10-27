package io.openems.edge.chp.ecpower.manager;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.chp.ecpower.control.XrgiControl;
import io.openems.edge.chp.ecpower.ro.XrgiRo;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.generator.api.ManagedSymmetricGenerator;
import io.openems.edge.generator.api.SymmetricGenerator;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "CHP.ECcpower.manager", //		
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE })
public class XrgiManagerImpl extends AbstractOpenemsComponent implements XrgiManager, ManagedSymmetricGenerator, SymmetricGenerator, OpenemsComponent, EventHandler {

	@Reference
	private ConfigurationAdmin cm;

	private final Logger log = LoggerFactory.getLogger(XrgiManagerImpl.class);

    // Eine (!) MULTIPLE-Referenz auf XrgiRo
    @Reference(
            name = "XrgiRo",
            service = XrgiRo.class,
            policy = ReferencePolicy.DYNAMIC,
            policyOption = ReferencePolicyOption.GREEDY,
            cardinality = ReferenceCardinality.MULTIPLE,
            bind = "bindRo",
            unbind = "unbindRo"
    )
    private final Map<String, XrgiRo> xrgiRos = new ConcurrentHashMap<>();
	
	@Reference(name = "XrgiControl", policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile XrgiControl xrgiControl = null;

	private Config config = null;
	
	private State state = State.UNDEFINED;	

	public XrgiManagerImpl() {
		super(
				OpenemsComponent.ChannelId.values(), 
				//ElectricityMeter.ChannelId.values(),
				ManagedSymmetricGenerator.ChannelId.values(),
				SymmetricGenerator.ChannelId.values(),
				XrgiManager.ChannelId.values()

		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

        // IDs from config. Only non-empty (distinct)
        List<String> ids = Stream.of(
                        config.xrgiRo0_id(),
                        config.xrgiRo1_id(),
                        config.xrgiRo2_id(),
                        config.xrgiRo3_id())
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());

        // 
        if (!ids.isEmpty()) {
            // 
            OpenemsComponent.updateReferenceFilter(
            	    this.cm, this.servicePid(), "XrgiRo",
            	    ids.toArray(new String[0]) // z.B. {"xrgiRo0","xrgiRo1"}
            	);
            	if (config.debugMode()) log.info("[XrgiManager] XrgiRo targets = {}", ids);
        } else {
            // Nichts binden: Filter, der garantiert nichts matched
            OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "XrgiRo", "(!(id=*))");
            if (config.debugMode()) log.info("[XrgiManager] XrgiRo target filter = (!(id=*))");
        }	

		if (!config.xrgiControl_id().isEmpty()) {
			OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "XrgiControl", config.xrgiControl_id());
		}

		this.config = config;
	}

	
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
        xrgiRos.clear();
        state = State.UNDEFINED;		
	}

    @Override
    public void applyPreparation(Boolean activate) {
        if (this.xrgiControl == null || xrgiRos.isEmpty()) {
            log.warn("[XrgiManager] XrgiControl or XrgiRo missing (control={}, roCount={}) – skip Preparation",
                    xrgiControl != null, xrgiRos.size());
            return;
        }
        this.xrgiControl.applyPreparation(activate);
    }	
	
	
    @Override
    public void applyPower(int activePowerTarget) {
        this.applyPower((Integer) activePowerTarget);
    }

    @Override
    public void applyPower(Integer activePowerTarget) {
        if (this.xrgiControl == null || xrgiRos.isEmpty()) {
            log.warn("[XrgiManager] XrgiControl or XrgiRo missing (control={}, roCount={}) – skip applyPower",
                    xrgiControl != null, xrgiRos.size());
            return;
        }
        this.xrgiControl.applyPower(activePowerTarget);
    }

	@Override
	public void handleEvent(Event event) {

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			//this._setActivePower(this.xrgiRo.getActivePower().get());
			//this.applyPower(19000);
			this.updateGeneratorPowerSum();
			this.updateReadyForOperation();
			this.updateBufferTankTemperature();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			this.checkState();
			break;

		}
	}
	
	public boolean awaitingStepTransitionHysteresis() {
		if (this.state ==  State.AWAITING_HYSTERESIS ) {
			return true;
		} else {
			return false;
		}
	}
	
    public void checkState() {
        if (xrgiRos.isEmpty() || this.xrgiControl == null) {
            this.state = State.ERROR;
        } else {
            this.state = State.NORMAL;
        }
    }
	
	
    // ===== DS bind/unbind für MULTIPLE =====
    void bindRo(XrgiRo ro) {
        if (ro == null) return;
        xrgiRos.put(ro.id(), ro);
        if (config != null && config.debugMode()) {
            log.info("[XrgiManager] bound {}", ro.id());
        }
    }

    void unbindRo(XrgiRo ro) {
        if (ro == null) return;
        xrgiRos.remove(ro.id());
        if (config != null && config.debugMode()) {
            log.info("[XrgiManager] unbound {}", ro.id());
        }
    }	
	
	

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

    @Override
    public String debugLog() {
        if (xrgiRos.isEmpty()) {
            log.warn("[XrgiManager] No XRGI bound");
            return "NO XRGI";
        }
        if (this.xrgiControl == null) {
            log.warn("[XrgiManager] xrgiControl not set");
            return "NO xrgiControl";
        }

        String perUnit = xrgiRos.keySet().stream().sorted()
                .map(id -> "Power " + id + ": " + xrgiRos.get(id).getActivePower().asString())
                .collect(Collectors.joining("\n"));

        int total = xrgiRos.values().stream()
                .mapToInt(ro -> ro.getActivePower().orElse(0))
                .sum();

        return "\n" + perUnit + "\nTotal Power: " + total;
    }

    private void updateGeneratorPowerSum() {
        int total = xrgiRos.values().stream()
                .mapToInt(ro -> ro.getActivePower().orElse(0))
                .sum();
        this._setGeneratorActivePower(total);
    }
    
    public void updateBufferTankTemperature() {
    	// get average temperature over all CHPs
    	int avgRounded = (int) Math.round(
    		    xrgiRos.values().stream()
    		        .map(XrgiRo.class::cast)
    		        .map(ro -> ro.getBufferTankTemperature().asOptional())
    		        .flatMap(Optional::stream)
    		        .mapToInt(Integer::intValue)
    		        .average()
    		        .orElse(0.0)
    		);

    	this._setAverageBufferTankTemperature(avgRounded);

    }
    
    public void updateReadyForOperation() {

    	// at least one is ready
    	boolean anyReady = xrgiRos.values().stream()
    		    .map(XrgiRo.class::cast)
    		    .map(ro -> ro.getReadyForOperation().asOptional())
    		    .flatMap(Optional::stream)
    		    .anyMatch(Boolean::booleanValue);
    	
    	// at least one is NOT locked
    	boolean anyUnlocked = xrgiRos.values().stream()
    		    .map(XrgiRo.class::cast)
    		    .map(ro -> ro.getNotReadyForOperation().asOptional())
    		    .flatMap(Optional::stream)
    		    .anyMatch(b -> !b);
    	
    	// at least one is operating
    	boolean anyOperating = xrgiRos.values().stream()
    		    .map(XrgiRo.class::cast)
    		    .map(ro -> ro.getIsOperating().asOptional())
    		    .flatMap(Optional::stream)
    		    .anyMatch(Boolean::booleanValue);    	

    	if (anyReady == true || anyUnlocked == true || anyOperating == true) {
    		this._setReadyForOperation(true);	
    	} else {
    		this._setReadyForOperation(false);
    	}
    	
    	

    }    

    // Exponiert den Summen-Channel als Value<Integer>
//    @Override
    //public Value<Integer> getGeneratorActivePower() {
        //return this.getGeneratorActivePower();
    //}

}
