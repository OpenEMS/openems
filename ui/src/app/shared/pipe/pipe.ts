import { DecimalPipe } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ClassnamePipe } from './classname/classname.pipe';
import { HasclassPipe } from './hasclass/hasclass.pipe';
import { IsclassPipe } from './isclass/isclass.pipe';
import { KeysPipe } from './keys/keys.pipe';
import { SecToHourMinPipe } from './sectohour/sectohour.pipe';
import { SignPipe } from './sign/sign.pipe';
import { UnitvaluePipe } from './unitvalue/unitvalue.pipe';

@NgModule({
    imports: [
        BrowserModule,
    ],
    entryComponents: [
        UnitvaluePipe,
        SignPipe,
        SecToHourMinPipe,
        KeysPipe,
        IsclassPipe,
        HasclassPipe,
        ClassnamePipe
    ],
    declarations: [
        UnitvaluePipe,
        SignPipe,
        SecToHourMinPipe,
        KeysPipe,
        IsclassPipe,
        HasclassPipe,
        ClassnamePipe
    ],
    exports: [
        UnitvaluePipe,
        SignPipe,
        SecToHourMinPipe,
        KeysPipe,
        IsclassPipe,
        HasclassPipe,
        ClassnamePipe
    ],
    providers: [
        DecimalPipe,
        SecToHourMinPipe,
        UnitvaluePipe,
    ]
})
export class PipeModule {

}
