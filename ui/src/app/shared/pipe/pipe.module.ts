import { DecimalPipe } from "@angular/common";
import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { ClassnamePipe } from "./classname/CLASSNAME.PIPE";
import { ConverterPipe } from "./converter/converter";
import { FormatSecondsToDurationPipe } from "./formatSecondsToDuration/FORMAT_SECONDS_TO_DURATION.PIPE";
import { IsclassPipe } from "./isclass/ISCLASS.PIPE";
import { KeysPipe } from "./keys/KEYS.PIPE";
import { SignPipe } from "./sign/SIGN.PIPE";
import { TimedisplayPipe } from "./timedisplay/TIMEDISPLAY.PIPE";
import { TypeofPipe } from "./typeof/TYPEOF.PIPE";
import { UnitvaluePipe } from "./unitvalue/UNITVALUE.PIPE";
import { VersionPipe } from "./version/VERSION.PIPE";
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

