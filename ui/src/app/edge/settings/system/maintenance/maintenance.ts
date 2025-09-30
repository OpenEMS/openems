// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { AlertController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { BehaviorSubject, Subscription } from "rxjs";
import { skip } from "rxjs/operators";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { ExecuteSystemRestartRequest, Type } from "src/app/shared/jsonrpc/request/executeSystemRestartRequest";
import { Role } from "src/app/shared/type/role";
import { environment } from "src/environments";

import { Edge, presentAlert, Service, Utils, Websocket } from "../../../../shared/shared";

enum SystemRestartState {
    INITIAL, // No restart
    RESTARTING, // System is restarting
    RESTARTED, // System was restarted successfully
    FAILED, // System restart failed
}

@Component({
    selector: MAINTENANCE_COMPONENT.SELECTOR,
    templateUrl: "./MAINTENANCE.HTML",
    styles: [`
    :host {
        :is(ion-card) {
            cursor: auto !important;;
        }
    }
    `],
    standalone: false,
})
export class MaintenanceComponent implements OnInit {

    private static readonly SELECTOR: string = "oe-maintenance";
    private static readonly TIMEOUT: number = 3000;

    protected readonly environment = environment;

    protected edge: Edge | null = null;
    protected options: { key: string, message: string, color: "success" | "warning" | null, info: string, roleIsAtLeast: Role, button: { disabled: boolean, label: string, callback: () => void } }[] = [
        {
            key: TYPE.HARD, message: null, color: null, info: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_WARNING", { system: ENVIRONMENT.EDGE_SHORT_NAME }), roleIsAtLeast: ROLE.OWNER, button: {
                callback: () => THIS.CONFIRMATION_ALERT(TYPE.HARD), disabled: false, label: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.EMS_RESTARTING", { edgeShortName: ENVIRONMENT.EDGE_SHORT_NAME }),
            },
        },
    ];

    protected systemRestartState: BehaviorSubject<{ key: Type, state: SystemRestartState }> = new BehaviorSubject({ key: null, state: SYSTEM_RESTART_STATE.INITIAL });
    protected spinnerId: string = MAINTENANCE_COMPONENT.SELECTOR;
    protected readonly SystemRestartState = SystemRestartState;

    constructor(
        protected utils: Utils,
        private websocket: Websocket,
        protected service: Service,
        private translate: TranslateService,
        private alertCtrl: AlertController,
    ) { }

    /**
 * Present confirmation alert
 */
    async presentAlert(type: Type) {
        const translate = THIS.TRANSLATE;
        const system = type === TYPE.HARD ? ENVIRONMENT.EDGE_SHORT_NAME : "OpenEMS";
        const alert = THIS.ALERT_CTRL.CREATE({
            subHeader: TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_CONFIRMATION", { system: system }),
            message: TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_WARNING", { system: system }),
            buttons: [{
                text: TRANSLATE.INSTANT("GENERAL.CANCEL"),
                role: "cancel",
            },
            {
                text: TRANSLATE.INSTANT("GENERAL.RESTART"),
                handler: () => THIS.EXEC_RESTART(type),
            }],
            cssClass: "alertController",
        });
        (await alert).present();
    }

    ngOnInit() {
        THIS.SERVICE.START_SPINNER(THIS.SPINNER_ID);
        THIS.SERVICE.GET_CURRENT_EDGE().then((edge) => {
            THIS.SERVICE.GET_CONFIG().then(() => {
                THIS.EDGE = edge;

                THIS.OPTIONS = THIS.OPTIONS.MAP(option => {
                    OPTION.BUTTON.DISABLED = !THIS.EDGE.ROLE_IS_AT_LEAST(OPTION.ROLE_IS_AT_LEAST);
                    return option;
                });
            });
        });

        THIS.SYSTEM_RESTART_STATE.SUBSCRIBE(state => {
            THIS.UPDATE_OPTIONS(STATE.KEY);
        });
    }

    protected confirmationAlert: (type: Type) => void = (type: Type) => presentAlert(THIS.ALERT_CTRL, THIS.TRANSLATE, {
        message: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_WARNING", { system: ENVIRONMENT.EDGE_SHORT_NAME }),
        subHeader: THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_CONFIRMATION", { system: ENVIRONMENT.EDGE_SHORT_NAME }),
        buttons: [{
            text: THIS.TRANSLATE.INSTANT("GENERAL.RESTART"),
            handler: () => THIS.EXEC_RESTART(type),
        }],
    });

    /**
     * Updates the options
     *
     * @param type the restart type
     */
    private updateOptions(type: Type): void {
        let message: string | null = null;
        let disableButtons: boolean = false;
        let showInfo: boolean = false;
        let color: "warning" | "success" | null = null;
        const system = type === TYPE.HARD ? ENVIRONMENT.EDGE_SHORT_NAME : THIS.TRANSLATE.INSTANT("GENERAL.SYSTEM");

        switch (THIS.SYSTEM_RESTART_STATE?.value?.state) {
            case SYSTEM_RESTART_STATE.FAILED:
                message = THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_FAILED", { system: system });
                color = "warning";
                disableButtons = false;
                showInfo = true;
                break;
            case SYSTEM_RESTART_STATE.RESTARTING:
                THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(THIS.SPINNER_ID + type);
                THIS.CHECK_SYSTEM_STATE(type);
                disableButtons = true;
                message = THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTARTING", { system: system, minutes: 10 });
                break;
            case SYSTEM_RESTART_STATE.RESTARTED:
                THIS.SERVICE.STOP_SPINNER(THIS.SPINNER_ID + type);
                disableButtons = false;
                color = "success";
                message = THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTARTED", { system: system });
                showInfo = true;
                break;
            default:
                break;

        }

        if (!message) {
            return;
        }

        THIS.OPTIONS = THIS.OPTIONS.MAP(option => {
            if (OPTION.KEY === type) {
                OPTION.MESSAGE = message;
            }
            // Hide and show buttons
            OPTION.BUTTON.DISABLED = disableButtons ? disableButtons : !THIS.EDGE.ROLE_IS_AT_LEAST(OPTION.ROLE_IS_AT_LEAST);
            OPTION.COLOR = color;
            OPTION.INFO = showInfo ? THIS.TRANSLATE.INSTANT("SETTINGS.SYSTEM_UPDATE.RESTART_WARNING", { system: ENVIRONMENT.EDGE_SHORT_NAME }) : null;
            return option;
        });
    }

    /**
     * Executes the system restart
     *
     * @param type the restart type
     */
    private execRestart(type: Type) {

        const request = new ComponentJsonApiRequest({ componentId: "_host", payload: new ExecuteSystemRestartRequest({ type: type }) });

        // Workaround, there could be no response
        THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, request).catch(() => {
            THIS.SYSTEM_RESTART_STATE.NEXT({ key: type, state: SYSTEM_RESTART_STATE.FAILED });
            return;
        });

        setTimeout(() => {
            if (THIS.SYSTEM_RESTART_STATE?.value?.state === SYSTEM_RESTART_STATE.FAILED) {
                return;
            }

            THIS.SYSTEM_RESTART_STATE.NEXT({ key: type, state: SYSTEM_RESTART_STATE.RESTARTING });
        }, MAINTENANCE_COMPONENT.TIMEOUT);
    }

    /**
     * Checks the system state and waits for a getEdgeConfig notification
     *
     * @param type the restart type
     */
    private checkSystemState(type: Type): void {

        const subscription: Subscription = new Subscription();
        SUBSCRIPTION.ADD(

            // wait for next edgeConfig
            THIS.EDGE.GET_CONFIG(THIS.WEBSOCKET).pipe(skip(1)).subscribe(() => {
                SUBSCRIPTION.UNSUBSCRIBE();
                THIS.SYSTEM_RESTART_STATE.NEXT({ key: type, state: SYSTEM_RESTART_STATE.RESTARTED });
            }),
        );
    }

}
