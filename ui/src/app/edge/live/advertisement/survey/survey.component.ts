import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { Edge, Service, Websocket } from '../../../../shared/shared';

@Component({
    selector: SurveyComponent.SELECTOR,
    templateUrl: './survey.component.html'
})
export class SurveyComponent {

    private static readonly SELECTOR = "survey";

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
    }
}