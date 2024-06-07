package io.openems.edge.evcs.keba.kecontact.state;

import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContact;
import io.openems.edge.evcs.keba.kecontact.EvcsKebaKeContactImpl;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.evcs.api.Phases;
import java.time.Instant;
import java.time.Duration;

public class PhaseSwitchHandler {
    private State currentState;
    private State inaktivState;
    private State runningOnePhaseState;
    private State runningThreePhaseState;
    private State switchToOnePhaseState;
    private State switchToThreePhaseState;
    private EvcsKebaKeContactImpl context;
    private Instant lastPhaseChangeTime = Instant.MIN;
    private static final long PHASE_SWITCH_COOLDOWN_SECONDS = 310;

    public PhaseSwitchHandler(EvcsKebaKeContactImpl context) {
        this.context = context;
        this.inaktivState = new Inaktiv();
        this.runningOnePhaseState = new RunningOnePhase();
        this.runningThreePhaseState = new RunningThreePhase();
        this.switchToOnePhaseState = new SwitchToOnePhase();
        this.switchToThreePhaseState = new SwitchToThreePhase();
        this.currentState = inaktivState; // Initial state
    }

    public void applyPower(int power) {
        if (isCooldownPeriod()) {
            Duration timeUntilNextSwitch = Duration.between(Instant.now(),
                this.lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS));
            long secondsUntilNextSwitch = timeUntilNextSwitch.getSeconds();
            context.channel(EvcsKebaKeContact.ChannelId.PHASE_SWITCH_COOLDOWN).setNextValue(secondsUntilNextSwitch);
            context.log.info("Phase switch cooldown period has not passed. Time before next switch: "
                + secondsUntilNextSwitch + " seconds.");
        } else {
            final var phases = context.getPhases();
            IntegerReadChannel maxCurrentCannel = context.channel(EvcsKebaKeContact.ChannelId.DIP_SWITCH_MAX_HW);
            var preferredPhases = Phases.preferredPhaseBehavior(power, phases, context.getMinHwCurrent(),
                maxCurrentCannel.value().orElse(EvcsKebaKeContactImpl.DEFAULT_MAXIMUM_HARDWARE_CURRENT));
            
            if (phases != preferredPhases) {
                currentState = (preferredPhases == Phases.ONE_PHASE) ? switchToOnePhaseState : switchToThreePhaseState;
                currentState.switchPhase(context);
            } else {
                currentState.handlePower(power, context);
            }
        }
    }

    public void switchPhase() {
        currentState.switchPhase(this.context);
    }

    public void setState(State state) {
        this.currentState = state;
    }

    public State getSwitchToOnePhaseState() {
        return switchToOnePhaseState;
    }

    public State getSwitchToThreePhaseState() {
        return switchToThreePhaseState;
    }

    public State getRunningOnePhaseState() {
        return runningOnePhaseState;
    }

    public State getRunningThreePhaseState() {
        return runningThreePhaseState;
    }

    public State getInaktivState() {
        return inaktivState;
    }

    private boolean isCooldownPeriod() {
        Instant now = Instant.now();
        return lastPhaseChangeTime.plusSeconds(PHASE_SWITCH_COOLDOWN_SECONDS).isAfter(now);
    }

    public void updatePhaseChangeTime() {
        this.lastPhaseChangeTime = Instant.now();
    }

    public void handlePhaseSwitch(Phases preferredPhases) {
        if (preferredPhases == Phases.TWO_PHASE) {
            // Set KEBA to two phases is not possible
            preferredPhases = Phases.THREE_PHASE;
        }
        String command = preferredPhases == Phases.ONE_PHASE ? "x2 0" : "x2 1";
        if (context.send(command)) {
            updatePhaseChangeTime(); // Update the cooldown timer regardless of the phase switch direction
            context.log.info("Switched to " + (preferredPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + " successfully.");
        } else {
            context.log.warn("Failed to switch to " + (preferredPhases == Phases.ONE_PHASE ? "1 phase" : "3 phases") + ".");
        }
    }
}
