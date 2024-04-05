import { NgModule } from '@angular/core';
import { ChangelogModule } from 'src/app/changelog/changelog.module';

import { SharedModule } from './../../shared/shared.module';
import { AlertingComponent } from './alerting/alerting.component';
import { AppModule } from './app/app.module';
import { ChannelsComponent } from './channels/channels.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { ComponentUpdateComponent } from './component/update/update.component';
import { NetworkComponent } from './network/network.component';
import { AliasUpdateComponent } from './profile/aliasupdate.component';
import { ProfileComponent } from './profile/profile.component';
import { SettingsComponent } from './settings.component';
import { MaintenanceComponent } from './system/maintenance/maintenance';
import { OeSystemUpdateComponent } from './system/oe-system-update.component';
import { SystemComponent } from './system/system.component';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';

@NgModule({
  imports: [
    AppModule,
    SharedModule,
    ChangelogModule,
  ],
  declarations: [
    AlertingComponent,
    AliasUpdateComponent,
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    MaintenanceComponent,
    NetworkComponent,
    OeSystemUpdateComponent,
    ProfileComponent,
    SettingsComponent,
    SystemComponent,
    SystemExecuteComponent,
  ],
  entryComponents: [
  ],
  exports: [
    OeSystemUpdateComponent,
  ],
})
export class SettingsModule { }
