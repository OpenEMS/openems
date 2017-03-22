import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MaterialModule } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';

import { DeviceHistoryComponent } from './history.component';
import { SocChartComponent } from './chart/socchart/socchart.component';

import { routing, appRoutingProviders } from './../../app.routing';
import { WebappService, WebsocketService } from './../../shared/shared';

@NgModule({
  imports: [
    CommonModule,
    MaterialModule.forRoot(),
    FlexLayoutModule.forRoot(),
    NgxChartsModule
  ],
  declarations: [
    DeviceHistoryComponent,
    SocChartComponent
  ],
  exports: [
    DeviceHistoryComponent
  ],
  providers: [
    appRoutingProviders,
    WebappService,
    WebsocketService
  ]
})
export class DeviceHistoryModule { }



