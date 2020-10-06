import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, Websocket, ChannelAddress } from 'src/app/shared/shared';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { DigitalInputModalComponent } from './modal/modal.component';

@Component({
    selector: 'digitalinput',
    templateUrl: './digitalinput.component.html'
})

export class DigitalInputComponent {

    private static readonly SELECTOR = "digitalinput";

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public ioComponents: EdgeConfig.Component[] = null;
    public ioComponentCount = 0;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        protected translate: TranslateService,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });

        this.service.getConfig().then(config => {
            this.config = config;
            let channels = [];

            this.ioComponents = config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput").filter(component => component.isEnabled);
            for (let component of this.ioComponents) {

                for (let channel in component.channels) {
                    channels.push(
                        new ChannelAddress(component.id, channel)
                    );
                }



            }
            this.edge.subscribeChannels(this.websocket, DigitalInputComponent.SELECTOR, channels);
            this.ioComponentCount = this.ioComponents.length;
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, DigitalInputComponent.SELECTOR);
        }
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: DigitalInputModalComponent,
            componentProps: {
                edge: this.edge,
                ioComponents: this.ioComponents,
            }
        });
        return await modal.present();
    }

}