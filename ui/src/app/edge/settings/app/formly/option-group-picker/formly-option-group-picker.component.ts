// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { OptionGroup, OptionGroupConfig, Option, getTitleFromOptionConfig } from "./optionGroupPickerConfiguration";

@Component({
    selector: 'formly-option-group-picker',
    templateUrl: './formly-option-group-picker.component.html',
})
export class FormlyOptionGroupPickerComponent extends FieldType<FieldTypeConfig> implements OnInit {

    protected multi: boolean = false;
    protected selectedGroup: OptionGroup | null = null;
    protected selectedIndex: number = 0;
    protected selectedValue: string | string[];

    protected optionGroups: OptionGroup[] = [];

    public ngOnInit(): void {
        this.multi = this.props.isMulti ?? false;

        // initialize the default value
        this.selectedValue = this.formControl.getRawValue();
        if (this.multi && !Array.isArray(this.selectedValue)) {
            this.selectedValue = [this.selectedValue as string];
        }
        this.invalidateOptionGroups();

        this.optionGroups.forEach((group, i) => {
            let anySelections = false;
            for (const option of group.options) {
                if (this.isMulti(this.selectedValue)) {
                    if (!this.selectedValue.some(v => v === option.value)) {
                        continue;
                    }
                    anySelections = true;
                    option.selected = true;
                } else {
                    if (option.value !== this.selectedValue) {
                        continue;
                    }
                    anySelections = true;
                    option.selected = true;
                }
            }

            if (!anySelections) {
                return;
            }

            // Set option as selected
            this.selectedGroup = group;
            this.selectedIndex = i;
        });

        // fallback default selected group
        if (!this.selectedGroup && this.optionGroups.length > 0) {
            this.selectedGroup = this.optionGroups[0];
            this.selectedIndex = 0;
        }
    }

    private isMulti(selectedValue: string | string[]): selectedValue is string[] {
        return this.multi;
    }

    protected valueChange() {
        this.formControl.setValue(this.selectedValue);
        this.form.markAsDirty();
    }

    protected valueChangeCheckbox(option: Option) {
        if (!this.isMulti(this.selectedValue)) {
            return;
        }
        option.selected = !option.selected;
        if (this.selectedValue.includes(option.value)) {
            this.selectedValue.splice(this.selectedValue.indexOf(option.value), 1);
        } else {
            this.selectedValue.push(option.value);
        }
        this.valueChange();
    }

    private invalidateOptionGroups() {
        this.optionGroups = FormlyOptionGroupPickerComponent.getOptionGroups(this.field, this.props.options as OptionGroupConfig[]);
    }

    private static getOptionGroups(field: FormlyFieldConfig, optionGroupConfigs: OptionGroupConfig[]): OptionGroup[] {
        return optionGroupConfigs.map<OptionGroup>(groupConfig => {
            return {
                group: groupConfig.group,
                title: groupConfig.title,
                options: groupConfig.options
                    .filter(optionConfig => {
                        // Remove hidden Options
                        return !(optionConfig.expressions?.hide?.(field) ?? optionConfig.hide ?? false);
                    })
                    .map<Option>(optionConfig => {
                        return {
                            value: optionConfig.value,
                            title: getTitleFromOptionConfig(optionConfig, field),
                            disabled: optionConfig.expressions?.disabled?.(field) ?? optionConfig.disabled ?? false,
                            selected: false,
                        };
                    }),
            };
        }).filter(group => {
            // Remove empty OptionGroups
            return group.options.length !== 0;
        });
    }

}
