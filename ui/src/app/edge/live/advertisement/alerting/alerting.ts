import { Component, OnInit } from "@angular/core";
import { Edge, Service } from "src/app/shared/shared";

@Component({
    selector: 'alerting',
    templateUrl: './alerting.html'
})
export class AlertingComponent implements OnInit {

    public edge: Edge | null = null;
    constructor(private service: Service) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
        });
    }
}