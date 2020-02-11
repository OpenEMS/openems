import { Component, Input } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket, EdgeConfig, ChannelAddress } from '../../../../shared/shared';
import { AsymmetricPeakshavingModalComponent } from './modal/modal.component';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: AsymmetricPeakshavingComponent.SELECTOR,
    templateUrl: './asymmetricpeakshaving.component.html'
})
export class AsymmetricPeakshavingComponent {

    private static readonly SELECTOR = "asymmetricpeakshaving";

    @Input() private componentId: string;


    public edge: Edge = null;

    public component: EdgeConfig.Component = null;

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
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
                this.edge.subscribeChannels(this.websocket, AsymmetricPeakshavingComponent.SELECTOR, [
                    new ChannelAddress(this.component.properties['meter.id'], 'ActivePower'),
                    new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL1'),
                    new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL2'),
                    new ChannelAddress(this.component.properties['meter.id'], 'ActivePowerL3')
                ])
            });
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AsymmetricPeakshavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge
            }
        });
        return await modal.present();
    }

    getMostStressedPhaseValue(): string {
        if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] >= 0) {
            return this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1']
        } else if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] >= 0) {
            return this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2']
        } else if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] >= 0) {
            return this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3']
        } else {
            return '-'
        }
    }

    getMostStressedPhaseL(): string {
        if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1'] >= 0) {
            return 'L1'
        } else if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2'] >= 0) {
            return 'L2'
        } else if (this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL1']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] > this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL2']
            && this.edge.currentData['_value'].channel[this.component.properties['meter.id'] + '/ActivePowerL3'] >= 0) {
            return 'L3'
        } else {
            return '-'
        }
    }

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, AsymmetricPeakshavingComponent.SELECTOR);
    }
}