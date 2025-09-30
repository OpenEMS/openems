// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Subject } from "rxjs";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../../shared/shared";

@Component({
    selector: "asymmetricpeakshaving-modal",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class Controller_Asymmetric_PeakShavingModalComponent implements OnInit {

    @Input({ required: true }) protected component!: EDGE_CONFIG.COMPONENT;
    @Input({ required: true }) protected edge!: Edge;
    @Input({ required: true }) protected mostStressedPhase!: Subject<{ name: "L1" | "L2" | "L3" | "", value: number }>;

    public formGroup: FormGroup;
    public loading: boolean = false;

    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public formBuilder: FormBuilder,
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
        });
    }

    applyChanges() {
        if (THIS.EDGE != null) {
            if (THIS.EDGE.ROLE_IS_AT_LEAST("owner")) {
                if (THIS.FORM_GROUP.CONTROLS["peakShavingPower"].valid && THIS.FORM_GROUP.CONTROLS["rechargePower"].valid) {
                    if (THIS.FORM_GROUP.CONTROLS["peakShavingPower"].value >= THIS.FORM_GROUP.CONTROLS["rechargePower"].value) {
                        const updateComponentArray = [];
                        OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS).forEach((element, index) => {
                            if (THIS.FORM_GROUP.CONTROLS[element].dirty) {
                                UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
                            }
                        });
                        THIS.LOADING = true;
                        THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray).then(() => {
                            THIS.COMPONENT.PROPERTIES.PEAK_SHAVING_POWER = THIS.FORM_GROUP.VALUE.PEAK_SHAVING_POWER;
                            THIS.COMPONENT.PROPERTIES.RECHARGE_POWER = THIS.FORM_GROUP.VALUE.RECHARGE_POWER;
                            THIS.LOADING = false;
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                        }).catch(reason => {
                            THIS.FORM_GROUP.CONTROLS["peakShavingPower"].setValue(THIS.COMPONENT.PROPERTIES.PEAK_SHAVING_POWER);
                            THIS.FORM_GROUP.CONTROLS["rechargePower"].setValue(THIS.COMPONENT.PROPERTIES.RECHARGE_POWER);
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
