import { Component, computed, input } from "@angular/core";
import { toObservable, toSignal } from "@angular/core/rxjs-interop";
import { FormGroup } from "@angular/forms";
import { IonicModule, ItemReorderCustomEvent } from "@ionic/angular";
import { of, startWith, switchMap } from "rxjs";
import { OeFormlyField } from "../../shared/oe-formly-component";

@Component({
    selector: "oe-formly-reorder-group",
    templateUrl: "./formly-reorder-group.html",
    standalone: true,
    imports: [IonicModule],
})
export class FormlyReorderGroupComponent {
    public readonly formGroup = input.required<FormGroup>();
    public readonly line = input.required<OeFormlyField.ReorderGroupLine>();

    protected readonly orderedItems = computed((): OeFormlyField.ReorderGroupLine["items"] => {
        const line = this.line();
        const values: unknown = this.controlValue() ?? null;
        if (!Array.isArray(values)) {
            return [];
        }

        const itemsByValue = new Map(line.items.map((item) => [item.value, item]));
        return values.map((value: string) => itemsByValue.get(value) ?? { value, label: value });
    });

    private readonly controlValue = toSignal(
        toObservable(computed(() => this.formGroup().controls[this.line().controlName])).pipe(
            switchMap((control) => control?.valueChanges.pipe(startWith(control.value)) ?? of(null)),
        ),
        { initialValue: null },
    );

    protected onReorder(event: ItemReorderCustomEvent): void {
        const control = this.formGroup().controls[this.line().controlName];
        if (control == null || !Array.isArray(control.value)) {
            event.detail.complete(false);
            return;
        }

        control.setValue(event.detail.complete(control.value));
        control.markAsDirty();
    }
}
