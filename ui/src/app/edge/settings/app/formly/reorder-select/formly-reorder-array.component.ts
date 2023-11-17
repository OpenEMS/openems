import { Component, OnInit } from "@angular/core";
import { ItemReorderEventDetail } from "@ionic/angular";
import { FieldType, FieldTypeConfig, FormlyFieldConfig, FormlyFieldProps } from "@ngx-formly/core";

@Component({
    selector: 'reorder-array',
    templateUrl: './formly-reorder-array.component.html'
})
export class FormlyReorderArrayComponent extends FieldType<FieldTypeConfig<FormlyFieldProps & {
    allowDuplicates?: boolean,
    selectOptions?: SelectOptionConfig[]
}>> implements OnInit {

    protected selectedItems: SelectOption[] = [];
    protected availableItems: SelectOption[];

    protected itemToAdd: SelectOption | null = null;

    public ngOnInit(): void {
        const oldValues = this.formControl.getRawValue() as string[];

        this.availableItems = this.selectOptions;
        if (oldValues) {
            for (const v of oldValues) {
                const foundItemIndex = this.availableItems.findIndex(e => e.value === v);
                if (foundItemIndex === -1) {
                    // item not found
                    continue;
                }

                this.selectedItems.push(this.availableItems[foundItemIndex]);
                if (!this.allowDuplicates) {
                    this.availableItems.splice(foundItemIndex, 1);
                }
            }
        }
        // select first element if existing
        if (this.availableItems.length !== 0) {
            this.itemToAdd = this.availableItems[0];
        }
        this.updateValue();
    }

    protected doReorder(ev: CustomEvent<ItemReorderEventDetail>) {
        if (this.selectedItems[ev.detail.to].expressions.locked) {
            ev.detail.complete(false);
            return;
        }
        this.selectedItems = ev.detail.complete(this.selectedItems);
        this.updateValue();
    }

    protected removeItem(item: SelectOption) {
        const deletedItems = this.selectedItems.splice(this.selectedItems.indexOf(item), 1);
        this.updateValue();
        if (this.allowDuplicates) {
            return;
        }
        this.availableItems.push(...deletedItems);
        if (!this.itemToAdd) {
            this.itemToAdd = deletedItems[0];
        }
    }

    protected addItem() {
        if (!this.itemToAdd) {
            return;
        }
        this.selectedItems.push(this.itemToAdd);
        this.updateValue();
        if (this.allowDuplicates) {
            return;
        }
        this.availableItems.splice(this.availableItems.indexOf(this.itemToAdd), 1);
        this.itemToAdd = this.availableItems.length !== 0 ? this.availableItems[0] : null;
    }

    private updateValue() {
        this.formControl.setValue(this.selectedItems.map(i => i.value));
        this.invalidateSelectOptions();
    }

    private invalidateSelectOptions() {
        const newOptions = this.selectOptions;
        this.selectedItems.forEach(option => {
            const validatedOption = newOptions.find(o => o.value === option.value);
            if (!validatedOption) {
                return;
            }
            option.expressions.locked = validatedOption.expressions.locked;
        });
    }

    private get allowDuplicates(): boolean {
        return this.props.allowDuplicates ?? false;
    }

    private get selectOptions(): SelectOption[] {
        return this.props.selectOptions.map<SelectOption>(optionConfig => {
            return {
                label: optionConfig.label,
                value: optionConfig.value,
                expressions: {
                    locked: optionConfig.expressions?.locked?.(this.field) ?? false
                }
            };
        }) ?? [];
    }

}

export type SelectOptionConfig = {
    label: string,
    value: string,
    expressions?: {
        locked?: (field: FormlyFieldConfig) => boolean,
    }
}

type SelectOption = {
    label: string,
    value: string,
    expressions: {
        locked: boolean,
    }
}
