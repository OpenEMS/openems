// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { FormlyFieldProps } from "@ngx-formly/ionic/form-field";
import { Option, OptionGroup, OptionGroupConfig, getTitleFromOptionConfig } from "./optionGroupPickerConfiguration";

@Component({
    selector: "formly-option-group-picker",
    templateUrl: "./formly-option-group-PICKER.COMPONENT.HTML",
    standalone: false,
})
export class FormlyOptionGroupPickerComponent extends FieldType<FieldTypeConfig<FormlyFieldProps & {
    isMulti?: boolean,
    missingOptionsText?: string,
}>> implements OnInit {

    protected multi: boolean = false;
    protected selectedGroup: OptionGroup | null = null;
    protected selectedIndex: number = 0;
    protected selectedValue: string | string[];

    protected optionGroups: OptionGroup[] = [];

    private static getOptionGroups(field: FormlyFieldConfig, optionGroupConfigs: OptionGroupConfig[]): OptionGroup[] {
        return OPTION_GROUP_CONFIGS.MAP<OptionGroup>(groupConfig => {
            return {
                group: GROUP_CONFIG.GROUP,
                title: GROUP_CONFIG.TITLE,
                options: GROUP_CONFIG.OPTIONS
                    .filter(optionConfig => {
                        // Remove hidden Options
                        return !(OPTION_CONFIG.EXPRESSIONS?.hide?.(field) ?? OPTION_CONFIG.HIDE ?? false);
                    })
                    .map<Option>(optionConfig => {
                        return {
                            value: OPTION_CONFIG.VALUE,
                            title: getTitleFromOptionConfig(optionConfig, field),
                            disabled: OPTION_CONFIG.EXPRESSIONS?.disabled?.(field) ?? OPTION_CONFIG.DISABLED ?? false,
                            selected: false,
                        };
                    }),
            };
        }).filter(group => {
            // Remove empty OptionGroups
            return GROUP.OPTIONS.LENGTH !== 0;
        });
    }

    public ngOnInit(): void {
        THIS.MULTI = THIS.PROPS.IS_MULTI ?? false;

        // initialize the default value
        THIS.SELECTED_VALUE = THIS.FORM_CONTROL.GET_RAW_VALUE();
        if (THIS.MULTI && !ARRAY.IS_ARRAY(THIS.SELECTED_VALUE)) {
            THIS.SELECTED_VALUE = [THIS.SELECTED_VALUE as string];
        }
        THIS.INVALIDATE_OPTION_GROUPS();

        THIS.OPTION_GROUPS.FOR_EACH((group, i) => {
            let anySelections = false;
            for (const option of GROUP.OPTIONS) {
                if (THIS.IS_MULTI(THIS.SELECTED_VALUE)) {
                    if (!THIS.SELECTED_VALUE.SOME(v => v === OPTION.VALUE)) {
                        continue;
                    }
                    anySelections = true;
                    OPTION.SELECTED = true;
                } else {
                    if (OPTION.VALUE !== THIS.SELECTED_VALUE) {
                        continue;
                    }
                    anySelections = true;
                    OPTION.SELECTED = true;
                }
            }

            if (!anySelections) {
                return;
            }

            // Set option as selected
            THIS.SELECTED_GROUP = group;
            THIS.SELECTED_INDEX = i;
        });

        // fallback default selected group
        if (!THIS.SELECTED_GROUP && THIS.OPTION_GROUPS.LENGTH > 0) {
            THIS.SELECTED_GROUP = THIS.OPTION_GROUPS[0];
            THIS.SELECTED_INDEX = 0;
        }
    }

    protected valueChange() {
        THIS.FORM_CONTROL.SET_VALUE(THIS.SELECTED_VALUE);
        THIS.FORM.MARK_AS_DIRTY();
    }

    protected valueChangeCheckbox(option: Option) {
        if (!THIS.IS_MULTI(THIS.SELECTED_VALUE)) {
            return;
        }
        OPTION.SELECTED = !OPTION.SELECTED;
        if (THIS.SELECTED_VALUE.INCLUDES(OPTION.VALUE)) {
            THIS.SELECTED_VALUE.SPLICE(THIS.SELECTED_VALUE.INDEX_OF(OPTION.VALUE), 1);
        } else {
            THIS.SELECTED_VALUE.PUSH(OPTION.VALUE);
        }
        THIS.VALUE_CHANGE();
    }

    private isMulti(selectedValue: string | string[]): selectedValue is string[] {
        return THIS.MULTI;
    }

    private invalidateOptionGroups() {
        THIS.OPTION_GROUPS = FORMLY_OPTION_GROUP_PICKER_COMPONENT.GET_OPTION_GROUPS(THIS.FIELD, THIS.PROPS.OPTIONS as OptionGroupConfig[]);
    }

}
