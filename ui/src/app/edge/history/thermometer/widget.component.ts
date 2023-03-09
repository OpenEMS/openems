import { ActivatedRoute } from '@angular/router';
import { Edge, Service } from '../../../shared/shared';
import { Component, Input, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
    selector: ThermometerWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ThermometerWidgetComponent implements OnInit {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "thermometerWidget";

    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
        });
    }
}

