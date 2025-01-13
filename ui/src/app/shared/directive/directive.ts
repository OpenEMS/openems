import { NgModule } from "@angular/core";
import { BrowserModule } from "@angular/platform-browser";
import { AutofillDirective } from "./autofill";
import { VarDirective } from "./ngvar";

@NgModule({
    imports: [
        BrowserModule,
    ],
    declarations: [
        VarDirective,
        AutofillDirective,
    ],
    exports: [
        VarDirective,
        AutofillDirective,
    ],
    providers: [
        VarDirective,
        AutofillDirective,
    ],
})
export class DirectiveModule { }
