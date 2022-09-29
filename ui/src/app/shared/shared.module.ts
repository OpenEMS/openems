import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { RouterModule } from '@angular/router';
import { IonicModule } from '@ionic/angular';
import { FormlyModule } from '@ngx-formly/core';
import { FormlyIonicModule } from '@ngx-formly/ionic';
import { TranslateModule } from '@ngx-translate/core';
import { ChartsModule } from 'ng2-charts';
import { NgxSpinnerModule } from "ngx-spinner";
import { appRoutingProviders } from './../app-routing.module';
import { ChartOptionsComponent } from './chartoptions/chartoptions.component';
import { DirectiveModule } from './directive/directive';
import { MeterModule } from './edge/meter/meter.module';
import { FormlyWrapperFormFieldComponent } from './formly/form-field.wrapper';
import { FormlySelectFieldWrapperComponent } from './formly/formly-select-field.wrapper';
import { InputTypeComponent } from './formly/input';
import { FormlyInputSerialNumberWrapperComponent as FormlyWrapperInputSerialNumber } from './formly/input-serial-number-wrapper';
import { RepeatTypeComponent } from './formly/repeat';
import { Generic_ComponentsModule } from './genericComponents/genericComponents';
import { HeaderComponent } from './header/header.component';
import { PercentageBarComponent } from './percentagebar/percentagebar.component';
import { PipeModule } from './pipe/pipe';
import { Logger } from './service/logger';
import { Service } from './service/service';
import { Utils } from './service/utils';
import { Websocket } from './service/websocket';

@NgModule({
  imports: [
    BrowserAnimationsModule,
    ChartsModule,
    CommonModule,
    DirectiveModule,
    FormsModule,
    IonicModule,
    NgxSpinnerModule.forRoot({
      type: 'ball-clip-rotate-multiple'
    }),
    ReactiveFormsModule,
    RouterModule,
    FormlyModule.forRoot({
      wrappers: [
        { name: 'form-field', component: FormlyWrapperFormFieldComponent },
        { name: "input-serial-number", component: FormlyWrapperInputSerialNumber },
        { name: 'formly-select-field-wrapper', component: FormlySelectFieldWrapperComponent }
      ],
      types: [
        { name: 'input', component: InputTypeComponent },
        { name: 'repeat', component: RepeatTypeComponent },
      ],
    }),
    PipeModule,
    Generic_ComponentsModule
  ],
  declarations: [
    // components
    ChartOptionsComponent,
    HeaderComponent,
    PercentageBarComponent,
    // formly
    InputTypeComponent,
    FormlyWrapperFormFieldComponent,
    RepeatTypeComponent,
    FormlyWrapperInputSerialNumber,
    FormlySelectFieldWrapperComponent
  ],
  exports: [
    // modules
    BrowserAnimationsModule,
    ChartsModule,
    CommonModule,
    DirectiveModule,
    FormlyIonicModule,
    FormlyModule,
    FormsModule,
    IonicModule,
    NgxSpinnerModule,
    ReactiveFormsModule,
    RouterModule,
    TranslateModule,
    PipeModule,
    Generic_ComponentsModule,
    MeterModule,
    // components
    ChartOptionsComponent,
    HeaderComponent,
    PercentageBarComponent,
  ],
  providers: [
    appRoutingProviders,
    Service,
    Utils,
    Websocket,
    Logger
  ]
})

export class SharedModule { }
