import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { GlsCrowdfunding } from './glsCrowdFunding/glscrowdfunding';
import { GridOptimizedCharge } from './gridOptimizedCharge/gridOptimizedCharge';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        GlsCrowdfunding,
        GridOptimizedCharge
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }