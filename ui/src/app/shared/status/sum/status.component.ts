import { Component, Input } from '@angular/core';
import { Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { StatusSingleComponent } from '../single/status.component';
import { Edge } from '../../edge/edge';

type stateAllowance = 'admin' | 'user' | 'none';

@Component({
    selector: StatusSumComponent.SELECTOR,
    templateUrl: './status.component.html'
})
export class StatusSumComponent {

    @Input() public edge: Edge;

    private static readonly SELECTOR = "status-sum";

    public stateAllowance: stateAllowance = 'none';

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        console.log(this.edge.roleIsAtLeast('admin'))
        if (this.edge.roleIsAtLeast('admin')) {
            this.stateAllowance = 'admin';
        } else if (this.edge.roleIsAtLeast('user') && this.isProducttypeAllowed(this.edge)) {
            this.stateAllowance = 'user';
        } else {
            this.stateAllowance = 'none';
        }
        console.log("state", this.stateAllowance)
    }

    private isProducttypeAllowed(edge: Edge) {
        if (edge.producttype == 'Pro 9-12' || edge.producttype.includes('MiniES')) {
            return true;
        } else {
            return false;
        }
    }

    async presentSingleStatusModal() {
        console.log("edge", this.edge)
        const modal = await this.modalCtrl.create({
            component: StatusSingleComponent,
        });
        return await modal.present();
    }
}