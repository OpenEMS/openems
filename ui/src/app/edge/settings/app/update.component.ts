import { ChangeDetectionStrategy, ChangeDetectorRef, Component, effect, inject, OnInit, signal } from "@angular/core";
import { FormGroup, ReactiveFormsModule } from "@angular/forms";
import { ActivatedRoute, Router, RouterModule } from "@angular/router";
import { AlertController, ToggleCustomEvent } from "@ionic/angular";
import { FormlyFieldConfig, FormlyModule } from "@ngx-formly/core";
import { TranslateService } from "@ngx-translate/core";
import { NgxSpinnerComponent } from "ngx-spinner";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { NavigationBackButtonComponent } from "src/app/shared/components/navigation/back-button/navigation-back-button";
import { JsonRpcUtils } from "src/app/shared/jsonrpc/jsonrpcutils";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { RouteService } from "src/app/shared/service/route/route.service";
import { Edge, EdgePermission, Service, Utils, Websocket } from "../../../shared/shared";
import { extractErrorMessage } from "../../../shared/utils/error/error.utils";
import { InstallAppComponent } from "./install.component";
import { CanSwitchArchitecture } from "./jsonrpc/canSwitchArchitecture";
import { DeleteAppInstance } from "./jsonrpc/deleteAppInstance";
import { Flag } from "./jsonrpc/flag/flag";
import { GetApp } from "./jsonrpc/getApp";
import { GetAppAssistant } from "./jsonrpc/getAppAssistant";
import { GetAppInstances } from "./jsonrpc/getAppInstances";
import { QueryAppInstancesByFilter } from "./jsonrpc/queryAppInstancesByFilter";
import { prepareProperties } from "./jsonrpc/shared";
import { SwitchArchitecture } from "./jsonrpc/switchArchitecture";
import { UpdateAppInstance } from "./jsonrpc/updateAppInstance";
import { ConfigurationOAuthComponent } from "./steps/oauth/configuration-oauth.component";

interface MyInstance {
    instanceId: string; // uuid
    form: FormGroup;
    isDeleting: boolean;
    isUpdating: boolean;
    fields: FormlyFieldConfig[];
    properties: Record<string, any>;
    steps: GetAppAssistant.AppConfigurationStep[];
}

@Component({
    selector: UpdateAppComponent.SELECTOR,
    templateUrl: "./update.component.html",
    standalone: true,
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [
        CommonUiModule,
        PipeComponentsModule,
        NgxSpinnerComponent,
        ReactiveFormsModule,
        RouterModule,
        FormlyModule,
        ConfigurationOAuthComponent,
        NavigationBackButtonComponent,
    ],
})
export class UpdateAppComponent implements OnInit {
    private static readonly SELECTOR = "app-update";
    public readonly spinnerId: string = UpdateAppComponent.SELECTOR;

    protected showSwitchArchitecture = signal<boolean>(false);
    protected currentArchitecture = signal<string | null>(null);
    protected instances = signal<MyInstance[]>([]);
    protected appName: string | null = null;
    protected isAppCenter: boolean = false;

    protected header: string | null = null;
    protected info: string | null = null;
    protected link: string | null = null;
    private edge: Edge | null = null;
    private switchMethod: string | null = null;
    private handlerId: string | null = null;
    private translateService = inject(TranslateService);
    private isRunning: boolean = false;
    private switchArchitectureRequested = signal<boolean>(false);

    private readonly cdRef = inject(ChangeDetectorRef);

    public constructor(
        private route: ActivatedRoute,
        private routeService: RouteService,
        protected utils: Utils,
        private websocket: Websocket,
        private service: Service,
        private router: Router,
        private translate: TranslateService,
        private alertCtrl: AlertController,
    ) {
        const queryParamsSignal = this.routeService.getQueryParams();
        effect(() => {
            const _queryParams = queryParamsSignal();
            if (this.isRunning) {
                return;
            }
            this.ngOnInit();
        });
    }

