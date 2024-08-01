import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { VarDirective } from './ngvar';
import { AutofillDirective } from './autofill';

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
