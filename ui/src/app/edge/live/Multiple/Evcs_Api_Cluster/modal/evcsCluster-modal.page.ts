// @ts-strict-ignore
import { Component, Input, OnInit, ViewChild } from "@angular/core";
import { ActivatedRoute, Router } from "@angular/router";
import { IonReorderGroup, ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

type ChargeMode = "FORCE_CHARGE" | "EXCESS_POWER";
type Priority = "CAR" | "STORAGE";

@Component({
    selector: "Evcs_Api_Cluster-modal",
    templateUrl: "./evcsCluster-MODAL.PAGE.HTML",
    standalone: false,
})
export class Evcs_Api_ClusterModalComponent implements OnInit {

    @Input({ required: true }) public edge!: Edge;
    @Input() public config: EDGE_CONFIG.COMPONENT | null = null;
    @Input({ required: true }) public componentId!: string;
    @Input() public evcsMap: { [sourceId: string]: EDGE_CONFIG.COMPONENT } = {};

    @ViewChild(IonReorderGroup, { static: true })
    public reorderGroup: IonReorderGroup;
    public evcsAmount: number;
    public swiperIndex: number = 0;
    public slideOpts = {
        noSwiping: true,
        noSwipingClass: "swiper-no-swiping",
        //noSwipingSelector: 'ion-range, ion-toggle',
        initialSlide: 0,
        speed: 1000,
    };
    public firstEvcs: string;
    public lastEvcs: string;
    public prioritizedEvcsList: string[];
    public evcsConfigMap: { [evcsId: string]: EDGE_CONFIG.COMPONENT } = {};

    public chargeState: ChargeState;
    private chargePlug: ChargePlug;

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        private route: ActivatedRoute,
        protected translate: TranslateService,
        private modalCtrl: ModalController,
    ) {
    }

    ngOnInit() {


        THIS.PRIORITIZED_EVCS_LIST = THIS.CONFIG.PROPERTIES["EVCS.IDS"];
        THIS.EVCS_AMOUNT = THIS.PRIORITIZED_EVCS_LIST.LENGTH;
        THIS.LAST_EVCS = THIS.PRIORITIZED_EVCS_LIST[THIS.EVCS_AMOUNT - 1];
        THIS.FIRST_EVCS = THIS.PRIORITIZED_EVCS_LIST[0];

        THIS.SERVICE.GET_CONFIG().then(config => {
            THIS.PRIORITIZED_EVCS_LIST.FOR_EACH(evcsId => {
                THIS.EVCS_CONFIG_MAP[evcsId] = CONFIG.GET_COMPONENT(evcsId);
            });
        });
    }

    doReorder(ev: any) {
        const oldListOrder = THIS.PRIORITIZED_EVCS_LIST;
        THIS.PRIORITIZED_EVCS_LIST = EV.DETAIL.COMPLETE(THIS.PRIORITIZED_EVCS_LIST);

        const newListOrder = THIS.PRIORITIZED_EVCS_LIST;

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.CONFIG.ID, [
                { name: "EVCS.IDS", value: newListOrder },
            ]).then(response => {
                THIS.CONFIG.PROPERTIES.CHARGE_MODE = newListOrder;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                THIS.CONFIG.PROPERTIES.CHARGE_MODE = oldListOrder;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    cancel() {
        THIS.MODAL_CTRL.DISMISS();
    }

    /**
    * Updates the Charge-Mode of the EVCS-Controller.
    *
    * @param event
    */
    updateChargeMode(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT) {
        const oldChargeMode = CURRENT_CONTROLLER.PROPERTIES.CHARGE_MODE;
        let newChargeMode: ChargeMode;

        switch (EVENT.DETAIL.VALUE) {
            case "FORCE_CHARGE":
                newChargeMode = "FORCE_CHARGE";
                break;
            case "EXCESS_POWER":
                newChargeMode = "EXCESS_POWER";
                break;
        }

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "chargeMode", value: newChargeMode },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.CHARGE_MODE = newChargeMode;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.CHARGE_MODE = oldChargeMode;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }
    /**
     * Changed the Priority between the components of the charging session
     */
    priorityChanged(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT) {
        const oldPriority = CURRENT_CONTROLLER.PROPERTIES.PRIORITY;
        let newPriority: Priority;

        switch (EVENT.DETAIL.VALUE) {
            case "CAR":
                newPriority = "CAR";
                break;
            case "STORAGE":
                newPriority = "STORAGE";
                break;
        }

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "priority", value: newPriority },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.PRIORITY = newPriority;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.PRIORITY = oldPriority;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    /**
     * Updates the Min-Power of force charging
     *
     * @param event
     */
    updateForceMinPower(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT, numberOfPhases: number) {
        const oldMinChargePower = CURRENT_CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER;
        let newMinChargePower = EVENT.DETAIL.VALUE;
        newMinChargePower /= numberOfPhases;

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "forceChargeMinPower", value: newMinChargePower },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER = newMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.FORCE_CHARGE_MIN_POWER = oldMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    /**
     * Updates the Min-Power of default charging
     *
     * @param event
     */
    updateDefaultMinPower(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT) {
        const oldMinChargePower = CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER;
        const newMinChargePower = EVENT.DETAIL.VALUE;

        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "defaultChargeMinPower", value: newMinChargePower },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER = newMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER = oldMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    currentLimitChanged(event: CustomEvent, property: string) {

    }

    /**
     * update the state of the toggle which renders the minimum charge power
     *
     * @param event
     * @param phases
     */
    allowMinimumChargePower(event: CustomEvent, phases: number, currentController: EDGE_CONFIG.COMPONENT) {

        const oldMinChargePower = CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER;

        let newMinChargePower = 0;
        if (oldMinChargePower == null || oldMinChargePower == 0) {
            newMinChargePower = phases != undefined ? 1400 * phases : 4200;
        }
        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "defaultChargeMinPower", value: newMinChargePower },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER = newMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.DEFAULT_CHARGE_MIN_POWER = oldMinChargePower;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }

    /**
    * Activates or deactivates the Charging
    *
    * @param event
    */
    enableOrDisableCharging(event: CustomEvent, currentController: EDGE_CONFIG.COMPONENT) {

        const oldChargingState = CURRENT_CONTROLLER.PROPERTIES.ENABLED_CHARGING;
        const newChargingState = !oldChargingState;
        if (THIS.EDGE != null) {
            THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, CURRENT_CONTROLLER.ID, [
                { name: "enabledCharging", value: newChargingState },
            ]).then(response => {
                CURRENT_CONTROLLER.PROPERTIES.ENABLED_CHARGING = newChargingState;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_ACCEPTED"), "success");
            }).catch(reason => {
                CURRENT_CONTROLLER.PROPERTIES.ENABLED_CHARGING = oldChargingState;
                THIS.SERVICE.TOAST(THIS.TRANSLATE.INSTANT("GENERAL.CHANGE_FAILED") + "\n" + REASON.ERROR.MESSAGE, "danger");
                CONSOLE.WARN(reason);
            });
        }
    }
    /**
     * Gets the output for the current state or the current charging power
     *
     * @param power
     * @param state
     * @param plug
     */
    getState(power: number, state: number, plug: number, currentController: EDGE_CONFIG.COMPONENT) {
        if (currentController != null) {
            if (CURRENT_CONTROLLER.PROPERTIES.ENABLED_CHARGING != null && CURRENT_CONTROLLER.PROPERTIES.ENABLED_CHARGING == false) {
                return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING_STATION_DEACTIVATED");
            }
        }
        if (power == null || power == 0) {

            THIS.CHARGE_STATE = state;
            THIS.CHARGE_PLUG = plug;

            if (THIS.CHARGE_PLUG == null) {
                if (THIS.CHARGE_STATE == null) {
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
                }
            } else if (THIS.CHARGE_PLUG != ChargePlug.PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED) {
                return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CABLE_NOT_CONNECTED");
            }

            switch (THIS.CHARGE_STATE) {
                case CHARGE_STATE.STARTING:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.STARTING");
                case CHARGE_STATE.UNDEFINED:
                case CHARGE_STATE.ERROR:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.ERROR");
                case ChargeState.READY_FOR_CHARGING:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.READY_FOR_CHARGING");
                case ChargeState.NOT_READY_FOR_CHARGING:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_READY_FOR_CHARGING");
                case ChargeState.AUTHORIZATION_REJECTED:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.NOT_CHARGING");
                case ChargeState.ENERGY_LIMIT_REACHED:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGE_LIMIT_REACHED");
                case ChargeState.CHARGING_FINISHED:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CAR_FULL");
                default:
                    return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
            }
        }
        return THIS.TRANSLATE.INSTANT("EDGE.INDEX.WIDGETS.EVCS.CHARGING");
    }

    /**
     * Round to 100 and
     * Round up (ceil)
     *
     * @param i
     */
    formatNumber(i: number) {
        const round = MATH.CEIL(i / 100) * 100;
        return round;
    }

    /**
     * Get Value or 3
     *
     * @param i
     */
    getValueOrThree(i: number) {
        if (i == null || i == undefined) {
            return 3;
        } else {
            return i;
        }
    }

    //TODO: Do it in the edge component
    currentChargingPower(): number {
        return THIS.SUM_OF_CHANNEL("ChargePower");
    }

    private sumOfChannel(channel: string): number {

        const sum = 0;/*
    THIS.EVCS_MAP.FOR_EACH(station => {
      let channelValue = THIS.EDGE.CURRENT_DATA.VALUE.CHANNEL[STATION.ID + "/" + channel];
      if (channelValue != null) {
        sum += channelValue;
      };
    });
    */
        return sum;
    }
}

enum ChargeState {
    UNDEFINED = -1,           //Undefined
    STARTING,                 //Starting
    NOT_READY_FOR_CHARGING,   //Not ready for Charging E.G. unplugged, X1 or "ena" not enabled, RFID not enabled,...
    READY_FOR_CHARGING,       //Ready for Charging waiting for EV charging request
    CHARGING,                 //Charging
    ERROR,                    //Error
    AUTHORIZATION_REJECTED,   //Authorization rejected
    ENERGY_LIMIT_REACHED,     //Charge limit reached
    CHARGING_FINISHED,         //Charging has finished
}

enum ChargePlug {
    UNDEFINED = -1,                           //Undefined
    UNPLUGGED,                                //Unplugged
    PLUGGED_ON_EVCS,                          //Plugged on EVCS
    PLUGGED_ON_EVCS_AND_LOCKED = 3,           //Plugged on EVCS and locked
    PLUGGED_ON_EVCS_AND_ON_EV = 5,            //Plugged on EVCS and on EV
    PLUGGED_ON_EVCS_AND_ON_EV_AND_LOCKED = 7,  //Plugged on EVCS and on EV and locked
}
