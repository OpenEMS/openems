import { Component, OnInit } from '@angular/core';
import { Service } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'storage-modal',
    templateUrl: './storage-modal.component.html',
})
export class StorageModalComponent implements OnInit {

    constructor(
        public service: Service,
        public translate: TranslateService
    ) { }

    ngOnInit() { }

}