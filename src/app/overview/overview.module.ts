import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { MaterialModule, MdSnackBar } from '@angular/material';
import { FlexLayoutModule } from '@angular/flex-layout';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import 'hammerjs';

import { OverviewComponent } from './overview.component';
import { SharedModule } from './../shared/shared.module';

//import { ConfigComponent } from './config/config.component';

/*
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorUniversalCurrentComponent } from './monitor/universal/current/universal-current.component';
import { MonitorDetailComponent } from './monitor/detail/detail.component';
import { ConfigurationComponent } from './monitor/configuration/configuration.component';
*/

/*
 * Services
 */
import { WebappService } from './../service/webapp.service';
import { WebsocketService } from './../service/websocket.service';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    OverviewComponent,
  ],
  providers: [
    MdSnackBar,
    WebappService,
    WebsocketService
  ],
  bootstrap: [
  ]
})
export class OverviewModule { }
