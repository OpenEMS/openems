import { Validators } from "@angular/forms";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { AlertingSettingResponse } from "src/app/shared/jsonrpc/response/getUserAlertingConfigsResponse";
import { Role } from "src/app/shared/type/role";
import { Icon } from "src/app/shared/type/widget";
import { AlertingType, DefaultValues, Delay } from "../ALERTING.COMPONENT";

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
                title: TRANSLATE.INSTANT("ALERTING.ONLINE_STATUS"),
            },
            fieldGroup: [
                {
                    key: "offline-toggle",
                    type: "toggle",
                },
                {
                    key: "offline-delay-selection",
                    type: "radio-buttons",
                    name: TRANSLATE.INSTANT("ALERTING.DELAY"),
                    props: {
                        options: defaultValues[ALERTING_TYPE.OFFLINE],
                        disabledOnFormControl: "offline-toggle",
                    },
                    validators: {
                        validation: [VALIDATORS.REQUIRED],
                    },
                },
                {
                    key: "offline-checkbox",
                    type: "checkbox",
                    name: "E-Mail",
                    props: {
                        help: TRANSLATE.INSTANT("ALERTING.CHECKBOX_HELP"),
                        disabledOnFormControl: "offline-toggle",
                    },
                    validators: [VALIDATORS.REQUIRED],
                },
            ],
        },

        ...(ROLE.IS_AT_LEAST(edgeRole, ROLE.INSTALLER) ?
            [{
                key: "fault",
                props: {
                    icon: {
                        color: "danger",
                        size: "large",
                        name: "alert-circle-outline",
                        position: "end",
                    },
                    title: TRANSLATE.INSTANT("ALERTING.FAULT"),
                },
                fieldGroup: [
                    {
                        key: "fault-toggle",
                        type: "toggle",
                    },
                    {
                        key: "fault-delay-selection",
                        type: "radio-buttons",
                        name: TRANSLATE.INSTANT("EDGE.CONFIG.ALERTING.DELAY"),

                        props: {
                            options: defaultValues[ALERTING_TYPE.OFFLINE],
                            disabledOnFormControl: "fault-toggle",
                        },
                        validators: [VALIDATORS.REQUIRED],
                    },
                    {
                        key: "fault-checkbox",
                        type: "checkbox",
                        name: "E-Mail",
                        props: {
                            help: TRANSLATE.INSTANT("ALERTING.CHECKBOX_HELP"),
                            required: true,
                            disabledOnFormControl: "fault-toggle",
                        },
                        validators: {
                            validation: [VALIDATORS.REQUIRED],
                        },
                    },
                ],
            }] : [])];
};


export const otherUserRows = (otherUsers: AlertingSettingResponse[], defaultValues: DefaultValues, translate: TranslateService): FormlyFieldConfig[] => {

    function buildFormlyFieldConfig(el: AlertingSettingResponse, translate: TranslateService): FormlyFieldConfig {
        return {
            key: EL.USER_LOGIN,
            fieldGroup: [
                ...otherUserRow("offline", TRANSLATE.INSTANT("ALERTING.ONLINE_STATUS"), defaultValues[ALERTING_TYPE.OFFLINE], { color: "danger", name: "cloud-offline-outline" }, translate),
                ...otherUserRow("fault", TRANSLATE.INSTANT("ALERTING.FAULT"), defaultValues[ALERTING_TYPE.FAULT], { color: "danger", name: "alert-circle-outline" }, translate),
            ],
        };
    };

    return OTHER_USERS.MAP(el => buildFormlyFieldConfig(el, translate));
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
                    name: TRANSLATE.INSTANT("EDGE.CONFIG.ALERTING.DELAY"),
                    props: {
                        options: delay,
                        disabledOnFormControl: key + "-toggle",
                    },
                    expressions: {
                        "PROPS.DISABLED": field => !FIELD.MODEL[key + "-toggle"],
                    },
                    validators: [VALIDATORS.REQUIRED],
                },
                {
                    key: key + "-checkbox",
                    type: "checkbox",
                    name: "E-Mail",
                    props: {
                        help: TRANSLATE.INSTANT("ALERTING.CHECKBOX_HELP"),
                        disabledOnFormControl: key + "-toggle",
                    },
                    validators: [VALIDATORS.REQUIRED_TRUE],
                },
            ],
        },
    ];
};
