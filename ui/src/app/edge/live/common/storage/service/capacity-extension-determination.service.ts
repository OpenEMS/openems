import { computed, effect, inject, Injectable, signal } from "@angular/core";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";

export interface CapacityExtensionState {
    essId: string;
    controllerId: string;
    isRunning: boolean;
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

        const channelAddresses = controllers.map(
            (controller) => new ChannelAddress(controller.id, "_PropertyIsRunning"),
        );

        if (channelAddresses.length > 0) {
            this.dataService.subscribeChannels(channelAddresses, edge);
        }
    }

    public setCapacityExtensionStates(currentData: CurrentData): void {
        const newStates = new Map<string, CapacityExtensionState>();

        for (const controller of this.trackedControllers()) {
            const isRunning = Number(currentData.allComponents[controller.controllerId + "/_PropertyIsRunning"]) === 1;

            newStates.set(controller.essId, {
                essId: controller.essId,
                controllerId: controller.controllerId,
                isRunning,
            });
        }

        this.capacityExtensionStates.set(newStates);
    }
}
