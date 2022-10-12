import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { AlertingComponent } from './alerting/alerting';
import { FeneconHomeExtensionComponent } from './feneconHomeExtension/feneconHomeExtension';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        AlertingComponent,
        FeneconHomeExtensionComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }