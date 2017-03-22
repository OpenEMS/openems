import { NgModule } from '@angular/core';
import { MdSnackBar } from '@angular/material';

import { environment } from '../environments';
import { appRoutingProviders } from './app.routing';

import { SharedModule } from './shared/shared.module';
import { WebappService, WebsocketService } from './shared/shared';
import { AboutModule } from './about/about.module';
import { LoginModule } from './login/login.module';
import { DeviceModule } from './device/device.module';
import { OverviewModule } from './overview/overview.module';

import { AppComponent } from './app.component';

@NgModule({
  imports: [
    SharedModule,
    AboutModule,
    DeviceModule,
    LoginModule,
    OverviewModule
  ],
  declarations: [
    AppComponent
  ],
  bootstrap: [
    AppComponent
  ],
  providers: [
    appRoutingProviders,
    MdSnackBar,
    WebappService,
    WebsocketService
  ]
})
export class AppModule { }
