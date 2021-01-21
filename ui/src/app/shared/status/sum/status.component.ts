import { Component, Input } from '@angular/core';
import { Edge } from '../../edge/edge';
import { ModalController } from '@ionic/angular';
import { Service } from '../../../shared/shared';
import { StatusSingleComponent } from '../single/status.component';


@Component({
    selector: StatusSumComponent.SELECTOR,
    templateUrl: './status.component.html'
})
export class StatusSumComponent {

    @Input() public edge: Edge;

    private static readonly SELECTOR = "status-sum";

    public roleState: string = 'none';

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        this.roleState = this.service.getInfoStates(this.edge);
    }


    async presentSingleStatusModal() {
        const modal = await this.modalCtrl.create({
            component: StatusSingleComponent,
            componentProps: {
                roleState: this.roleState
            }
        });
        return await modal.present();
    }
}