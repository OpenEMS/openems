import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { SharedModule } from 'src/app/shared/shared.module';
import { Flat } from './flat/flat';
import { Chart } from './chart/chart';
import { AutarchyChartOverviewComponent } from './overview/overview';

@NgModule({
    imports: [
        BrowserModule,
        SharedModule,
    ],
    entryComponents: [
        Flat,
    ],
    declarations: [
        Flat,
        Chart,
        AutarchyChartOverviewComponent
    ],
    exports: [
        Flat,
        Chart,
        AutarchyChartOverviewComponent
    ]
})
export class Autarchy { }
