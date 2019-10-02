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

    private static readonly SELECTOR = "heatingelement-modal";
    customPickerOptions: any;
    selectOptions: any;
    time: any = '17:00';
    timeStandardValue: any = "17:00";
    modimode: string = 'ZEIT';

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
            },
            {
                text: 'Abbrechen',
                role: 'cancel', // has no effect
                handler: (value: any): void => {
                    this.time = this.timeStandardValue;
                    console.log(value, 'cancel');
                },
            }
            ],
        }
        this.selectOptions = {
            buttons: [{
                text: 'OK',
                handler: (value: any): void => {
                    console.log(value, 'ok');
                },
            },
            {
                text: 'Abbrechen',
                role: 'cancel', // has no effect
                handler: (value: any): void => {
                    console.log(value, 'cancel');
                },
            }
            ],
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
}