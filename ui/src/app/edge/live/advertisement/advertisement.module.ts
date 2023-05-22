import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { AlertingComponent } from './alerting/alerting';
import { FeneconAvuComponent } from './avu/feneconAvu';
import { FeneconHomeExtensionComponent } from './feneconHomeExtension/feneconHomeExtension';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        AlertingComponent,
        FeneconHomeExtensionComponent,
        FeneconAvuComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }