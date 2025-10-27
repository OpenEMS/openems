// @ts-strict-ignore
import { formatNumber } from "@angular/common";
import { Component, Input, OnInit } from "@angular/core";
import { Router } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { filter, take } from "rxjs/operators";
import { UnitvaluePipe } from "src/app/shared/pipe/unitvalue/unitvalue.pipe";
import { Currency, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";
import { Language } from "src/app/shared/type/language";

type mode = "MANUAL_ON" | "MANUAL_OFF" | "AUTOMATIC";

enum ChpState {
    UNDEFINED = -1,                     // Undefined state
    NORMAL = 0,                         // Controller is inactive and waiting
    ERROR = 1,                          // Error State
    CHP_ACTIVE = 2,                     // CHP running
    CHP_INACTIVE = 3,                   // CHP stopping
    CHP_PREPARING = 4,                  // CHP preparing (wait for lower temperatures)
    IDLE = 5,                           // grid consumption too low
    OVER_TEMPERATURE = 6,               // buffer tank temperature too high
    CHP_NOT_READY = 7,                  // CHPs not ready due to hardware locks
}

enum HysteresisState {
    UNDEFINED = -1,                  // Undefined state
    INACTIVE = 0,                    // Controller is inactive and waiting
    ACTIVE = 1,                      // Error State

}

@Component({
    selector: Controller_ChpCostOptimizationModalComponent.SELECTOR,
    templateUrl: "./modal.component.html",
    standalone: false,
})
export class Controller_ChpCostOptimizationModalComponent implements OnInit {


    private static readonly SELECTOR = "ChpCostOptimization-modal";

    @Input({ required: true }) public edge!: Edge;
    @Input({ required: true }) public component!: EdgeConfig.Component;

    public threshold: number = null; // %- from ion component
    protected meta: EdgeConfig.Component = null;
    protected currency: string = null;
    private currencyLabel: Currency.Label; // Default
    private currencyUnit: Currency.Unit; // Default
    private locale: string = "-";
    private unitpipe: UnitvaluePipe;
    private highCostsThreshold: number;
    private priceThreshold: number;
    // Variable to hold the current state of the PeakShavingState
    private currentState: ChpState = ChpState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingStartHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingPreparationHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingRunHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingTransitionHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingDeviceHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED
    private currentAwaitingReducedPowerHysteresis: HysteresisState = HysteresisState.UNDEFINED; // Default value is UNDEFINED

    constructor(
        unitpipe: UnitvaluePipe,
        public service: Service,
        public websocket: Websocket,
        public router: Router,
        public translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    get costsWithLabel(): string {
        if (this.highCostsThreshold == null) { return "-"; }
        return formatNumber(this.priceThreshold, this.locale, "1.0-2") + " " + this.currency + "/h";
    }

    ngOnInit() {
        this.threshold = this.component.properties["priceThreshold"];
        this.edge.currentData.subscribe((currentData) => {
            this.setCurrentStateFromData(currentData);
            console.log("Current Data:", currentData); // Check what data is coming in
        });

        this.edge.getConfig(this.websocket)
            .pipe(filter(config => !!config), take(1))
            .subscribe(config => {
                const meta: EdgeConfig.Component = config?.getComponent("_meta");
                this.currency = config?.getPropertyFromComponent<string>(meta, "currency");
                this.currencyLabel = Currency.getCurrencyLabelByCurrency(this.currency);
                this.currencyUnit = Currency.getChartCurrencyUnitLabel(this.currency);

            });
        this.locale = (Language.getByKey(localStorage.LANGUAGE) ?? Language.DEFAULT).i18nLocaleKey;
    }

    formatCosts(value: number | null | undefined): string {
        if (value == null || isNaN(value)) { return "-"; }
        return formatNumber(value, this.locale, "1.0-2") + " " + this.currency + "/h";
    }

    formatPrice(value: number | null | undefined): string {
        if (value == null || isNaN(value)) { return "-"; }
        return formatNumber(value, this.locale, "1.0-0") + " " + this.currency + "/MWh";
    }

    setCurrentStateFromData(currentData): void {
        // Get the latest value from the BehaviorSubject
        //const currentData = this.edge.currentData.value;

        // Check if currentData and channel exist
        if (!currentData || !currentData.channel) {
            console.warn("currentData or currentData.channel is undefined");
            return;
        }

        const controllerId = this.component?.id;
        if (!controllerId) {
            console.warn("controllerId is undefined");
            return;
        }

        // Safely access the PeakShavingStateMachine channel
        const currentStateValue = currentData.channel[`${controllerId}/StateMachine`];
        const currentAwaitingStartHysteresisValue = currentData.channel[`${controllerId}/AwaitingStartHysteresis`];
        const currentAwaitingPreparationHysteresisValue = currentData.channel[`${controllerId}/AwaitingPreparationHysteresis`];
        const currentAwaitingRunHysteresisValue = currentData.channel[`${controllerId}/AwaitingRunHysteresis`];
        const currentAwaitingTransitionHysteresisValue = currentData.channel[`${controllerId}/AwaitingTransitionHysteresis`];
        const currentAwaitingDeviceHysteresisValue = currentData.channel[`${controllerId}/AwaitingDeviceHysteresis`];
        const currentAwaitingReducedPowerHysteresisValue = currentData.channel[`${controllerId}/AwaitingReducedPowerHysteresis`];

        // Check if currentStateValue is not undefined or null and is a valid number
        if (isNaN(currentStateValue) || isNaN(currentAwaitingStartHysteresisValue) || isNaN(currentAwaitingPreparationHysteresisValue) || isNaN(currentAwaitingRunHysteresisValue) || isNaN(currentAwaitingTransitionHysteresisValue) || isNaN(currentAwaitingDeviceHysteresisValue) || isNaN(currentAwaitingReducedPowerHysteresisValue)) {
            console.warn(`States for ${controllerId} is undefined or null`);
            this.currentAwaitingPreparationHysteresis = HysteresisState.UNDEFINED;
            this.currentAwaitingStartHysteresis = HysteresisState.UNDEFINED;
            this.currentAwaitingRunHysteresis = HysteresisState.UNDEFINED;
            this.currentAwaitingTransitionHysteresis = HysteresisState.UNDEFINED;
            this.currentAwaitingDeviceHysteresis = HysteresisState.UNDEFINED;
            this.currentAwaitingReducedPowerHysteresis = HysteresisState.UNDEFINED;
            this.currentState = ChpState.UNDEFINED;
        } else {
            // Ensure currentStateValue is a valid enum value (number) before casting
            this.currentAwaitingPreparationHysteresis = HysteresisState[currentAwaitingPreparationHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentAwaitingStartHysteresis = HysteresisState[currentAwaitingStartHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentAwaitingRunHysteresis = HysteresisState[currentAwaitingRunHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentAwaitingTransitionHysteresis = HysteresisState[currentAwaitingTransitionHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentAwaitingDeviceHysteresis = HysteresisState[currentAwaitingDeviceHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentAwaitingReducedPowerHysteresis = HysteresisState[currentAwaitingReducedPowerHysteresisValue as keyof typeof HysteresisState] ?? HysteresisState.UNDEFINED;
            this.currentState = ChpState[currentStateValue as keyof typeof ChpState] ?? ChpState.UNDEFINED;
            //console.log("Mapped currentState:", this.currentState);
        }
    }


    getBackgroundClass(state: string): string {

        switch (state) {
            case "UNDEFINED":
                return "danger"; // Neutral or undefined state
            case "NORMAL":
                return "success"; // Green for idle or ready
            case "ERROR":
                return "danger"; // Red for errors
            case "INACTIVE":
                return "medium"; // Grey for disabled
            case "ACTIVE":
                return "warning"; // Blue for active state
            case "CHP_ACTIVE":
                return "warning"; // Orange for charging
            case "CHP_INACTIVE":
                return "tertiary"; // Different color for hysteresis active
            case "CHP_PREPARING":
                return "warning";
            case "IDLE":
                return "medium";
            case "OVER_TEMPERATURE":
                return "danger";
            case "CHP_NOT_READY":
                return "danger";
            default:
                return "default"; // Optional fallback if state doesn't match
        }
    }

    /**
    * Updates Mode of chp
    *
    * @param event
    */
    updateMode(event: CustomEvent) {
        const oldMode = this.component.properties.mode;
        let newMode: mode;

        switch (event.detail.value) {
            case "MANUAL_ON":
                newMode = "MANUAL_ON";
                break;
            case "MANUAL_OFF":
                newMode = "MANUAL_OFF";
                break;
            case "AUTOMATIC":
                newMode = "AUTOMATIC";
                break;
        }

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, this.component.id, [
                { name: "mode", value: newMode },
            ]).then(() => {
                this.component.properties.mode = newMode;
                this.service.toast(this.translate.instant("General.changeAccepted"), "success");
            }).catch(reason => {
                this.component.properties.mode = oldMode;
                this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
                console.warn(reason);
            });
        }
    }
    /**
    * Updates the Min-Power of force charging
    *
    * @param event
    */
    updateThreshold() {
        const oldPriceThreshold = Number(this.component.properties["priceThreshold"]);

        const newPriceThreshold = Number(this.threshold);

        // prevents automatic update when no values have changed
        if (this.edge != null && oldPriceThreshold != newPriceThreshold) {
            this.edge.updateComponentConfig(this.websocket, this.component.id, [
                { name: "priceThreshold", value: newPriceThreshold },
            ]).then(() => {
                this.component.properties["priceThreshold"] = newPriceThreshold;
                this.service.toast(this.translate.instant("General.changeAccepted"), "success");
            }).catch(reason => {
                this.component.properties["priceThreshold"] = oldPriceThreshold;
                this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
                console.warn(reason);
            });
        }
    }
}

