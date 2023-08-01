import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';

import { ChartComponent } from './chart/chart';
import { FlatComponent } from './flat/flat';
import { OverviewComponent } from './overview/overview';

@NgModule({
    imports: [
        BrowserModule,
        SharedModule
    ],
    entryComponents: [
        FlatComponent,
        OverviewComponent,
        ChartComponent
    ],
    declarations: [
        FlatComponent,
        OverviewComponent,
        ChartComponent
    ],
    exports: [
        FlatComponent,
        OverviewComponent,
        ChartComponent
    ]
})
export class Common_SymmetricPeakShaving { }