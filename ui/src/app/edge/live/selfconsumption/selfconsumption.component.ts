import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { Edge, Service, Utils } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { SelfconsumptionModalComponent } from './modal/modal.component';

@Component({
    selector: 'selfconsumption',
    templateUrl: './selfconsumption.component.html'
})
export class SelfConsumptionComponent {


    public edge: Edge = null;

    constructor(
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
        let x = Math.max(
            Utils.orElse(
                (
                    1 - (
                        Utils.divideSafely(
                            Utils.orElse(0, 0), (
                            Math.max(Utils.orElse(1600, 0), 0)
                        )
                        )
                    )
                ) * 100, 0
            ), 0)
        let y = (
            Utils.divideSafely(
                Utils.orElse(0, 0), (
                Math.max(Utils.orElse(1600, 0), 0)
            )
            )
        )
        console.log("x", x, "y", y)
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: SelfconsumptionModalComponent,
        });
        return await modal.present();
    }
}
