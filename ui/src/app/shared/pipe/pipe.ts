import { DecimalPipe } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { ClassnamePipe } from './classname/classname.pipe';
import { FormatSecondsToDurationPipe } from './formatSecondsToDuration/formatSecondsToDuration.pipe';
import { IsclassPipe } from './isclass/isclass.pipe';
import { KeysPipe } from './keys/keys.pipe';
import { SignPipe } from './sign/sign.pipe';
import { TypeofPipe } from './typeof/typeof.pipe';
import { UnitvaluePipe } from './unitvalue/unitvalue.pipe';
import { VersionPipe } from './version/version.pipe';

@NgModule({
    imports: [
        BrowserModule,
    ],
    entryComponents: [
        UnitvaluePipe,
        SignPipe,
        FormatSecondsToDurationPipe,
        KeysPipe,
        IsclassPipe,
        ClassnamePipe,
        VersionPipe,
        TypeofPipe,
    ],
    declarations: [
        UnitvaluePipe,
        SignPipe,
        FormatSecondsToDurationPipe,
        KeysPipe,
        IsclassPipe,
        ClassnamePipe,
        VersionPipe,
        TypeofPipe,
    ],
    exports: [
        UnitvaluePipe,
        SignPipe,
        FormatSecondsToDurationPipe,
        KeysPipe,
        IsclassPipe,
        ClassnamePipe,
        VersionPipe,
        TypeofPipe,
    ],
    providers: [
        DecimalPipe,
        FormatSecondsToDurationPipe,
        UnitvaluePipe,
        TypeofPipe,
    ],
})
export class PipeModule { }
