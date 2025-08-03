import { Directive, Input, TemplateRef, ViewContainerRef, inject } from "@angular/core";

@Directive({
    selector: "[ngVar]",
    standalone: false,
})
export class VarDirective {
    private templateRef = inject<TemplateRef<any>>(TemplateRef);
    private vcRef = inject(ViewContainerRef);

    private context: {
        $implicit: unknown;
        ngVar: unknown;
    } = {
            $implicit: null,
            ngVar: null,
        };

    private hasView: boolean = false;

    /** Inserted by Angular inject() migration for backwards compatibility */
    constructor(...args: unknown[]);

    constructor() { }

    @Input()
    set ngVar(context: unknown) {
        this.context.$implicit = this.context.ngVar = context;

        if (!this.hasView) {
            this.vcRef.createEmbeddedView(this.templateRef, this.context);
            this.hasView = true;
        }
    }

}
