import { Component, Input } from '@angular/core';
import { Edge, Service, Websocket, EdgeConfig } from '../../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'evcs-popover',
    templateUrl: './evcs-popover.page.html'
})
export class EvcsPopoverComponent {

    @Input() isChargingStrategy: boolean;
    @Input() controller: EdgeConfig.Component;

    private static readonly SELECTOR = "evcs-popover";

    constructor(
        protected translate: TranslateService,
    ) { }

    ngOnInit() {
    }

    ngOnDestroy() {
    }
}