// @ts-strict-ignore
import { Component, OnInit } from '@angular/core';
import { AlertController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, Subscription } from 'rxjs';
import { skip } from 'rxjs/operators';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { ExecuteSystemRestartRequest, Type } from 'src/app/shared/jsonrpc/request/executeSystemRestartRequest';
import { Role } from 'src/app/shared/type/role';
import { environment } from 'src/environments';

import { Edge, presentAlert, Service, Utils, Websocket } from '../../../../shared/shared';

enum SystemRestartState {
    INITIAL, // No restart
    RESTARTING, // System is restarting
    RESTARTED, // System was restarted successfully
    FAILED, // System restart failed
}

@Component({
    selector: MaintenanceComponent.SELECTOR,
    templateUrl: './maintenance.html',
    styles: [`
    :host {
        ion-card: {
            cursor: auto !important;;
        }
    }
    `],
})
export class MaintenanceComponent implements OnInit {

    protected readonly environment = environment;

    protected edge: Edge | null = null;
    protected options: { key: string, message: string, color: 'success' | 'warning' | null, info: string, roleIsAtLeast: Role, button: { disabled: boolean, label: string, callback: () => void } }[] = [
        {
            key: Type.HARD, message: null, color: null, info: this.translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_WARNING', { system: environment.edgeShortName }), roleIsAtLeast: Role.OWNER, button: {
                callback: () => this.confirmationAlert(Type.HARD), disabled: false, label: this.translate.instant('SETTINGS.SYSTEM_UPDATE.EMS_RESTARTING', { edgeShortName: environment.edgeShortName }),
            },
        },
    ];

    protected systemRestartState: BehaviorSubject<{ key: Type, state: SystemRestartState }> = new BehaviorSubject({ key: null, state: SystemRestartState.INITIAL });
    protected spinnerId: string = MaintenanceComponent.SELECTOR;
    protected readonly SystemRestartState = SystemRestartState;
    protected confirmationAlert: (type: Type) => void = (type: Type) => presentAlert(this.alertCtrl, this.translate, {
        message: this.translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_WARNING', { system: environment.edgeShortName }),
        subHeader: this.translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_CONFIRMATION', { system: environment.edgeShortName }),
        buttons: [{
            text: this.translate.instant('General.RESTART'),
            handler: () => this.execRestart(type),
        }],
    });

    private static readonly SELECTOR: string = 'oe-maintenance';
    private static readonly TIMEOUT: number = 3000;

    constructor(
        protected utils: Utils,
        private websocket: Websocket,
        protected service: Service,
        private translate: TranslateService,
        private alertCtrl: AlertController,
    ) {
    }

    ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        this.service.getCurrentEdge().then((edge) => {
            this.service.getConfig().then(() => {
                this.edge = edge;

                this.options = this.options.map(option => {
                    option.button.disabled = !this.edge.roleIsAtLeast(option.roleIsAtLeast);
                    return option;
                });
            });
        });

        this.systemRestartState.subscribe(state => {
            this.updateOptions(state.key);
        });
    }

    /**
     * Updates the options
     *
     * @param type the restart type
     */
    private updateOptions(type: Type): void {
        let message: string | null = null;
        let disableButtons: boolean = false;
        let showInfo: boolean = false;
        let color: 'warning' | 'success' | null = null;
        const system = type === Type.HARD ? environment.edgeShortName : this.translate.instant('General.SYSTEM');

        switch (this.systemRestartState?.value?.state) {
            case SystemRestartState.FAILED:
                message = this.translate.instant("SETTINGS.SYSTEM_UPDATE.RESTART_FAILED", { system: system });
                color = 'warning';
                disableButtons = false;
                showInfo = true;
                break;
            case SystemRestartState.RESTARTING:
                this.service.startSpinnerTransparentBackground(this.spinnerId + type);
                this.checkSystemState(type);
                disableButtons = true;
                message = this.translate.instant("SETTINGS.SYSTEM_UPDATE.RESTARTING", { system: system, minutes: 10 });
                break;
            case SystemRestartState.RESTARTED:
                this.service.stopSpinner(this.spinnerId + type);
                disableButtons = false;
                color = 'success';
                message = this.translate.instant("SETTINGS.SYSTEM_UPDATE.RESTARTED", { system: system });
                showInfo = true;
                break;
        }

        if (!message) {
            return;
        }

        this.options = this.options.map(option => {
            if (option.key === type) {
                option.message = message;
            }
            // Hide and show buttons
            option.button.disabled = disableButtons ? disableButtons : !this.edge.roleIsAtLeast(option.roleIsAtLeast);
            option.color = color;
            option.info = showInfo ? this.translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_WARNING', { system: environment.edgeShortName }) : null;
            return option;
        });
    }

    /**
     * Executes the system restart
     *
     * @param type the restart type
     */
    private execRestart(type: Type) {

        const request = new ComponentJsonApiRequest({ componentId: '_host', payload: new ExecuteSystemRestartRequest({ type: type }) });

        // Workaround, there could be no response
        this.edge.sendRequest(this.websocket, request).catch(() => {
            this.systemRestartState.next({ key: type, state: SystemRestartState.FAILED });
            return;
        });

        setTimeout(() => {
            if (this.systemRestartState?.value?.state === SystemRestartState.FAILED) {
                return;
            }

            this.systemRestartState.next({ key: type, state: SystemRestartState.RESTARTING });
        }, MaintenanceComponent.TIMEOUT);
    }

    /**
     * Checks the system state and waits for a getEdgeConfig notification
     *
     * @param type the restart type
     */
    private checkSystemState(type: Type): void {

        const subscription: Subscription = new Subscription();
        subscription.add(

            // wait for next edgeConfig
            this.edge.getConfig(this.websocket).pipe(skip(1)).subscribe(() => {
                subscription.unsubscribe();
                this.systemRestartState.next({ key: type, state: SystemRestartState.RESTARTED });
            }),
        );
    }

    /**
     * Present confirmation alert
     */
    async presentAlert(type: Type) {
        const translate = this.translate;
        const system = type === Type.HARD ? environment.edgeShortName : 'OpenEMS';
        const alert = this.alertCtrl.create({
            subHeader: translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_CONFIRMATION', { system: system }),
            message: translate.instant('SETTINGS.SYSTEM_UPDATE.RESTART_WARNING', { system: system }),
            buttons: [{
                text: translate.instant('General.cancel'),
                role: 'cancel',
            },
            {
                text: translate.instant('General.RESTART'),
                handler: () => this.execRestart(type),
            }],
            cssClass: 'alertController',
        });
        (await alert).present();
    }
}
