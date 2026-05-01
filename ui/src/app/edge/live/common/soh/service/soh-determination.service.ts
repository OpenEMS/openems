import { computed, effect, inject, Injectable, Injector, OnDestroy, Signal, signal } from "@angular/core";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";
import de from "../i18n/de.json";
import en from "../i18n/en.json";

enum SohCycleStateMachine {
    IDLE = 1,
    PREPARE = 2,
    REFERENCE_CYCLE_CHARGING = 3,
    REFERENCE_CYCLE_CHARGING_WAIT = 4,
    REFERENCE_CYCLE_DISCHARGING = 5,
    REFERENCE_CYCLE_DISCHARGING_WAIT = 6,
    MEASUREMENT_CYCLE_CHARGING = 7,
    MEASUREMENT_CYCLE_CHARGING_WAIT = 8,
    CHECK_BALANCING = 9,
    MEASUREMENT_CYCLE_DISCHARGING = 10,
    MEASUREMENT_CYCLE_DISCHARGING_WAIT = 11,
    EVALUATE_RESULT = 12,
    DONE = 13,
    ERROR_ABORT = 14,
}

export interface SohCycleState {
    essId: string;
    controllerId: string;
    isRunning: boolean;
    stateMachine: number | null;
    hasError: boolean;
    sohPercent: number | null;
    measuredCapacity: number | null;
    isBatteryBalanced: boolean;
    isMeasured: boolean;
}

@Injectable({
    providedIn: "root",
})
export class SohDeterminationService implements OnDestroy {

    public anySohCycleRunningWithoutError = computed(() => {
        return Array.from(this.sohStates().values()).some(state => state.isRunning && !state.hasError);
    });

    public anySohCycleRunningWithError = computed(() => {
        return Array.from(this.sohStates().values()).some(state => state.isRunning && state.hasError);
    });

    private readonly sohStates = signal<Map<string, SohCycleState>>(new Map());
    private readonly runningSignals = new Map<string, Signal<boolean>>();
    private readonly runningWithoutErrorSignals = new Map<string, Signal<boolean>>();
    private readonly runningWithErrorSignals = new Map<string, Signal<boolean>>();
    private readonly capacityMeasuredSignals = new Map<string, Signal<boolean>>();
    private readonly stopOnDestroy = new Subject<void>();

    private readonly translate = inject(TranslateService);
    private readonly dataService = inject(DataService);
    private readonly injector = inject(Injector);
    private readonly service = inject(Service);

    constructor() {
        Language.normalizeAdditionalTranslationFiles({ de, en }).then((translations) => {
            for (const { lang, translation, shouldMerge } of translations) {
                this.translate.setTranslation(lang, translation, shouldMerge);
            }
        });

        effect(() => {
            const val = this.dataService.currentValue();
            this.setSohStates(val);
        }, { injector: this.injector });
    }

    /**
     * Initializes SoH cycle state tracking for the given edge and config
     */
    public initializeSohTracking(config: EdgeConfig | null, edge: Edge | null): void {

        if (config == null || edge == null) {
            return;
        }

        const essSohCycleCtrl = (config.getComponentsByFactory("Controller.Ess.SoH.Cycle") ?? [])
            .filter(component => component.isEnabled);

        if (essSohCycleCtrl.length === 0) {
            return;
        }

        const channelAddresses: ChannelAddress[] = [];
        for (const controller of essSohCycleCtrl) {
            channelAddresses.push(
                new ChannelAddress(controller.id, "_PropertyEnabled"),
                new ChannelAddress(controller.id, "_PropertyIsRunning"),
                new ChannelAddress(controller.id, "StateMachine"),
                new ChannelAddress(controller.id, "SohPercent"),
                new ChannelAddress(controller.id, "MeasuredCapacity"),
                new ChannelAddress(controller.id, "IsBatteryBalanced"),
                new ChannelAddress(controller.id, "IsMeasured"),
            );
        }

        this.dataService.subscribeChannels(channelAddresses, edge);
    }

    public async setSohStates(currentData: CurrentData) {
        const config = await this.service.getConfig();
        const essSohCycleCtrl = (config.getComponentsByFactory("Controller.Ess.SoH.Cycle") ?? [])
            .filter(component => component.isEnabled);
        const newStates = new Map<string, SohCycleState>();

        for (const controller of essSohCycleCtrl) {
            const essId = controller.properties?.["ess.id"] || controller.id;
            const stateMachine = currentData.allComponents[controller.id + "/StateMachine"];

            newStates.set(essId, {
                essId: essId,
                controllerId: controller.id,
                isRunning: currentData.allComponents[controller.id + "/_PropertyIsRunning"] === 1,
                stateMachine: stateMachine,
                hasError: stateMachine === SohCycleStateMachine.ERROR_ABORT,
                sohPercent: currentData.allComponents[controller.id + "/SohPercent"],
                measuredCapacity: currentData.allComponents[controller.id + "/MeasuredCapacity"],
                isBatteryBalanced: Number(currentData.allComponents[controller.id + "/IsBatteryBalanced"]) === 1,
                isMeasured: Number(currentData.allComponents[controller.id + "/IsMeasured"]) === 1,
            });
        }

        this.sohStates.set(newStates);
    }

    public getIsSohCycleRunningSignal(essId: string): Signal<boolean> {
        return this.getOrCreateSignal(
            essId,
            this.runningSignals,
            false,
            (state) => state?.isRunning ?? false,
        );
    }

    public getIsSohCycleRunningWithoutErrorSignal(essId: string): Signal<boolean> {
        return this.getOrCreateSignal(
            essId,
            this.runningWithoutErrorSignals,
            false,
            (state) => (state?.isRunning ?? false) && !state?.hasError,
        );
    }

    public getIsSohCycleRunningWithErrorSignal(essId: string): Signal<boolean> {
        return this.getOrCreateSignal(
            essId,
            this.runningWithErrorSignals,
            false,
            (state) => (state?.isRunning ?? false) && (state?.hasError ?? false),
        );
    }

    public getIsSohCycleCapacityMeasuredSignal(essId: string): Signal<boolean> {
        return this.getOrCreateSignal(
            essId,
            this.capacityMeasuredSignals,
            false,
            (state) => {
                if (!state) {
                    return false;
                }
                return state.isMeasured || (state.measuredCapacity ?? 0) > 0;
            },
        );
    }

    public getStateMachineName(value: number | null | undefined): string {
        if (value == null) {
            return "UNKNOWN";
        }
        return SohCycleStateMachine[value] || "UNKNOWN";
    }

    ngOnDestroy(): void {
        this.runningSignals.clear();
        this.runningWithoutErrorSignals.clear();
        this.runningWithErrorSignals.clear();
        this.capacityMeasuredSignals.clear();
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    /**
     * Generic helper to create or retrieve a cached signal derived from SoH state
     */
    private getOrCreateSignal<T>(
        essId: string,
        cache: Map<string, Signal<T>>,
        defaultValue: T,
        derive: (state: SohCycleState | undefined) => T,
    ): Signal<T> {
        if (essId === "") {
            return computed(() => defaultValue);
        }

        let signal = cache.get(essId);
        if (!signal) {
            signal = computed(() => derive(this.sohStates().get(essId)));
            cache.set(essId, signal);
        }
        return signal;
    }

}
