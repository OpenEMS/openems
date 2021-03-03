import { ActivatedRoute } from '@angular/router';
import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';

@Component({
    selector: LukasChartOverviewComponent.SELECTOR,
    templateUrl: './lukaschartoverview.component.html'
})
export class LukasChartOverviewComponent {

    private static readonly SELECTOR = "lukas-chart-overview";

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