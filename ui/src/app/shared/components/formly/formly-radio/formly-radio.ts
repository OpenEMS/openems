import { ChangeDetectionStrategy, Component, OnChanges, OnInit, SimpleChanges } from "@angular/core";
import { FieldType } from "@ngx-formly/core";

@Component({
    selector: "formly-radio",
    templateUrl: "./formly-radio.html",
    standalone: false,
    changeDetection: ChangeDetectionStrategy.OnPush,
    styles: [
        `
    :host {
      width: 100%;
    }
    `,
    ],
})
export class FormlyRadioTypeComponent extends FieldType implements OnInit, OnChanges {

    protected fieldOptions: any[] = [];
    protected defaultOption: any | undefined = undefined;

    public ngOnInit(): void {
        this.updateFieldOptions();
    }

    public ngOnChanges(changes: SimpleChanges): void {
        if (changes["props"]?.currentValue?.options) {
            this.updateFieldOptions();
        }
    }

    private updateFieldOptions(): void {
        const opts = this.props?.options;
        this.fieldOptions = Array.isArray(opts) ? opts : [];
        this.defaultOption = this.fieldOptions.find(el => el.default) ?? this.field?.defaultValue ?? null;
    }
}
