import { AliasUpdateComponent } from './profile/aliasupdate.component';
import { AutoinstallerComponent } from './autoinstaller/autoinstaller.component';
import { ChannelsComponent } from './channels/channels.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { ComponentUpdateComponent } from './component/update/update.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { NetworkComponent } from './network/network.component';
import { NgModule } from '@angular/core';
import { ProfileComponent } from './profile/profile.component';
import { SettingsComponent } from './settings.component';
import { SharedModule } from './../../shared/shared.module';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';
import { HeatingElementRtuInstallerComponent } from './autoinstaller/heatingelementrtu/heatingelementrtu.component';
import { HeatingElementTcpInstallerComponent } from './autoinstaller/heatingelementtcp/heatingelementtcp.component';
import { EvcsInstallerComponent } from './autoinstaller/evcs/evcs.component';
import { HeatingpumpTcpInstallerComponent } from './autoinstaller/heatingpumptcp/heatingpumptcp.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    AliasUpdateComponent,
    AutoinstallerComponent,
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    NetworkComponent,
    ProfileComponent,
    SettingsComponent,
    SystemExecuteComponent,
    HeatingElementRtuInstallerComponent,
    HeatingElementTcpInstallerComponent,
    EvcsInstallerComponent,
    HeatingpumpTcpInstallerComponent,
  ],
  entryComponents: [
    HeatingElementRtuInstallerComponent,
    HeatingElementTcpInstallerComponent,
    EvcsInstallerComponent,
    HeatingpumpTcpInstallerComponent,
  ]
})
export class SettingsModule { }
