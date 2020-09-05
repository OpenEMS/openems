import { ActivatedRoute } from '@angular/router';
import { AsymmetricPeakshavingModalComponent } from './modal/modal.component';
import { BehaviorSubject, Subject } from 'rxjs';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';
import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { takeUntil } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: AsymmetricPeakshavingComponent.SELECTOR,
    templateUrl: './asymmetricpeakshaving.component.html'
})
export class AsymmetricPeakshavingComponent {

    private static readonly SELECTOR = "asymmetricpeakshaving";

    @Input() private componentId: string = '';

    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;
    public mostStressedPhase: BehaviorSubject<{ name: 'L1' | 'L2' | 'L3' | '', value: number }> = new BehaviorSubject<{ name: 'L1' | 'L2' | 'L3' | '', value: number }>({ name: "", value: 0 });
    private stopOnDestroy: Subject<void> = new Subject<void>();

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        protected translate: TranslateService,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            if (edge != null) {
                this.edge = edge;
                this.service.getConfig().then(config => {
                    this.component = config.getComponent(this.componentId);
                    let meterId = this.component.properties['meter.id'];
                    edge.subscribeChannels(this.websocket, AsymmetricPeakshavingComponent.SELECTOR, [
                        new ChannelAddress(meterId, 'ActivePower'),
                        new ChannelAddress(meterId, 'ActivePowerL1'),
                        new ChannelAddress(meterId, 'ActivePowerL2'),
                        new ChannelAddress(meterId, 'ActivePowerL3')
                    ])
                    edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                        let activePowerL1 = currentData.channel[meterId + '/ActivePowerL1'];
                        let activePowerL2 = currentData.channel[meterId + '/ActivePowerL2'];
                        let activePowerL3 = currentData.channel[meterId + '/ActivePowerL3'];
                        let name: 'L1' | 'L2' | 'L3' | '' = '';
                        let value = null;
                        if (activePowerL1 > activePowerL2 && activePowerL1 > activePowerL3) {
                            name = 'L1';
                            value = activePowerL1;
                        } else if (activePowerL2 > activePowerL1 && activePowerL2 > activePowerL3) {
                            name = 'L2';
                            value = activePowerL2;
                        } else if (activePowerL3 > activePowerL1 && activePowerL3 > activePowerL1) {
                            name = 'L3';
                            value = activePowerL3;
                        }
                        if (value < 0) {
                            name = '';
                            value = null;
                        }
                        this.mostStressedPhase.next({ name: name, value: value });
                    });
                });
            }
        });
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: AsymmetricPeakshavingModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                mostStressedPhase: this.mostStressedPhase
            }
        });
        return await modal.present();
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, AsymmetricPeakshavingComponent.SELECTOR);
        }
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }
}