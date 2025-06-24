import { Validators } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { AlertingSettingResponse } from "src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse";
import { Role } from "src/app/shared/type/role";
import { Icon } from "src/app/shared/type/widget";
import { AlertingType, DefaultValues, Delay } from "../alerting.component";

export const currentUserRows = (defaultValues: DefaultValues, translate: TranslateService, edgeRole: Role): FormlyFieldConfig[] => {
    return [
        {
            key: "offline",
            props: {
                icon: {
                    color: "danger",
                    size: "large",
                    name: "cloud-offline-outline",
                    position: "end",
                },
                title: translate.instant("ALERTING.ONLINE_STATUS"),
            },
            fieldGroup: [
                {
                    key: "offline-toggle",
                    type: "toggle",
                },
                {
                    key: "offline-delay-selection",
                    type: "radio-buttons",
                    name: translate.instant("ALERTING.DELAY"),
                    props: {
                        options: defaultValues[AlertingType.OFFLINE],
                        disabledOnFormControl: "offline-toggle",
                    },
                    validators: {
                        validation: [Validators.required],
                    },
                },
                {
                    key: "offline-checkbox",
                    type: "checkbox",
                    name: "E-Mail",
                    props: {
                        help: translate.instant("ALERTING.CHECKBOX_HELP"),
                        disabledOnFormControl: "offline-toggle",
                    },
                    validators: [Validators.required],
                },
            ],
        },

        ...(Role.isAtLeast(edgeRole, Role.INSTALLER) ?
            [{
                key: "fault",
                props: {
                    icon: {
                        color: "danger",
                        size: "large",
                        name: "alert-circle-outline",
                        position: "end",
                    },
                    title: translate.instant("ALERTING.FAULT"),
                },
                fieldGroup: [
                    {
                        key: "fault-toggle",
                        type: "toggle",
                    },
                    {
                        key: "fault-delay-selection",
                        type: "radio-buttons",
                        name: translate.instant("Edge.Config.ALERTING.DELAY"),

                        props: {
                            options: defaultValues[AlertingType.OFFLINE],
                            disabledOnFormControl: "fault-toggle",
                        },
                        validators: [Validators.required],
                    },
                    {
                        key: "fault-checkbox",
                        type: "checkbox",
                        name: "E-Mail",
                        props: {
                            help: translate.instant("ALERTING.CHECKBOX_HELP"),
                            required: true,
                            disabledOnFormControl: "fault-toggle",
                        },
                        validators: {
                            validation: [Validators.required],
                        },
                    },
                ],
            }] : [])];
};


export const otherUserRows = (otherUsers: AlertingSettingResponse[], defaultValues: DefaultValues, translate: TranslateService): FormlyFieldConfig[] => {

    function buildFormlyFieldConfig(el: AlertingSettingResponse, translate: TranslateService): FormlyFieldConfig {
        return {
            key: el.userLogin,
            fieldGroup: [
                ...otherUserRow("offline", translate.instant("ALERTING.ONLINE_STATUS"), defaultValues[AlertingType.OFFLINE], { color: "danger", name: "cloud-offline-outline" }, translate),
                ...otherUserRow("fault", translate.instant("ALERTING.FAULT"), defaultValues[AlertingType.FAULT], { color: "danger", name: "alert-circle-outline" }, translate),
            ],
        };
    };

    return otherUsers.map(el => buildFormlyFieldConfig(el, translate));
};


export const otherUserRow = (key: "fault" | "offline", name: string, delay: Delay[], icon: Partial<Icon>, translate: TranslateService): FormlyFieldConfig[] => {
    return [
        {
            key: key + "-toggle",
            type: "toggle",
            name: name,
            props: {
                icon: {
                    color: "danger",
                    size: "large",
                    name: "help-circle-outline",
                    position: "end",
                    ...icon,
                },
            },
            fieldGroup: [
                {
                    key: key + "-delay-selection",
                    type: "radio-buttons",
                    name: translate.instant("Edge.Config.ALERTING.DELAY"),
                    props: {
                        options: delay,
                        disabledOnFormControl: key + "-toggle",
                    },
                    expressions: {
                        "props.disabled": field => !field.model[key + "-toggle"],
                    },
                    validators: [Validators.required],
                },
                {
                    key: key + "-checkbox",
                    type: "checkbox",
                    name: "E-Mail",
                    props: {
                        help: translate.instant("ALERTING.CHECKBOX_HELP"),
                        disabledOnFormControl: key + "-toggle",
                    },
                    validators: [Validators.requiredTrue],
                },
            ],
        },
    ];
};
