import { Component, OnInit } from "@angular/core";
import { Edge, Service } from "src/app/shared/shared";

@Component({
    selector: 'fems-app-center',
    templateUrl: './fems-app-center.html'
})
export class FemsAppCenterComponent implements OnInit {
    public edge: Edge | null = null;
    protected updatedRequired: boolean = true;
    constructor(private service: Service) { }

    public ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
            this.updatedRequired = !edge.isVersionAtLeast('2023.3.6');
        })
    }
}