import { Component, Input } from '@angular/core';
import { Service } from '../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { StatusSingleComponent } from '../single/status.component';
import { Edge } from '../../edge/edge';

type RoleState = 'admin' | 'user' | 'none';

@Component({
    selector: StatusSumComponent.SELECTOR,
    templateUrl: './status.component.html'
})
export class StatusSumComponent {

    @Input() public edge: Edge;

    private static readonly SELECTOR = "status-sum";

    public roleState: RoleState = 'none';

    constructor(
        public modalCtrl: ModalController,
        public service: Service,
    ) { }

    ngOnInit() {
        ///////////////////////////////////////////
        //this.service.isStatusAllowed(this.edge)//
        ///////////IS FENECON ONLY CODE////////////
        //////////// CARE WHEN REVIEW//////////////
        ///////////////////////////////////////////
        if (this.edge != null) {
            if (this.edge.roleIsAtLeast('admin')) {
                this.roleState = 'admin';
            } else if (this.edge.roleIsAtLeast('user') && this.service.isStatusAllowed(this.edge)) {
                this.roleState = 'user';
            } else {
                this.roleState = 'none';
            }
        }
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