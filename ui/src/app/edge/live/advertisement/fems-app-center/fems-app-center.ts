import { Component } from "@angular/core";
import { Edge, Service } from "src/app/shared/shared";

@Component({
    selector: 'fems-app-center',
    templateUrl: './fems-app-center.html'
})
export class FemsAppCenterComponent {
    public edge: Edge | null = null;
    constructor(private service: Service) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
        })
    }
}