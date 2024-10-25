// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../../shared/shared";

enum PeakShavingState {
    UNDEFINED = -1,              // Undefined state
    IDLE = 0,                    // Peakshaver is inactive and waiting
    ERROR = 1,                   // Error State
    DISABLED = 2,                // Peak Shaver not active
    ACTIVE = 3,                  // Active Peak Shaving
    CHARGING = 4,                // ESS charges
    HYSTERESIS_ACTIVE = 5,        // Waiting. No Active Peak Shaving since hysteresis start
}


@Component({
    selector: "thresholdpeakshaving-modal",
    templateUrl: "./modal.component.html",
})


export class Controller_Symmetric_Threshold_PeakShavingModalComponent implements OnInit {

    private static readonly SELECTOR = "thresholdpeakshaving-modal";

    @Input() protected component: EdgeConfig.Component | null = null;
    @Input() protected edge: Edge | null = null;

    public formGroup: FormGroup;
    public loading: boolean = false;
    // Variable to hold the current state of the PeakShavingState
    public currentState: PeakShavingState = PeakShavingState.UNDEFINED; // Default value is UNDEFINED


    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        console.log("Edge currentData (before subscription):", this.edge.currentData);

        this.formGroup = this.formBuilder.group({
            peakShavingPower: new FormControl(this.component.properties.peakShavingPower, Validators.compose([
                Validators.pattern("^(?:[1-9][0-9]*|0)$"),
                Validators.required,
            ])),
            peakShavingThresholdPower: new FormControl(this.component.properties.peakShavingThresholdPower, Validators.compose([
                Validators.pattern("^(?:[1-9][0-9]*|0)$"),
                Validators.required,
            ])),
            rechargePower: new FormControl(this.component.properties.rechargePower, Validators.compose([
                Validators.pattern("^(?:[1-9][0-9]*|0)$"),
                Validators.required,
            ])),
            // FormControl for the Controller ID
            controllerId: new FormControl(this.component?.id || "", Validators.required),
        });

        this.edge.currentData.subscribe((currentData) => {
            this.setCurrentStateFromData(currentData);
            console.log("Current Data:", currentData); // Check what data is coming in
        });
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
        const currentStateValue = currentData.channel[`${controllerId}/PeakShavingStateMachine`];

        // Debugging the fetched state value
        //console.log(`Fetched state from ${controllerId}/PeakShavingStateMachine:`, currentStateValue);

        // Check if currentStateValue is not undefined or null and is a valid number
        if (currentStateValue === undefined || currentStateValue === null || isNaN(currentStateValue)) {
            console.warn(`State for ${controllerId}/PeakShavingStateMachine is undefined or null`);
            this.currentState = PeakShavingState.UNDEFINED;
        } else {
            // Ensure currentStateValue is a valid enum value (number) before casting
            this.currentState = PeakShavingState[currentStateValue as keyof typeof PeakShavingState] ?? PeakShavingState.UNDEFINED;
            //console.log("Mapped currentState:", this.currentState);
        }
    }

    getBackgroundClass(state: string): string {

        switch (state) {
            case "UNDEFINED":
                return "danger"; // Neutral or undefined state
            case "IDLE":
                return "success"; // Green for idle or ready
            case "ERROR":
                return "danger"; // Red for errors
            case "DISABLED":
                return "medium"; // Grey for disabled
            case "ACTIVE":
                return "warning"; // Blue for active state
            case "CHARGING":
                return "warning"; // Orange for charging
            case "HYSTERESIS_ACTIVE":
                return "tertiary"; // Different color for hysteresis active
            case "CHARGING_FINISHED":
                return "success"; // Green to indicate charging is complete
            case "DISCHARGING_FAILS":
                return "danger"; // Red for discharging failure
            case "PEAKSHAVING_POWER_TOO_LOW":
                return "danger"; // Red to indicate a critical power issue
            case "PEAKSHAVING_TARGET_NOT_REACHED":
                return "warning"; // Yellow/orange to indicate a warning but not failure
            default:
                return "default"; // Optional fallback if state doesn't match
        }
    }

    applyChanges() {
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast("owner")) {
                const peakShavingPower = this.formGroup.controls["peakShavingPower"];
                const rechargePower = this.formGroup.controls["rechargePower"];
                const peakShavingThresholdPower = this.formGroup.controls["peakShavingThresholdPower"];
                if (peakShavingPower.valid && rechargePower.valid) {
                    if (peakShavingPower.value >= rechargePower.value) {
                        const updateComponentArray = [];
                        Object.keys(this.formGroup.controls).forEach((element, index) => {
                            if (this.formGroup.controls[element].dirty) {
                                if (Object.keys(this.formGroup.controls)[index] == "slowChargePower") {
                                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: (this.formGroup.controls[element].value) * -1 });
                                } else {
                                    updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value });
                                }
                            }
                        });
                        this.loading = true;
                        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
                            this.component.properties.peakShavingPower = peakShavingPower.value;
                            this.component.properties.rechargePower = rechargePower.value;
                            this.component.properties.peakShavingThresholdPower = peakShavingThresholdPower.value;
                            this.loading = false;
                            this.service.toast(this.translate.instant("General.changeAccepted"), "success");
                        }).catch(reason => {
                            peakShavingPower.setValue(this.component.properties.peakShavingPower);
                            rechargePower.setValue(this.component.properties.rechargePower);
                            peakShavingThresholdPower.setValue(this.component.properties.peakShavingThresholdPower);
                            this.loading = false;
                            this.service.toast(this.translate.instant("General.changeFailed") + "\n" + reason.error.message, "danger");
                            console.warn(reason);
                        });
                        this.formGroup.markAsPristine();
                    } else {
                        this.service.toast(this.translate.instant("Edge.Index.Widgets.Peakshaving.relationError"), "danger");
                    }
                } else {
                    this.service.toast(this.translate.instant("General.inputNotValid"), "danger");
                }
            } else {
                this.service.toast(this.translate.instant("General.insufficientRights"), "danger");
            }
        }
    }
}
