import { Directive, Inject, Input, } from "@angular/core";
import { FormBuilder, FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { ModalController } from "@ionic/angular";
import { TranslateService } from "@ngx-translate/core";
import { UUID } from "angular2-uuid";
import { Subject } from "rxjs";
import { takeUntil } from "rxjs/operators";
import { ChannelAddress, CurrentData, Edge, EdgeConfig, Service, Websocket } from "src/app/shared/shared";

@Directive()
export abstract class AbstractModal {

    @Input() componentId: string;
    @Input() controlName: string;
    @Input() formGroup: FormGroup;

    public loading: boolean;
    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    @Input() component: EdgeConfig.Component = null;
    public stopOnDestroy: Subject<void> = new Subject<void>();

    private selector: string = UUID.UUID().toString();

    constructor(
        @Inject(Websocket) protected websocket: Websocket,
        @Inject(ActivatedRoute) protected route: ActivatedRoute,
        @Inject(Service) protected service: Service,
        @Inject(ModalController) public modalCtrl: ModalController,
        @Inject(TranslateService) protected translate: TranslateService,
        @Inject(FormBuilder) public formBuilder: FormBuilder,
    ) {
    }

    ngOnChanges() {
        this.formGroup = this.getFormGroup()
    }
    ngOnInit() {
        this.formGroup = this.getFormGroup();
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;

                // // announce initialized
                this.isInitialized = true;

                // get the channel addresses that should be subscribed
                let channelAddresses: ChannelAddress[] = this.getChannelAddresses();
                let channelIds = this.getChannelIds();
                for (let channelId of channelIds) {
                    channelAddresses.push(new ChannelAddress(this.componentId, channelId));
                }
                if (channelAddresses.length != 0) {
                    this.edge.subscribeChannels(this.websocket, this.selector, channelAddresses);
                }
                // call onCurrentData() with latest data
                edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                    let allComponents = {};
                    let thisComponent = {};
                    for (let channelAddress of channelAddresses) {
                        let ca = channelAddress.toString();
                        allComponents[ca] = currentData.channel[ca];
                        if (channelAddress.componentId === this.componentId) {
                            thisComponent[channelAddress.channelId] = currentData.channel[ca];
                        }
                    }
                    this.onCurrentData({ thisComponent: thisComponent, allComponents: allComponents });
                });
            });
        });
    }
    /**
       * Called on every new data.
       * 
       * @param currentData new data for the subscribed Channel-Addresses
       */
    protected onCurrentData(currentData: CurrentData) {
    }

    /**
     * Gets the ChannelAddresses that should be subscribed.
     */
    protected getChannelAddresses(): ChannelAddress[] {
        return [];
    }
    protected getFormGroup(): FormGroup {
        return
    }
    /**
   * Gets the ChannelIds of the current Component that should be subscribed.
   */
    protected getChannelIds(): string[] {
        return [];
    }

    // applyChanges() {
    //     console.log("test 1")
    //     if (this.edge != null) {
    //         console.log("test 2")
    //         // if (this.edge.roleIsAtLeast('owner')) {
    //         console.log("test 3")
    //         let updateComponentArray = [];
    //         Object.keys(this.formGroup.controls).forEach((element, index) => {
    //             if (this.formGroup.controls[element].dirty) {
    //                 updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
    //             }
    //         })
    //         this.loading = true;
    //         this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
    //             this.component.properties['power'] = this.formGroup.controls['power'].value;
    //             console.log("componentproperties", this.component.properties['power'])
    //             this.loading = false;
    //             this.formGroup.markAsPristine()
    //             this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    //         }).catch(reason => {
    //             this.formGroup.controls['mode'].setValue(this.component.properties.mode);
    //             this.formGroup.controls['power'].setValue(this.component.properties['power']);
    //             this.loading = false;
    //             this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
    //             console.warn(reason);
    //         })
    //         // } else {
    //         //     this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
    //     }
    //     // }
    // }
}