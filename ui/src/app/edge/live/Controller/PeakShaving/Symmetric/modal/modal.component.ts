// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../../../shared/shared";

@Component({
    selector: "symmetricpeakshaving-modal",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class Controller_Symmetric_PeakShavingModalComponent implements OnInit {

    @Input({ required: true }) protected component!: EDGE_CONFIG.COMPONENT;
    @Input({ required: true }) protected edge!: Edge;


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
                                UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
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
