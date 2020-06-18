import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Edge, Service, Websocket } from '../../../shared/shared';

@Component({
    selector: AwattarComponent.SELECTOR,
    templateUrl: './awattar.component.html'
})
export class AwattarComponent {

    private static readonly SELECTOR = "awattar";


    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
        });
    }
}
