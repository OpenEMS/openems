import { NgModule } from '@angular/core';
import { SharedModule } from 'src/app/shared/shared.module';
import { InstallAppComponent } from './install.component';
import { IndexComponent } from './index.component';
import { SingleAppComponent } from './single.component';
import { UpdateAppComponent } from './update.component';
import { KeyModalComponent } from './keypopup/modal.component';
import { FormControl, ValidationErrors } from '@angular/forms';
import { FormlyModule } from '@ngx-formly/core';

export function KeyValidator(control: FormControl): ValidationErrors {
  return /^(.{4}-){3}.{4}$/.test(control.value) ? null : { 'key': true };
}

@NgModule({
  imports: [
    SharedModule,
    FormlyModule.forRoot({
      validators: [
        { name: 'key', validation: KeyValidator }
      ],
      validationMessages: [
        { name: 'key', message: "The key doesnt match the pattern!" }
      ]
    })
  ],
  declarations: [
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
    KeyModalComponent,
  ],
  exports: [
    IndexComponent,
    InstallAppComponent,
    SingleAppComponent,
    UpdateAppComponent,
  ],
})
export class AppModule { }
