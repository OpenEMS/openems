import { NgModule } from '@angular/core';
import { SharedModule } from './../../shared/shared.module';
import { SettingsComponent } from './settings.component';
import { SystemLogComponent } from './systemlog/systemlog.component';
import { IndexComponent as ComponentInstallIndexComponent } from './component/install/index.component';
import { ComponentInstallComponent } from './component/install/install.component';
import { IndexComponent as ComponentUpdateIndexComponent } from './component/update/index.component';
import { ComponentUpdateComponent } from './component/update/update.component';

@NgModule({
  imports: [
    SharedModule
  ],
  declarations: [
    SettingsComponent,
    SystemLogComponent,
    ComponentInstallIndexComponent,
    ComponentInstallComponent,
    ComponentUpdateIndexComponent,
    ComponentUpdateComponent
  ]
})
export class SettingsModule { }
