import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { routing, appRoutingProviders }  from './app.routing';
import { AppComponent } from './app.component';

/*
 * Frontend
 */
import { MonitorGrafanaComponent } from './monitor/grafana/grafana.component';
import { MonitorUniversalCurrentComponent } from './monitor/universal/current/universal-current.component';
import { LoginComponent } from './login/login.component';

/*
 * Common components
 */

// Meter
import { CommonMeterSimulatorComponent } from './common/thing/meter/simulator/simulator.component';
import { CommonMeterAsymmetricComponent } from './common/thing/meter/asymmetric/asymmetric.component';
import { CommonMeterSymmetricComponent } from './common/thing/meter/symmetric/symmetric.component';

// Ess
import { CommonEssSimulatorComponent } from './common/thing/ess/simulator/simulator.component';
import { CommonEssFeneconProComponent } from './common/thing/ess/feneconpro/feneconpro.component';
import { CommonEssFeneconCommercialComponent } from './common/thing/ess/feneconcommercial/feneconcommercial.component';

/*
 * Services
 */
import { ConnectionService } from './service/connection.service';
import { LocalstorageService } from './service/localstorage.service';

/*
 * Pipe
 */
import { KeysPipe } from './common/pipe/keys/keys-pipe';

import { ChartsModule } from 'ng2-charts/ng2-charts';
import { CommonSocComponent } from './common/soc/common-soc.component';

@NgModule({
  declarations: [
    AppComponent,
    MonitorGrafanaComponent,
    MonitorUniversalCurrentComponent,
    LoginComponent,
    // common
    CommonSocComponent,
    //   Meter
    CommonMeterSimulatorComponent,
    CommonMeterSymmetricComponent,
    CommonMeterAsymmetricComponent,
    //   Ess
    CommonEssSimulatorComponent,
    CommonEssFeneconProComponent,
    CommonEssFeneconCommercialComponent,
    // pipe
    KeysPipe
  ],
  imports: [
    BrowserModule,
    FormsModule,
    ReactiveFormsModule,
    HttpModule,
    ChartsModule,
    routing,
  ],
  providers: [
    appRoutingProviders,
    ConnectionService,
    LocalstorageService
    /*OdooRPCService*/
    /*{ provide: DataService, useClass: OdooDataService }*/
  ],
  bootstrap: [
    AppComponent
  ]
})
export class AppModule { }
