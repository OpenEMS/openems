import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { AppModule } from './app/app.module';
import { ChannelsComponent } from './channels/channels.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { ComponentUpdateComponent } from './component/update/update.component';
import { NetworkComponent } from './network/network.component';
import { AliasUpdateComponent } from './profile/aliasupdate.component';
import { ProfileComponent } from './profile/profile.component';
import { ServiceAssistantModule } from './serviceassistant/serviceassistant.module';
import { SettingsComponent } from './settings.component';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';
import { SystemUpdateOldComponent } from './systemupdate.old/systemupdate.old.component';
import { SystemUpdateComponent } from './systemupdate/systemupdate.component';
import { AlertingComponent } from './alerting/alerting.component';

@NgModule({
  imports: [
    AppModule,
    SharedModule,
    ServiceAssistantModule
  ],
  declarations: [
    AliasUpdateComponent,
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    NetworkComponent,
    ProfileComponent,
    SettingsComponent,
    SystemExecuteComponent,
    SystemUpdateComponent,
    SystemUpdateOldComponent,
    AlertingComponent,
  ],
  entryComponents: [
  ]
})
export class SettingsModule { }
