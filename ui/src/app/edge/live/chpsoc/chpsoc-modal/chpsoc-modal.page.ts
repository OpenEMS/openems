import { Component, OnInit, HostListener, Input, OnChanges } from '@angular/core';
import { environment } from 'src/environments/openems-backend-dev-local';
import { PopoverController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';


export class EvcsModalComponent implements OnInit {

    @Input() edge: Edge;
    @Input() controller: EdgeConfig.Component = null;
    @Input() private componentId: string;



    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        private modalCtrl: ModalController,
    ) { }

    ngOnInit() {
    }

    cancel() {
        this.modalCtrl.dismiss();
    }


}