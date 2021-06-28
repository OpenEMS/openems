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

    @Input() componentId;
    @Input() controlName: string;
    @Input() formGroup: FormGroup;

    public isInitialized: boolean = false;
    public edge: Edge = null;
    public config: EdgeConfig = null;
    public component: EdgeConfig.Component = null;
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

    ngOnInit() {
        this.formGroup = this.getFormGroup();
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.service.getConfig().then(config => {
                // store important variables publically
                this.edge = edge;
                this.config = config;
                this.component = config.components[this.componentId];

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
}