    public ngOnInit() {
        this.service.startSpinnerTransparentBackground(this.spinnerId);
        const appId = this.routeService.getQueryParam<string>("appId");
        if (appId == null) {
            this.service.stopSpinner(this.spinnerId);
            this.service.toast("Missing app id in route.", "danger");
            return;
        }
        this.isRunning = true;
        const componentId = this.routeService.getQueryParam<string>("componentId");

        const queryName = this.routeService.getQueryParam<string>("name");
        this.isAppCenter = componentId == null;

        const appName = queryName ?? this.service.currentPageTitle;

        this.service.setCurrentComponent(appName ?? "", this.route).then((edge) => {
            this.edge = edge;
            this.edge
                .sendRequest(
                    this.websocket,
                    new ComponentJsonApiRequest({
                        componentId: "_appManager",
                        payload: new GetApp.Request({ appId: appId }),
                    }),
                )
                .then((getAppResponse) => {
                    const app = (getAppResponse as GetApp.Response).result.app;
                    const flag = app.flags.filter((t) => t.name === "canSwitchVersion");
                    if (flag.length > 0) {
                        this.canSwitchArchitecture(flag[0]);
                    }
                });
            edge.sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new GetAppInstances.Request({ appId: appId }),
                }),
            )
                .then((getInstancesResponse) => {
                    const recInstances = (getInstancesResponse as GetAppInstances.Response).result.instances;
                    edge.sendRequest(
                        this.websocket,
                        new ComponentJsonApiRequest({
                            componentId: "_appManager",
                            payload: new GetAppAssistant.Request({
                                appId: appId,
                            }),
                        }),
                    )
                        .then((getAppAssistantResponse) => {
                            const appAssistant = (getAppAssistantResponse as GetAppAssistant.Response).result;

                            if (this.isAppCenter == true) {
                                this.setInstance(appAssistant, null, recInstances, null, appId);
                                return;
                            }

                            if (componentId == null) {
                                this.setInstance(appAssistant, null, recInstances, null, appId);
                                return;
                            }

                            edge.sendRequest(
                                this.websocket,
                                new ComponentJsonApiRequest({
                                    componentId: "_appManager",
                                    payload: new QueryAppInstancesByFilter.Request({
                                        filter: {
                                            component: {
                                                componentId: [componentId],
                                            },
                                        },
                                        pagination: {
                                            limit: 1,
                                        },
                                    }),
                                }),
                            )
                                .then((queryAppInstancesByFilter) => {
                                    const queryedAppInstance = (
                                        queryAppInstancesByFilter as QueryAppInstancesByFilter.Response
                                    ).result.apps;

                                    this.setInstance(
                                        appAssistant,
                                        componentId,
                                        recInstances,
                                        queryedAppInstance,
                                        appId,
                                    );
                                })
                                .catch(
                                    InstallAppComponent.errorToast(
                                        this.service,
                                        (error) => "Error while receiving App-Instances for [" + appId + "]: " + error,
                                    ),
                                );
                        })
                        .catch(
                            InstallAppComponent.errorToast(
                                this.service,
                                (error) => "Error while receiving App Assistant for [" + appId + "]: " + error,
                            ),
                        );
                })
                .catch(
                    InstallAppComponent.errorToast(
                        this.service,
                        (error) => "Error while receiving App-Instances for [" + appId + "]: " + error,
                    ),
                )
                .finally(() => {
                    this.isRunning = false;
                    this.cdRef.markForCheck();
                });
        });
    }

    protected async submit(instance: MyInstance): Promise<void> {
        const edge = this.edge;
        if (edge == null) {
            return;
        }

        this.service.startSpinnerTransparentBackground(instance.instanceId);
        instance.isUpdating = true;

        try {
            const warnings = instance.form.dirty ? await this.updateInstance(edge, instance) : [];
            if (warnings == null) {
                return;
            }

            const isSwitchSuccessful = !this.switchArchitectureRequested() || (await this.switchArchitecture(edge));

            if (warnings.length > 0) {
                this.service.toast(warnings.join(";"), "warning");
            } else if (isSwitchSuccessful) {
                this.service.toast(this.translate.instant("EDGE.CONFIG.APP.SUCCESS_UPDATE"), "success");
            }
        } finally {
            instance.isUpdating = false;
            this.service.stopSpinner(instance.instanceId);
            this.cdRef.markForCheck();
        }
    }

    protected async submitDelete(instance: MyInstance) {
        const translate = this.translate;

        const alert = this.alertCtrl.create({
            subHeader: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM_HEADLINE"),
            message: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM_DESCRIPTION"),
            buttons: [
                {
                    text: translate.instant("GENERAL.CANCEL"),
                    role: "cancel",
                },
                {
                    text: translate.instant("EDGE.CONFIG.APP.DELETE_CONFIRM"),
                    handler: () => this.delete(instance),
                },
            ],
            cssClass: "alertController",
        });
        (await alert).present();
    }

    protected delete(instance: MyInstance) {
        const currentEdge = this.edge;
        if (currentEdge == null) {
            return;
        }
        this.service.startSpinnerTransparentBackground(instance.instanceId);
        instance.isDeleting = true;
        currentEdge
            .sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new DeleteAppInstance.Request({
                        instanceId: instance.instanceId,
                    }),
                }),
            )
            .then((response) => {
                this.instances().splice(this.instances().indexOf(instance), 1);
                this.service.toast(this.translate.instant("EDGE.CONFIG.APP.SUCCESS_DELETE"), "success");
                const navigationExtras = {
                    state: { appInstanceChange: true },
                    relativeTo: this.route,
                };
                const appId = this.routeService.getQueryParam<string>("appId");

                this.router.navigate(["../"], {
                    ...navigationExtras,
                    queryParams: {
                        name: this.appName,
                        appId: appId,
                    },
                });
            })
            .catch(
                InstallAppComponent.errorToast(this.service, (error) =>
                    this.translate.instant("EDGE.CONFIG.APP.FAIL_DELETE", {
                        error: error,
                    }),
                ),
            )
            .finally(() => {
                instance.isDeleting = false;
                this.service.stopSpinner(instance.instanceId);
                this.cdRef.markForCheck();
            });
    }

    private setInstance(
        appAssistant: GetAppAssistant.AppAssistant,
        componentId: string | null,
        recInstances: GetAppInstances.AppInstance[],
        queryedAppInstance: QueryAppInstancesByFilter.AppInstance[] | null,
        appId: string,
    ) {
        this.appName = appAssistant.name;
        this.instances.set([]);

        const first = queryedAppInstance?.[0];
        const instanceId = first?.instanceId ?? first?.["instanceId"];

        if (this.isAppCenter == true) {
            this.buildUiInstances(recInstances, appAssistant);
            return;
        }

        if (instanceId == null && appId == "App.Evse.ElectricVehicle.Generic") {
            this.service.toast(this.translateService.instant("EDGE.INDEX.WIDGETS.EVSE.VEHICLE_ID_ERROR"), "warning");
            this.buildUiInstances(recInstances, appAssistant);
            return;
        }

        if (componentId != null && this.isAppCenter == false) {
            const instancesFiltered = recInstances.filter((i) => i.instanceId == instanceId);
            this.buildUiInstances(instancesFiltered, appAssistant);
            return;
        }
    }

    private canSwitchArchitecture(flag: Flag): void {
        if (this.edge == null) {
            return;
        }
        if (!EdgePermission.hasSwitchArchitecture(this.edge)) {
            this.showSwitchArchitecture.set(false);
            return;
        }

        this.edge
            .sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: flag.handlerId,
                    payload: new CanSwitchArchitecture.Request(flag.canSwitchMethod),
                }),
            )
            .then((response) => {
                const result = (response as CanSwitchArchitecture.Response).result;
                this.switchMethod = flag.switchMethod;
                this.handlerId = flag.handlerId;
                this.showSwitchArchitecture.set(result.canSwitch);
                this.currentArchitecture.set(result.current ?? null);
                this.header = result.header;
                this.info = result.info;
                this.link = result.link;
                this.cdRef.markForCheck();
            })
            .catch((reason) => {
                console.log(reason);
            });
    }

    private setSwitchArchitecture(event: ToggleCustomEvent): void {
        const isCurrentlyNew = this.currentArchitecture() === "NEW";
        this.switchArchitectureRequested.set(event.detail.checked !== isCurrentlyNew);
    }

    /**
     * Updates the given app instance.
     *
     * @param edge The current edge
     * @param instance The instance to update
     * @returns The warnings of the update; null if the update failed
     */
    private async updateInstance(edge: Edge, instance: MyInstance): Promise<string[] | null> {
        const [err, response] = await JsonRpcUtils.handle(
            edge.sendRequest<UpdateAppInstance.Response>(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: "_appManager",
                    payload: new UpdateAppInstance.Request({
                        instanceId: instance.instanceId,
                        alias: instance.form.value["ALIAS"],
                        properties: prepareProperties(instance.form.value, instance.fields),
                    }),
                }),
            ),
        );

        if (err != null || response == null) {
            InstallAppComponent.errorToast(this.service, (error) =>
                this.translate.instant("EDGE.CONFIG.APP.FAIL_UPDATE", {
                    error: error,
                }),
            )(err);
            return null;
        }

        const result = response.result;
        instance.properties = result.instance.properties;
        instance.properties["ALIAS"] = result.instance.alias;
        instance.form.markAsPristine();
        return result.warnings ?? [];
    }

    /**
     * Switches the architecture of the app and navigates back on success.
     *
     * @param edge The current edge
     * @returns True if the architecture was switched
     */
    private async switchArchitecture(edge: Edge): Promise<boolean> {
        if (this.handlerId == null || this.switchMethod == null) {
            return false;
        }

        this.instances().forEach((instance) => {
            this.service.startSpinnerTransparentBackground(instance.instanceId);
            instance.isUpdating = true;
        });

        const [err] = await JsonRpcUtils.handle(
            edge.sendRequest(
                this.websocket,
                new ComponentJsonApiRequest({
                    componentId: this.handlerId,
                    payload: new SwitchArchitecture.Request(this.switchMethod),
                }),
            ),
        );

        this.instances().forEach((instance) => {
            this.service.stopSpinner(instance.instanceId);
            instance.isUpdating = false;
        });

        if (err != null) {
            this.service.toast(extractErrorMessage(err), "danger");
            return false;
        }

        this.switchArchitectureRequested.set(false);
        this.router.navigate(["../"], {
            state: { appInstanceChange: true },
            relativeTo: this.route,
        });
        return true;
    }

    private buildUiInstances(
        instances: GetAppInstances.AppInstance[],
        appAssistant: GetAppAssistant.AppAssistant,
    ): void {
        for (const instance of instances) {
            const form = new FormGroup({});

            const model = {
                ...(instance.alias != null ? { ALIAS: instance.alias } : {}),
                ...instance.properties,
            };

            const steps = [
                {
                    type: GetAppAssistant.AppConfigurationStepType.CONFIGURATION,
                    params: {},
                },
                ...(appAssistant.steps ?? []),
            ];

            this.instances.update((el) => {
                el.push({
                    instanceId: instance.instanceId,
                    form,
                    isDeleting: false,
                    isUpdating: false,
                    fields: GetAppAssistant.getInitialFields(
                        GetAppAssistant.postprocess(structuredClone(appAssistant)).fields,
                        structuredClone(model),
                        instance.instanceId,
                    ),
                    properties: model,
                    steps,
                });
                return el;
            });
        }
        this.service.stopSpinner(this.spinnerId);
    }
}
