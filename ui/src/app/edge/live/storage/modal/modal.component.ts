import { Component, OnInit } from '@angular/core';
import { Service, EdgeConfig } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent implements OnInit {


    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            console.log("config:", config)
        })
    }

}