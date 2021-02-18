import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../shared/shared';
import { PredictionModalComponent } from './modal/modal.component';

@Component({
    selector: 'prediction',
    templateUrl: './prediction.component.html'
})
export class PredictionComponent {

    constructor(
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route);
    };

    ngOnDestroy() {
    }

    async presentModal() {
        const modal = await this.modalCtrl.create({
            component: PredictionModalComponent,
            componentProps: {}
        });
        return await modal.present();
    }
}
