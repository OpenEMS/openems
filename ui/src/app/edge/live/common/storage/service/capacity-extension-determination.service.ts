import { computed, effect, inject, Injectable, signal } from "@angular/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";

const AT_TARGET_SOC = 5;

export interface CapacityExtensionState {
    essId: string;
    controllerId: string;
    isRunning: boolean;
    stateMachine: number | null;
}

interface TrackedController {
    essId: string;
    controllerId: string;
}

@Injectable({
    providedIn: "root",
})
export class CapacityExtensionDeterminationService {
    public readonly anyCapacityExtensionRunning = computed(() =>
        Array.from(this.capacityExtensionStates().values()).some((state) => state.isRunning),
    );

    public readonly anyExpansionAvailable = computed(() =>
        Array.from(this.capacityExtensionStates().values()).some(
            (state) => state.isRunning && state.stateMachine === AT_TARGET_SOC,
        ),
    );

    private readonly dataService = inject(DataService);

    private readonly capacityExtensionStates = signal<Map<string, CapacityExtensionState>>(new Map());

    private readonly trackedControllers = signal<TrackedController[]>([]);

    constructor() {
        effect(() => {
            const currentData = this.dataService.currentValue();
            this.setCapacityExtensionStates(currentData);
        });
    }

    /** Initializes capacity extension tracking for the given edge and config. */
    public initializeCapacityExtensionTracking(config: EdgeConfig | null, edge: Edge | null): void {
        if (config == null || edge == null) {
            return;
        }

        const controllers = (config.getComponentsByFactory("Controller.Ess.PrepareBatteryExtension") ?? []).filter(
            (component) => component.isEnabled,
        );

        this.trackedControllers.set(
            controllers.map((controller) => ({
                essId: controller.properties?.["ess.id"] || controller.id,
                controllerId: controller.id,
            })),
        );

        const channelAddresses = controllers.flatMap((controller) => [
            new ChannelAddress(controller.id, "_PropertyIsRunning"),
            new ChannelAddress(controller.id, "StateMachine"),
        ]);

        if (channelAddresses.length > 0) {
            this.dataService.subscribeChannels(channelAddresses, edge);
        }
    }

    public setCapacityExtensionStates(currentData: CurrentData): void {
        const newStates = new Map<string, CapacityExtensionState>();

        for (const controller of this.trackedControllers()) {
            const prefix = controller.controllerId + "/";

            const isRunning = Number(currentData.allComponents[prefix + "_PropertyIsRunning"]) === 1;

            const rawStateMachine = currentData.allComponents[prefix + "StateMachine"];

            const stateMachine = rawStateMachine == null ? null : Number(rawStateMachine);

            newStates.set(controller.essId, {
                essId: controller.essId,
                controllerId: controller.controllerId,
                isRunning,
                stateMachine,
            });
        }

        this.capacityExtensionStates.set(newStates);
    }
}
