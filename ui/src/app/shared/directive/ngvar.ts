import { Directive, Input, TemplateRef, ViewContainerRef } from "@angular/core";

@Directive({
    selector: "[ngVar]",
    standalone: false,
})
export class VarDirective {
    private context: {
        $implicit: unknown;
        ngVar: unknown;
    } = {
            $implicit: null,
            ngVar: null,
        };

    private hasView: boolean = false;

    constructor(
        private templateRef: TemplateRef<any>,
        private vcRef: ViewContainerRef,
    ) { }

    @Input()
    set ngVar(context: unknown) {
        THIS.CONTEXT.$implicit = THIS.CONTEXT.NG_VAR = context;

        if (!THIS.HAS_VIEW) {
            THIS.VC_REF.CREATE_EMBEDDED_VIEW(THIS.TEMPLATE_REF, THIS.CONTEXT);
            THIS.HAS_VIEW = true;
        }
    }

}
