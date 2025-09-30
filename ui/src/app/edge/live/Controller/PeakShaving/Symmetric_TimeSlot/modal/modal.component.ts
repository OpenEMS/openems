// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../../shared/shared";

@Component({
    selector: "timeslotpeakshaving-modal",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class Controller_Symmetric_TimeSlot_PeakShavingModalComponent implements OnInit {

    private static readonly SELECTOR = "timeslotpeakshaving-modal";

    @Input() protected component: EDGE_CONFIG.COMPONENT | null = null;
    @Input() protected edge: Edge | null = null;

    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        public formBuilder: FormBuilder,
        public modalCtrl: ModalController,
        public service: Service,
        public translate: TranslateService,
        public websocket: Websocket,
    ) { }

    ngOnInit() {
        THIS.FORM_GROUP = THIS.FORM_BUILDER.GROUP({
            peakShavingPower: new FormControl(THIS.COMPONENT.PROPERTIES.PEAK_SHAVING_POWER, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
                VALIDATORS.REQUIRED,
            ])),
            rechargePower: new FormControl(THIS.COMPONENT.PROPERTIES.RECHARGE_POWER, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
                VALIDATORS.REQUIRED,
            ])),
            hysteresisSoc: new FormControl(THIS.COMPONENT.PROPERTIES.HYSTERESIS_SOC, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(100|[1-9]?[0-9])$"),
                VALIDATORS.REQUIRED,
            ])),
            slowChargePower: new FormControl((THIS.COMPONENT.PROPERTIES.SLOW_CHARGE_POWER) * -1),
            slowChargeStartTime: new FormControl(THIS.COMPONENT.PROPERTIES.SLOW_CHARGE_START_TIME, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"),
                VALIDATORS.REQUIRED,
            ])),
            startDate: new FormControl(THIS.COMPONENT.PROPERTIES.START_DATE, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(0[1-9]|[12][0-9]|3[01])[.](0[1-9]|1[012])[.](19|20)[0-9]{2}$"),
                VALIDATORS.REQUIRED,
            ])),
            startTime: new FormControl(THIS.COMPONENT.PROPERTIES.START_TIME, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"),
                VALIDATORS.REQUIRED,
            ])),
            endDate: new FormControl(THIS.COMPONENT.PROPERTIES.END_DATE),
            endTime: new FormControl(THIS.COMPONENT.PROPERTIES.END_TIME, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"),
                VALIDATORS.REQUIRED,
            ])),
            monday: new FormControl(THIS.COMPONENT.PROPERTIES.MONDAY),
            tuesday: new FormControl(THIS.COMPONENT.PROPERTIES.TUESDAY),
            wednesday: new FormControl(THIS.COMPONENT.PROPERTIES.WEDNESDAY),
            thursday: new FormControl(THIS.COMPONENT.PROPERTIES.THURSDAY),
            friday: new FormControl(THIS.COMPONENT.PROPERTIES.FRIDAY),
            saturday: new FormControl(THIS.COMPONENT.PROPERTIES.SATURDAY),
            sunday: new FormControl(THIS.COMPONENT.PROPERTIES.SUNDAY),
        });
    }

    applyChanges() {
        if (THIS.EDGE != null) {
            if (THIS.EDGE.ROLE_IS_AT_LEAST("owner")) {
                const peakShavingPower = THIS.FORM_GROUP.CONTROLS["peakShavingPower"];
                const rechargePower = THIS.FORM_GROUP.CONTROLS["rechargePower"];
                if (PEAK_SHAVING_POWER.VALID && RECHARGE_POWER.VALID) {
                    if (PEAK_SHAVING_POWER.VALUE >= RECHARGE_POWER.VALUE) {
                        const updateComponentArray = [];
                        OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS).forEach((element, index) => {
                            if (THIS.FORM_GROUP.CONTROLS[element].dirty) {
                                if (OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index] == "slowChargePower") {
                                    UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: (THIS.FORM_GROUP.CONTROLS[element].value) * -1 });
                                } else {
                                    UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
                                }
                            }
                        });
                        THIS.LOADING = true;
                        THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray).then(() => {
                            THIS.COMPONENT.PROPERTIES.PEAK_SHAVING_POWER = PEAK_SHAVING_POWER.VALUE;
                            THIS.COMPONENT.PROPERTIES.RECHARGE_POWER = RECHARGE_POWER.VALUE;
                            THIS.LOADING = false;
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                        }).catch(reason => {
                            PEAK_SHAVING_POWER.SET_VALUE(THIS.COMPONENT.PROPERTIES.PEAK_SHAVING_POWER);
                            RECHARGE_POWER.SET_VALUE(THIS.COMPONENT.PROPERTIES.RECHARGE_POWER);
                            THIS.LOADING = false;
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                            CONSOLE.WARN(reason);
                        });
                        THIS.FORM_GROUP.MARK_AS_PRISTINE();
                    } else {
                        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.PEAKSHAVING.RELATION_ERROR"), "danger");
                    }
                } else {
                    THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INPUT_NOT_VALID"), "danger");
                }
            } else {
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.INSUFFICIENT_RIGHTS"), "danger");
            }
        }
    }
}
