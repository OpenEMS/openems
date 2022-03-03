import { DecimalPipe } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ClassnamePipe } from './classname/classname.pipe';
import { HasclassPipe } from './hasclass/hasclass.pipe';
import { IsclassPipe } from './isclass/isclass.pipe';
import { KeysPipe } from './keys/keys.pipe';
import { SecToHourMinPipe } from './sectohour/sectohour.pipe';
import { SignPipe } from './sign/sign.pipe';
import { TypeofPipe } from './typeof/typeof.pipe';
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
        ClassnamePipe,
        TypeofPipe
    ],
    declarations: [
        UnitvaluePipe,
        SignPipe,
        SecToHourMinPipe,
        KeysPipe,
        IsclassPipe,
        HasclassPipe,
        ClassnamePipe,
        TypeofPipe
    ],
    exports: [
        UnitvaluePipe,
        SignPipe,
        SecToHourMinPipe,
        KeysPipe,
        IsclassPipe,
        HasclassPipe,
        ClassnamePipe,
        TypeofPipe
    ],
    providers: [
        DecimalPipe,
        SecToHourMinPipe,
        UnitvaluePipe,
        TypeofPipe
    ]
})
export class PipeModule { }
