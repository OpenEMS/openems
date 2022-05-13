import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { InstallAppComponent } from './install.component';
import { IndexComponent } from './index.component';
import { SingleAppComponent } from './single.component';
import { UpdateAppComponent } from './update.component';

@NgModule({
  imports: [
    SharedModule,
  ],
  declarations: [
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
  ],
  exports: [
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
  ],
})
export class AppModule { }
