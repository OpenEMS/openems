import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { AdvertisementComponent } from './advertisement.component';
import { AlertingComponent } from './alerting/alerting';
import { FeneconAvuComponent } from './avu/feneconAvu';
import { FemsAppCenterComponent } from './fems-app-center/fems-app-center';
import { FeneconHomeExtensionComponent } from './feneconHomeExtension/feneconHomeExtension';

@NgModule({
    imports: [
        SharedModule
    ],
    declarations: [
        AdvertisementComponent,
        AlertingComponent,
        FeneconHomeExtensionComponent,
        FeneconAvuComponent,
        FemsAppCenterComponent
    ],
    exports: [
        AdvertisementComponent
    ]
})
export class AdvertisementModule { }