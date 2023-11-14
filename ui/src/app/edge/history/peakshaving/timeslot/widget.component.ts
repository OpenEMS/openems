import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { Edge, EdgeConfig, Service } from 'src/app/shared/shared';

@Component({
    selector: TimeslotPeakshavingWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html',
})
export class TimeslotPeakshavingWidgetComponent implements OnInit {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private static readonly SELECTOR = "timeslotPeakshavingWidget";

    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }
}

