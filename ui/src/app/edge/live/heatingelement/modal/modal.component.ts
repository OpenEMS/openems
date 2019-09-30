import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

type mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';


@Component({
    selector: HeatingElementModalComponent.SELECTOR,
    templateUrl: './modal.component.html',
    styleUrls: ['./modal.component.scss']
})
export class HeatingElementModalComponent implements OnInit {

    private static readonly SELECTOR = "heatingelement-modal";
    customPickerOptions: any;
    time: any = '17:00';
    timeStandardValue: any = "17:00";

    @Input() public edge: Edge;
    @Input() public controller: EdgeConfig.Component;
    @Input() public componentId: string;
    @Input() public outputChannel: ChannelAddress;
    @Input() public inputChannel: ChannelAddress;

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
                    this.time = value;
                    console.log(value, 'ok');
                },
            }, {
                text: 'Abbrechen',
                role: 'cancel', // has no effect
                class: 'vollrot',
                handler: (value: any): void => {
                    this.time = this.timeStandardValue;
                    console.log(value, 'cancel');
                },
            }],
        }
    }

    ngOnInit() {
        this.edge.subscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.componentId, [
            new ChannelAddress(this.controller.id, "Mode")
        ]);
    };

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, HeatingElementModalComponent.SELECTOR + this.componentId);
    }

    cancel() {
        this.modalCtrl.dismiss();
    }



    /**  
    * Updates the Charge-Mode of the EVCS-Controller.
    * 
    * @param event 
    */
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
            }).catch(reason => {
                currentController.properties.mode = oldMode;
                console.warn(reason);
            });
        }
    }
}