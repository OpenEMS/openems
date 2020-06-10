import { ChannelsComponent } from './channels/channels.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { ComponentUpdateComponent } from './component/update/update.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { NetworkComponent } from './network/network.component';
import { NgModule } from '@angular/core';
import { ProfileComponent } from './profile/profile.component';
import { ProfilePopoverComponent } from './profile/popover/popover.component';
import { SettingsComponent } from './settings.component';
import { SharedModule } from './../../shared/shared.module';
import { SystemExecuteComponent } from './systemexecute/systemexecute.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    ChannelsComponent,
    ComponentInstallComponent,
    ComponentInstallIndexComponent,
    ComponentUpdateComponent,
    ComponentUpdateIndexComponent,
    NetworkComponent,
    ProfileComponent,
    ProfilePopoverComponent,
    SettingsComponent,
    SystemExecuteComponent,
  ],
  entryComponents: [
    ProfilePopoverComponent,
  ]
})
export class SettingsModule { }
