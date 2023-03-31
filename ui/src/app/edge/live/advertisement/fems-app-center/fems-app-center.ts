import { Component } from "@angular/core";
import { Edge, Service } from "src/app/shared/shared";

@Component({
    selector: 'fems-app-center',
    templateUrl: './fems-app-center.html'
})
export class FemsAppCenterComponent {
    public edge: Edge | null = null;
    protected updatedRequired: boolean = true;
    constructor(private service: Service) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.updatedRequired = !edge.isVersionAtLeast('2023.3.6');
        })
    }
}