import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { RangeValue } from '@ionic/core';

type mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';


@Component({
    selector: HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class HeatingElementModalComponent implements OnInit {


    @Input() private componentId: string;
    @Input() public edge: Edge;
    @Input() public controller: EdgeConfig.Component;
    @Input() public outputChannelPhaseOne: ChannelAddress;
    @Input() public outputChannelPhaseTwo: ChannelAddress;
    @Input() public outputChannelPhaseThree: ChannelAddress;

    private static readonly SELECTOR = "heatingelement-modal";

    public pickerOptions: any;

    public minTime: RangeValue;
    public minKwh: RangeValue;

    constructor(
        public service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
    ) {
        this.pickerOptions = {
            buttons: [
                {
                    text: 'Cancel',
                    role: 'cancel'
                },
                {
                    text: 'OK',
                    handler: (value: any): void => {
                        if (this.edge != null) {
                            let endTime = value.hour.text + ':' + value.minute.text;
                            let oldTime = this.controller.properties['endTime'];
                            this.edge.updateComponentConfig(this.websocket, this.controller.id, [
                                { name: 'endTime', value: endTime }
                            ]).then(() => {
                                this.controller.properties['endTime'] = endTime;
                                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
                            }).catch(reason => {
                                this.controller.properties['endTime'] = oldTime;
                                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                                console.warn(reason);
                            });
                        }
                    },
                },
            ],
        }
    }

    ngOnInit() {
        this.edge.subscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.componentId, [
            new ChannelAddress(this.componentId, 'TotalPhasePower'),
            new ChannelAddress(this.componentId, 'TotalPhaseTime')
        ]);
    };

    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: mode;

        switch (event.detail.value) {
            case 'MANUAL_ON':
                newMode = 'MANUAL_ON';
                break;
            case 'MANUAL_OFF':
                newMode = 'MANUAL_OFF';
                break;
            case 'AUTOMATIC':
                newMode = 'AUTOMATIC';
                break;
        }

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'mode', value: newMode }
            ]).then(() => {
                currentController.properties.mode = newMode;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties.mode = oldMode;
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                console.warn(reason);
            });
        }
    }

    /**
     * Updates the minimum active time
     *
     * @param event
     */
    updateMinTime(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMinTime = currentController.properties.minTime;
        let newMinTime = event;
        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'minTime', value: newMinTime }
            ]).then(() => {
                currentController.properties.minTime = newMinTime;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties.minTime = oldMinTime;
                console.warn(reason);
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
            });
        }
    }

    /**
     * Updates the minimum energy to be active
     *
     * @param event
     */
    updateMinKwh(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMinKwh = currentController.properties.minkwh;
        let newMinKwh = event;
        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'minkwh', value: newMinKwh }
            ]).then(() => {
                currentController.properties.minkwh = newMinKwh;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties.minkwh = oldMinKwh;
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                console.warn(reason);
            });
        }
    }


    /**  
     * Updates the Charge-Mode of the EVCS-Controller.
     * 
     * @param event 
     */
    updateProcedureMode(event: any, currentController: EdgeConfig.Component) {
        if (this.edge != null) {
            let oldProcedureMode = this.controller.properties['priority'];
            let newProcedureMode: string;

            switch (event) {
                case 'TIME':
                    newProcedureMode = 'TIME';
                    break;
                case 'KILO_WATT_HOUR':
                    newProcedureMode = 'KILO_WATT_HOUR'
                    break;
            }
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'priority', value: newProcedureMode },
            ]).then(() => {
                currentController.properties.priority = newProcedureMode;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties.priority = oldProcedureMode;
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                console.warn(reason);
            });
        }
    }

    updateEndTime(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldTime = currentController.properties['endTime'];
        let newTime = event.detail.value;

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'endTime', value: newTime }
            ]).then(() => {
                currentController.properties['endTime'] = newTime;
                this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
            }).catch(reason => {
                currentController.properties['endTime'] = oldTime;
                this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                console.warn(reason);
            });
        }
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.componentId);
        }
    }
}