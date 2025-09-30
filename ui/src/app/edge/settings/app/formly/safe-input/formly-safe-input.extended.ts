// @ts-strict-ignore
import { ChangeDetectorRef, Component, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { FieldWrapper, FormlyFieldConfig } from "@ngx-formly/core";
import { GetAppAssistant } from "../../jsonrpc/getAppAssistant";
import { OptionGroupConfig, getTitleFromOptionConfig } from "../option-group-picker/optionGroupPickerConfiguration";
import { FormlySafeInputModalComponent } from "./formly-safe-input-MODAL.COMPONENT";

@Component({
    selector: "formly-safe-input-wrapper",
    templateUrl: "./formly-safe-INPUT.EXTENDED.HTML",
    standalone: false,
})
export class FormlySafeInputWrapperComponent extends FieldWrapper implements OnInit {

    protected pathToDisplayValue: string;
    protected displayType: "string" | "boolean" | "number" | "optionGroup";

    constructor(
        private modalController: ModalController,
        private changeDetectorRef: ChangeDetectorRef,
    ) {
        super();
    }

    ngOnInit(): void {
        THIS.PATH_TO_DISPLAY_VALUE = THIS.PROPS["pathToDisplayValue"];
        THIS.DISPLAY_TYPE = THIS.PROPS["displayType"] ?? "string";
    }

    public getValue() {
        if (THIS.DISPLAY_TYPE === "boolean"
            || THIS.DISPLAY_TYPE === "number"
            || THIS.DISPLAY_TYPE === "string") {
            return THIS.MODEL[THIS.PATH_TO_DISPLAY_VALUE];
        }

        if (THIS.DISPLAY_TYPE === "optionGroup") {
            const value = THIS.GET_VALUE_OF_OPTION_GROUP();
            if (value) {
                return value;
            }
        }

        // not defined
        return THIS.MODEL[THIS.PATH_TO_DISPLAY_VALUE];
    }

    protected onSelectItem() {
        THIS.FORM_CONTROL.MARK_AS_TOUCHED();
        THIS.OPEN_MODAL();
    }

    /**
     * Opens the model to select the option.
     */
    private async openModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: FormlySafeInputModalComponent,
            componentProps: {
                title: THIS.PROPS.LABEL,
                fields: THIS.GET_FIELDS(),
                model: structuredClone(THIS.MODEL),
            },
            cssClass: ["auto-height"],
        });
        MODAL.ON_DID_DISMISS().then(event => {
            if (!EVENT.DATA) {
                // nothing selected
                return;
            }

            const finalModel = { ...THIS.FORM.GET_RAW_VALUE(), ...EVENT.DATA };

            const changedValues = {};
            for (const [key, value] of OBJECT.ENTRIES(finalModel)) {
                if (value === THIS.MODEL[key]) {
                    continue;
                }
                changedValues[key] = value;
            }

            for (const [key, value] of OBJECT.ENTRIES(changedValues)) {
                THIS.MODEL[key] = value;
            }

            // set values with current form value when the fields are set via fieldGroup
            // to make sure every value gets set accordingly to the object hierarchy
            if (THIS.FIELD.FIELD_GROUP) {
                THIS.FORM.SET_VALUE(THIS.FORM.GET_RAW_VALUE());
            } else {
                // only update values which got changed
                for (const [key, value] of OBJECT.ENTRIES(changedValues)) {
                    const control = THIS.FORM.CONTROLS[key];
                    if (!control) {
                        continue;
                    }
                    CONTROL.SET_VALUE(value);
                }
            }
            THIS.FORM_CONTROL.MARK_AS_DIRTY();
            THIS.CHANGE_DETECTOR_REF.DETECT_CHANGES();
        });
        return await MODAL.PRESENT();
    }

    private getValueOfOptionGroup(): string {
        const field = GET_APP_ASSISTANT.FIND_FIELD(THIS.GET_FIELDS(), THIS.PATH_TO_DISPLAY_VALUE.SPLIT("."));
        if (!field) {
            return null;
        }
        const value = THIS.MODEL[THIS.PATH_TO_DISPLAY_VALUE];
        const options = ((FIELD.TEMPLATE_OPTIONS ?? FIELD.PROPS).options as OptionGroupConfig[]).map(optionGroup => OPTION_GROUP.OPTIONS)
            .reduce((acc, val) => ACC.CONCAT(val), []);
        if (ARRAY.IS_ARRAY(value)) {
            return (value as []).map(e => OPTIONS.FIND(option => OPTION.VALUE === e))
                .map(option => getTitleFromOptionConfig(option, THIS.FIELD))
                .join(", ");
        } else {
            const option = OPTIONS.FIND(option => OPTION.VALUE === value);
            if (!option) {
                return null;
            }
            return getTitleFromOptionConfig(option, THIS.FIELD);
        }
    }


    private getFields(): FormlyFieldConfig[] {
        // @Deprecated rather set this#PROPS.FIELDS
        if (THIS.FIELD.FIELD_GROUP) {
            return THIS.FIELD.FIELD_GROUP;
        }
        if (THIS.PROPS.FIELDS) {
            return THIS.PROPS.FIELDS;
        }
        return [];
    }

}
