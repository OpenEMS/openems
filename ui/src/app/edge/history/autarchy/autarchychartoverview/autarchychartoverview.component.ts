import { Component, Input } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { Service, Edge } from '../../../../shared/shared';
import { ActivatedRoute } from '@angular/router';

@Component({
    selector: AutarchyChartOverviewComponent.SELECTOR,
    templateUrl: './autarchychartoverview.component.html'
})
export class AutarchyChartOverviewComponent {

    @Input() public period: DefaultTypes.HistoryPeriod;

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