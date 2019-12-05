import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

type mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';


@Component({
    selector: HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
})
export class HeatingElementModalComponent implements OnInit {


    @Input() public edge: Edge;
    @Input() public controller: EdgeConfig.Component;
    @Input() public outputChannelPhaseOne: ChannelAddress;
    @Input() public outputChannelPhaseTwo: ChannelAddress;
    @Input() public outputChannelPhaseThree: ChannelAddress;

    private static readonly SELECTOR = "heatingelement-modal";
    customPickerOptions: any;
    selectOptions: any;
    time: any = '17:00';
    timeStandardValue: any = "17:00";
    modimode: string = 'ZEIT';

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
    ) {
        this.customPickerOptions = {
            buttons: [{
                text: 'OK',
                handler: (value: any): void => {
                    if (this.edge != null) {
                        let oldTime = this.controller.properties['endTime'];
                        this.edge.updateComponentConfig(this.websocket, this.controller.id, [
                            { name: 'endTime', value: value['hour'].value.toString() + ':' + value['minute'].value.toString() }
                        ]).then(() => {
                            this.controller.properties['endTime'] = value['hour'].value.toString() + ':' + value['minute'].value.toString();
                            this.service.toast(this.translate.instant('General.ChangeAccepted'), 'success');
                        }).catch(reason => {
                            this.controller.properties['endTime'] = oldTime;
                            this.service.toast(this.translate.instant('General.ChangeFailed') + '\n' + reason, 'danger');
                            console.warn(reason);
                        });
                    }
                },
            },
            {
                text: 'Abbrechen',
                role: 'cancel', // has no effect
                handler: (value: any): void => { },
            }
            ],
        }
    }

    ngOnInit() { };

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

    showData() {
        console.log(this.controller.properties['endTime'].slice(0, -3))
    }

    ngOnDestroy() { }
}