// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { AlertController } from "@ionic/angular";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerComponent } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { Edge, Service, Utils, Websocket } from "../../../shared/shared";
import { InstallAppComponent } from "./install.component";
import { DeleteAppInstance } from "./jsonrpc/deleteAppInstance";
import { GetAppAssistant } from "./jsonrpc/getAppAssistant";
import { GetAppInstances } from "./jsonrpc/getAppInstances";
import { UpdateAppInstance } from "./jsonrpc/updateAppInstance";
import { ConfigurationOAuthComponent } from "./steps/oauth/configuration-oauth.component";

interface MyInstance {
    instanceId: string, // uuid
    form: FormGroup,
    isDeleting: boolean,
    isUpdating: boolean,
    fields: FormlyFieldConfig[]
    properties: {},
    steps: GetAppAssistant.AppConfigurationStep[],
}

@Component({
    selector: UpdateAppComponent.SELECTOR,
    templateUrl: "./update.component.html",
    standalone: true,
    imports: [
        CommonUiModule,
        PipeComponentsModule,
        NgxSpinnerComponent,
        ReactiveFormsModule,
        RouterModule,
        FormlyModule,
        ConfigurationOAuthComponent,
    ],
})
export class UpdateAppComponent implements OnInit {

    private static readonly SELECTOR = "app-update";
    public readonly spinnerId: string = UpdateAppComponent.SELECTOR;

    protected instances: MyInstance[] = [];
    protected appName: string | null = null;

    private edge: Edge | null = null;

    public constructor(
        private route: ActivatedRoute,
        protected utils: Utils,
        private websocket: Websocket,
        private service: Service,
        private router: Router,
        private translate: TranslateService,
        private alertCtrl: AlertController,
    ) {
    }

    public ngOnInit() {
        this.service.startSpinner(this.spinnerId);
        const appId = this.route.snapshot.params["appId"];
        const appName = this.route.snapshot.queryParams["name"] ?? this.service.currentPageTitle;
        this.service.setCurrentComponent(appName ?? "", this.route).then(edge => {
            this.edge = edge;
            edge.sendRequest(this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new GetAppInstances.Request({ appId: appId }),
                })).then(getInstancesResponse => {
                    const recInstances = (getInstancesResponse as GetAppInstances.Response).result.instances;

                    edge.sendRequest(this.websocket,
                        new ComponentJsonApiRequest({
                            componentId: "_appManager",
                            payload: new GetAppAssistant.Request({ appId: appId }),
                        })).then(getAppAssistantResponse => {
                            const appAssistant = (getAppAssistantResponse as GetAppAssistant.Response).result;
                            this.appName = appAssistant.name;
                            this.instances = [];
                            for (const instance of recInstances) {
                                const form = new FormGroup({});
                                const model = {
                                    "ALIAS": instance.alias,
                                    ...instance.properties,
                                };

                                // tread configuration as a installation step
                                const steps = [
                                    {
                                        type: GetAppAssistant.AppConfigurationStepType.CONFIGURATION,
                                        params: {},
                                    },
                                    ...(appAssistant.steps ?? []),
                                ];

                                this.instances.push({
                                    instanceId: instance.instanceId,
                                    form: form,
                                    isDeleting: false,
                                    isUpdating: false,
                                    fields: GetAppAssistant.setInitialModel(GetAppAssistant.postprocess(structuredClone(appAssistant)).fields, structuredClone(model)),
                                    properties: model,
                                    steps: steps,
                                });
                            }

                            this.service.stopSpinner(this.spinnerId);
                        }).catch(InstallAppComponent.errorToast(this.service, error => "Error while receiving App Assistant for [" + appId + "]: " + error));
                }).catch(InstallAppComponent.errorToast(this.service, error => "Error while receiving App-Instances for [" + appId + "]: " + error));
        });
    }

    protected submit(instance: MyInstance) {
        this.service.startSpinnerTransparentBackground(instance.instanceId);
        instance.isUpdating = true;
        // remove alias field from properties
        const alias = instance.form.value["ALIAS"];
        const clonedFields = {};
        for (const item in instance.form.value) {
            if (item != "ALIAS") {
                clonedFields[item] = instance.form.value[item];
            }
        }
        instance.form.markAsPristine();
        this.edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
                componentId: "_appManager",
                payload: new UpdateAppInstance.Request({
                    instanceId: instance.instanceId,
                    alias: alias,
                    properties: clonedFields,
                }),
            })).then(response => {
                const result = (response as UpdateAppInstance.Response).result;

                if (result.warnings && result.warnings.length > 0) {
                    this.service.toast(result.warnings.join(";"), "warning");
                } else {
                    this.service.toast(this.translate.instant("EDGE.CONFIG.APP.SUCCESS_UPDATE"), "success");
                }
                instance.properties = result.instance.properties;
                instance.properties["ALIAS"] = result.instance.alias;
            })
            .catch(InstallAppComponent.errorToast(this.service, error => this.translate.instant("EDGE.CONFIG.APP.FAIL_UPDATE", { error: error })))
            .finally(() => {
                instance.isUpdating = false;
                this.service.stopSpinner(instance.instanceId);
            });
    }

    protected async submitDelete(instance: MyInstance) {
        const translate = this.translate;

        const alert = this.alertCtrl.create({
            subHeader: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM_HEADLINE"),
            message: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM_DESCRIPTION"),
            buttons: [{
                text: translate.instant("GENERAL.CANCEL"),
                role: "cancel",
            },
            {
                text: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM"),
                handler: () => this.delete(instance),
            }],
            cssClass: "alertController",
        });
        (await alert).present();
    }

    protected delete(instance: MyInstance) {
        this.service.startSpinnerTransparentBackground(instance.instanceId);
        instance.isDeleting = true;
        this.edge.sendRequest(this.websocket,
            new ComponentJsonApiRequest({
                componentId: "_appManager",
                payload: new DeleteAppInstance.Request({
                    instanceId: instance.instanceId,
                }),
            })).then(response => {
                this.instances.splice(this.instances.indexOf(instance), 1);
                this.service.toast(this.translate.instant("EDGE.CONFIG.APP.SUCCESS_DELETE"), "success");
                const navigationExtras = { state: { appInstanceChange: true } };
                this.router.navigate(["device/" + (this.edge.id) + "/settings/app/"], navigationExtras);
            })
            .catch(InstallAppComponent.errorToast(this.service, error => this.translate.instant("EDGE.CONFIG.APP.FAIL_DELETE", { error: error })))
            .finally(() => {
                instance.isDeleting = false;
                this.service.stopSpinner(instance.instanceId);
            });
    }
}
