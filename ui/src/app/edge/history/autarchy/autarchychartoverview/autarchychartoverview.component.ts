import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';

@Component({
    selector: AutarchyChartOverviewComponent.SELECTOR,
    templateUrl: './autarchychartoverview.component.html'
})
export class AutarchyChartOverviewComponent {

    private static readonly SELECTOR = "autarchy-chart-overview";

    public edge: Edge = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }
}