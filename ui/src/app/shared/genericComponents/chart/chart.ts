import { Component, Input } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { Edge, Service } from "../../shared";

@Component({
    selector: 'oe-chart',
    templateUrl: './chart.html',
})
export class ChartComponent {

    public edge: Edge | null = null;
    @Input() public title: string = '';

    constructor(
        protected service: Service,
        private route: ActivatedRoute
    ) { }
    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        })
    }
}