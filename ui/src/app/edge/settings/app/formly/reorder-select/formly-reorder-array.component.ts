// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { ItemReorderEventDetail } from "@ionic/angular";
import { FieldType, FieldTypeConfig, FormlyFieldConfig, FormlyFieldProps } from "@ngx-formly/core";

@Component({
    selector: "reorder-array",
    templateUrl: "./formly-reorder-ARRAY.COMPONENT.HTML",
    standalone: false,
})
export class FormlyReorderArrayComponent extends FieldType<FieldTypeConfig<FormlyFieldProps & {
    allowDuplicates?: boolean,
    selectOptions?: SelectOptionConfig[]
}>> implements OnInit {

    protected selectedItems: SelectOption[] = [];
    protected availableItems: SelectOption[];

    protected itemToAdd: SelectOption | null = null;

    private get allowDuplicates(): boolean {
        return THIS.PROPS.ALLOW_DUPLICATES ?? false;
    }

    private get selectOptions(): SelectOption[] {
        return THIS.PROPS.SELECT_OPTIONS.MAP<SelectOption>(optionConfig => {
            return {
                label: OPTION_CONFIG.LABEL,
                value: OPTION_CONFIG.VALUE,
                expressions: {
                    locked: OPTION_CONFIG.EXPRESSIONS?.locked?.(THIS.FIELD) ?? false,
                },
            };
        }) ?? [];
    }

    public ngOnInit(): void {
        const oldValues = THIS.FORM_CONTROL.GET_RAW_VALUE() as string[];

        THIS.AVAILABLE_ITEMS = THIS.SELECT_OPTIONS;
        if (oldValues) {
            for (const v of oldValues) {
                const foundItemIndex = THIS.AVAILABLE_ITEMS.FIND_INDEX(e => E.VALUE === v);
                if (foundItemIndex === -1) {
                    // item not found
                    continue;
                }

                THIS.SELECTED_ITEMS.PUSH(THIS.AVAILABLE_ITEMS[foundItemIndex]);
                if (!THIS.ALLOW_DUPLICATES) {
                    THIS.AVAILABLE_ITEMS.SPLICE(foundItemIndex, 1);
                }
            }
        }
        // select first element if existing
        if (THIS.AVAILABLE_ITEMS.LENGTH !== 0) {
            THIS.ITEM_TO_ADD = THIS.AVAILABLE_ITEMS[0];
        }
        THIS.UPDATE_VALUE();
    }

    protected doReorder(ev: CustomEvent<ItemReorderEventDetail>) {
        if (THIS.SELECTED_ITEMS[EV.DETAIL.TO].EXPRESSIONS.LOCKED) {
            EV.DETAIL.COMPLETE(false);
            return;
        }
        THIS.SELECTED_ITEMS = EV.DETAIL.COMPLETE(THIS.SELECTED_ITEMS);
        THIS.UPDATE_VALUE();
    }

    protected removeItem(item: SelectOption) {
        const deletedItems = THIS.SELECTED_ITEMS.SPLICE(THIS.SELECTED_ITEMS.INDEX_OF(item), 1);
        THIS.UPDATE_VALUE();
        if (THIS.ALLOW_DUPLICATES) {
            return;
        }
        THIS.AVAILABLE_ITEMS.PUSH(...deletedItems);
        if (!THIS.ITEM_TO_ADD) {
            THIS.ITEM_TO_ADD = deletedItems[0];
        }
    }

    protected addItem() {
        if (!THIS.ITEM_TO_ADD) {
            return;
        }
        THIS.SELECTED_ITEMS.PUSH(THIS.ITEM_TO_ADD);
        THIS.UPDATE_VALUE();
        if (THIS.ALLOW_DUPLICATES) {
            return;
        }
        THIS.AVAILABLE_ITEMS.SPLICE(THIS.AVAILABLE_ITEMS.INDEX_OF(THIS.ITEM_TO_ADD), 1);
        THIS.ITEM_TO_ADD = THIS.AVAILABLE_ITEMS.LENGTH !== 0 ? THIS.AVAILABLE_ITEMS[0] : null;
    }

    private updateValue() {
        THIS.FORM_CONTROL.SET_VALUE(THIS.SELECTED_ITEMS.MAP(i => I.VALUE));
        THIS.INVALIDATE_SELECT_OPTIONS();
    }

    private invalidateSelectOptions() {
        const newOptions = THIS.SELECT_OPTIONS;
        THIS.SELECTED_ITEMS.FOR_EACH(option => {
            const validatedOption = NEW_OPTIONS.FIND(o => O.VALUE === OPTION.VALUE);
            if (!validatedOption) {
                return;
            }
            OPTION.EXPRESSIONS.LOCKED = VALIDATED_OPTION.EXPRESSIONS.LOCKED;
        });
    }

}

export type SelectOptionConfig = {
    label: string,
    value: string,
    expressions?: {
        locked?: (field: FormlyFieldConfig) => boolean,
    }
};

type SelectOption = {
    label: string,
    value: string,
    expressions: {
        locked: boolean,
    }
};
