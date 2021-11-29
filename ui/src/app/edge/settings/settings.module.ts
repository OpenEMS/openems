import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { ChannelsComponent } from './channels/channels.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { ComponentUpdateComponent } from './component/update/update.component';
import { NetworkComponent } from './network/network.component';
import { AliasUpdateComponent } from './profile/aliasupdate.component';
import { ProfileComponent } from './profile/profile.component';
import { SettingsComponent } from './settings.component';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';
import { SystemUpdateComponent } from './systemupdate/systemupdate.component';

@NgModule({
  imports: [
    SharedModule,
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
  ],
  entryComponents: []
})
export class SettingsModule { }
