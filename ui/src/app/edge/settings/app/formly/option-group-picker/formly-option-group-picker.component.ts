import { Component, OnInit } from "@angular/core";
import { FieldType, FieldTypeConfig, FormlyFieldConfig } from "@ngx-formly/core";
import { OptionGroup, OptionGroupConfig, Option } from "./optionGroupPickerConfiguration";

@Component({
    selector: 'formly-option-group-picker',
    templateUrl: './formly-option-group-picker.component.html',
})
export class FormlyOptionGroupPickerComponent extends FieldType<FieldTypeConfig> implements OnInit {

    protected selectedGroup: OptionGroup | null = null;
    protected selectedIndex: number = 0;
    protected selectedValue: string;

    protected optionGroups: OptionGroup[] = [];

    public ngOnInit(): void {
        this.selectedValue = this.formControl.getRawValue();
        this.invalidateOptionGroups();

        this.optionGroups.forEach((group, i) => {
            if (!group.options.some(o => o.value === this.selectedValue)) {
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

    protected valueChange() {
        this.formControl.setValue(this.selectedValue);
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
                            title: optionConfig.expressions?.title?.(field) ?? optionConfig.title ?? optionConfig.value,
                            disabled: optionConfig.expressions?.disabled?.(field) ?? optionConfig.disabled ?? false,
                        };
                    }),
            };
        }).filter(group => {
            // Remove empty OptionGroups
            return group.options.length !== 0;
        });
    }

}
