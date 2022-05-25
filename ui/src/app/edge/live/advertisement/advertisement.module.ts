import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { GlsCrowdfunding } from './glsCrowdFunding/glscrowdfunding';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        GlsCrowdfunding
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }