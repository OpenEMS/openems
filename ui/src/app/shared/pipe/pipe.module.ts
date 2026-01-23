import { DecimalPipe } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ClassnamePipe } from "./classname/classname.pipe";
import { ConverterPipe } from "./converter/converter";
import { FormatSecondsToDurationPipe } from "./formatSecondsToDuration/formatSecondsToDuration.pipe";
import { IsclassPipe } from "./isclass/isclass.pipe";
import { KeysPipe } from "./keys/keys.pipe";
import { SignPipe } from "./sign/sign.pipe";
import { TimedisplayPipe } from "./timedisplay/timedisplay.pipe";
import { TypeofPipe } from "./typeof/typeof.pipe";
import { UnitvaluePipe } from "./unitvalue/unitvalue.pipe";
import { VersionPipe } from "./version/version.pipe";
@NgModule({
    declarations: [
        UnitvaluePipe,
        SignPipe,
        FormatSecondsToDurationPipe,
        KeysPipe,
        IsclassPipe,
        ClassnamePipe,
        VersionPipe,
        TypeofPipe,
        ConverterPipe,
        TimedisplayPipe,
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
        ConverterPipe,
        TimedisplayPipe,
    ],
    providers: [
        DecimalPipe,
        FormatSecondsToDurationPipe,
        UnitvaluePipe,
        TypeofPipe,
    ],
})
export class PipeComponentsModule { }

@NgModule({
    imports: [
        BrowserModule,
        PipeComponentsModule,
    ],
    exports: [
        PipeComponentsModule,
    ],
})
export class PipeModule { }

