import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { AlertingComponent } from './alerting/alerting';
import { GlsCrowdfundingComponent } from './glsCrowdfunding/glsCrowdfunding';
import { HeatingElementComponent } from './heatingelement/heatingelement';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        AlertingComponent,
        HeatingElementComponent,
        GlsCrowdfundingComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }