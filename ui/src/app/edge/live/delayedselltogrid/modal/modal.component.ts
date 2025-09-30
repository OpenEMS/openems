// @ts-strict-ignore
import { Component, Input, OnInit } from "@angular/core";
import { FormBuilder, FormControl, FormGroup, Validators } from "@angular/forms";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "../../../../shared/shared";

@Component({
    selector: DELAYED_SELL_TO_GRID_MODAL_COMPONENT.SELECTOR,
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class DelayedSellToGridModalComponent implements OnInit {

    private static readonly SELECTOR = "delayedselltogrid-modal";
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
            continuousSellToGridPower: new FormControl(THIS.COMPONENT.PROPERTIES.CONTINUOUS_SELL_TO_GRID_POWER, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
                VALIDATORS.REQUIRED,
            ])),
            sellToGridPowerLimit: new FormControl(THIS.COMPONENT.PROPERTIES.SELL_TO_GRID_POWER_LIMIT, VALIDATORS.COMPOSE([
                VALIDATORS.PATTERN("^(?:[1-9][0-9]*|0)$"),
                VALIDATORS.REQUIRED,
            ])),
        });
    }

    applyChanges() {
        if (THIS.EDGE != null) {
            if (THIS.EDGE.ROLE_IS_AT_LEAST("owner")) {
                const continuousSellToGridPower = THIS.FORM_GROUP.CONTROLS["continuousSellToGridPower"];
                const sellToGridPowerLimit = THIS.FORM_GROUP.CONTROLS["sellToGridPowerLimit"];
                if (CONTINUOUS_SELL_TO_GRID_POWER.VALID && SELL_TO_GRID_POWER_LIMIT.VALID) {
                    if (SELL_TO_GRID_POWER_LIMIT.VALUE > CONTINUOUS_SELL_TO_GRID_POWER.VALUE) {
                        const updateComponentArray = [];
                        OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS).forEach((element, index) => {
                            if (THIS.FORM_GROUP.CONTROLS[element].dirty) {
                                UPDATE_COMPONENT_ARRAY.PUSH({ name: OBJECT.KEYS(THIS.FORM_GROUP.CONTROLS)[index], value: THIS.FORM_GROUP.CONTROLS[element].value });
                            }
                        });
                        THIS.LOADING = true;
                        THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT.ID, updateComponentArray).then(() => {
                            THIS.COMPONENT.PROPERTIES.CONTINUOUS_SELL_TO_GRID_POWER = CONTINUOUS_SELL_TO_GRID_POWER.VALUE;
                            THIS.COMPONENT.PROPERTIES.SELL_TO_GRID_POWER_LIMIT = SELL_TO_GRID_POWER_LIMIT.VALUE;
                            THIS.LOADING = false;
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
                        }).catch(reason => {
                            CONTINUOUS_SELL_TO_GRID_POWER.SET_VALUE(THIS.COMPONENT.PROPERTIES.CONTINUOUS_SELL_TO_GRID_POWER);
                            SELL_TO_GRID_POWER_LIMIT.SET_VALUE(THIS.COMPONENT.PROPERTIES.SELL_TO_GRID_POWER_LIMIT);
                            THIS.LOADING = false;
                            THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                            CONSOLE.WARN(reason);
                        });
                        THIS.FORM_GROUP.MARK_AS_PRISTINE();
                    } else {
                        THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.DELAYED_SELL_TO_GRID.RELATION_ERROR"), "danger");
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
