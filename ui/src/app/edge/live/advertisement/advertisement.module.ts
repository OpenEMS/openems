import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { Alerting } from './alerting/alerting';
import { EnergyRevolutionWithFems } from './energyRevolutionWithFems/energyRevolutionWithFems';
import { GlsCrowdfunding } from './glsCrowdFunding/glscrowdfunding';
import { GridOptimizedCharge } from './gridOptimizedCharge/gridOptimizedCharge';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        GlsCrowdfunding,
        GridOptimizedCharge,
        EnergyRevolutionWithFems,
        Alerting
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }