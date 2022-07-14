import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { Alerting } from './alerting/alerting';
import { HeatingElement } from './heatingelement/heatingelement';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        Alerting,
        HeatingElement
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }