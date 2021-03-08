package io.openems.edge.ess.sinexcel.statemachine;

import java.util.Optional;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.ess.sinexcel.Config;
import io.openems.edge.ess.sinexcel.EssSinexcel;
import io.openems.edge.ess.sinexcel.EssSinexcelImpl;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine.State;

public class Context extends AbstractContext<EssSinexcel>{
	protected final EssSinexcelImpl component;
	protected final ComponentManager componentManager;
	protected final Battery battery;
	protected final Config config;
	protected boolean startOnce = false;

	public boolean isStartOnce() {
		return startOnce;
	}

	public void setStartOnce(boolean startOnce) {
		this.startOnce = startOnce;
	}

	protected Optional<Boolean> mainContactor;
	protected Optional<Boolean> gridDetector;
	protected Optional<Boolean> grounding;

	public Context(EssSinexcelImpl component,  ComponentManager componentManager, Battery battery, Config config) {
		super(component);
		this.component = component;
		this.componentManager = componentManager;
		this.battery = battery;
		this.config = config;
	}

	protected State stateTransitionHelper() throws IllegalArgumentException, OpenemsNamedException {
		mainContactor = getMainContactor();
		gridDetector = getGridDetector();
		grounding = getGrounding();

		if (mainContactor.isPresent() && gridDetector.isPresent() && grounding.isPresent()) {
			State s = getStateFromInputs(mainContactor.get(), gridDetector.get(), grounding.get());
			//System.out.println(s);
			return s;
		} else {
			return State.UNDEFINED;
		}
	}

	protected Optional<Boolean> getMainContactor() throws IllegalArgumentException, OpenemsNamedException {
		BooleanReadChannel inChannel1 = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.digitalInput1()));
		return inChannel1.value().asOptional();

	}

	protected Optional<Boolean> getGridDetector() throws IllegalArgumentException, OpenemsNamedException {
		BooleanReadChannel inChannel2 = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.digitalInput2()));
		return inChannel2.value().asOptional();
	}

	protected Optional<Boolean> getGrounding() throws IllegalArgumentException, OpenemsNamedException {
		BooleanReadChannel inChannel3 = this.componentManager
				.getChannel(ChannelAddress.fromString(this.config.digitalInput3()));
		return inChannel3.value().asOptional();
	}

	protected static State getStateFromInputs(boolean mainContactor, boolean gridDetector, boolean grounding) {

//		System.out.println("grid detector : " + gridDetector);
//		System.out.println("main : " + mainContactor);
//		System.out.println("ground : " + grounding);
		
		
		if (gridDetector) {
			// off grid
			if (mainContactor) {
				// main is true, maincontactor is open
				if (grounding) {
					// ground is true , grounding is closed
					// 1 1 1
					// error
					//System.out.println(" one " + State.ERROR);
					return State.ERROR;
				}else {
					// 1 1 0
					//total offgrid
					//System.out.println(" one " + State.TOTAL_OFFGRID);
					return State.TOTAL_OFFGRID;
				}
			}else {
				// main is false, main contactor is cloased
				if (grounding) {
					// 0 1 1
					// We are going to off grid
					//System.out.println(" one " + State.GROUNDSET);
					return State.GROUNDSET;
				}else {
					//0 1 0
					//System.out.println(" one " + State.ERROR);
					return State.GROUNDSET;
				}				
			}			
		}else {
			// on grid
			if (mainContactor) {
				if (grounding) {
					//  1 0 1
					//System.out.println(" one " + State.GROUNDSET);
					return State.GROUNDSET;
				}else {
					// 0 1 0
					//System.out.println(" one " + State.ERROR);
					return State.GROUNDSET;
				}
				
			}else {
				if (grounding) {
					//  0 0 1
					//System.out.println(" one " + State.TOTAL_ONGRID);
					return State.TOTAL_ONGRID;
				}else {
					//  0 0 0
					//System.out.println(" one " + State.GROUNDSET);
					return State.GROUNDSET;
					
				}
				
			}
		}
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		

//		if (gridDetector) {
//			// gridDetector = true
//			if (!mainContactor && !grounding) {
//				// mainContactor = true, grounding = true
//				System.out.println(" one " + State.TOTAL_OFFGRID);
//				return State.TOTAL_OFFGRID;
//			} else if (mainContactor && !grounding) {
//				// mainContactor = true, grounding = false
//				System.out.println(" two " + State.GROUNDSET);
//				return State.GROUNDSET;
//			} else {
//				System.out.println(" three " + State.UNDEFINED);
//				return State.UNDEFINED;
//			}
//		} else {
//			// gridDetector = false
//			if (mainContactor && grounding) {
//				System.out.println(" four " + State.TOTAL_ONGRID);
//				// mainContactor = false, grounding = false
//				return State.TOTAL_ONGRID;
//			} else if (!mainContactor && grounding) {
//				System.out.println(" five " + State.GROUNDSET);
//				// mainContactor = false, grounding = true
//				return State.GROUNDSET;
//			} else if (mainContactor && !grounding) {
//				// mainContactor = true, grounding = false
//				System.out.println(" six " + State.TOTAL_ONGRID);
//				return State.TOTAL_ONGRID;
//			} else {
//				System.out.println(" seven " + State.UNDEFINED);
//				return State.UNDEFINED;
//			}
//
//		}

	}

//	protected static State getStateFromInputs(boolean mainContactor, boolean gridDetector, boolean grounding) {
//		if (mainContactor) {
//			if (gridDetector) {
//				if (grounding) {
//					// 1 1 1
//					return State.TOTAL_OFFGRID;
//				} else {
//					// 1 1 0
//					return State.GROUNDSET;
//				}
//			} else {
//				if (grounding) {
//					// 1 0 1
//					return State.TRANSITION_OFF_TO_ON;
//				} else {
//					// 1 0 0
//					return State.GOING_ONGRID;
//				}
//			}
//		} else {
//			if (gridDetector) {
//				if (grounding) {
//					// 0 1 1
//					return State.ERROR_ONGRID;
//				} else {
//					// 0 1 0
//					return State.TRANSITION_ON_TO_OFF;
//				}
//			} else {
//				if (grounding) {
//					// 0 0 1
//					return State.GROUNDSET;
//				} else {
//					// 0 0 0
//					return State.TOTAL_ONGRID;
//				}
//			}
//		}
//	}

}
