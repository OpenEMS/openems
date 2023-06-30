import { Component, OnInit } from '@angular/core';
import { Edge, Service } from 'src/app/shared/shared';

@Component({
    selector: 'offline',
    templateUrl: './offline.component.html'
})
export class OfflineComponent implements OnInit {

    public edge: Edge | null = null;

    constructor(
        public service: Service
    ) { }

    ngOnInit() {
        this.service.getCurrentEdge().then(edge => {
            this.edge = edge;
        });
    }
}
