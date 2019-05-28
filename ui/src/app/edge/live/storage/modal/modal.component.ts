import { Component, OnInit, Input } from '@angular/core';
import { Service, EdgeConfig, Edge, Websocket, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { ModalController } from '@ionic/angular';
import { stringToKeyValue } from '@angular/flex-layout/extended/typings/style/style-transforms';

@Component({
    selector: 'storage-modal',
    templateUrl: './modal.component.html',
})
export class StorageModalComponent implements OnInit {

    private static readonly SELECTOR = "storage-modal";

    @Input() edge: Edge;

    edgeConfig: EdgeConfig = null;
    components: EdgeConfig.Component[] = null;
    component: EdgeConfig.Component = null;
    public outputChannel: ChannelAddress[] = null;


    constructor(
        public service: Service,
        public translate: TranslateService,
        public modalCtrl: ModalController,
        public websocket: Websocket,

    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            let components = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss");
            for (let component of components) {

                // component.channels
                let factoryID = component.factoryId;
                let factory = config.factories[factoryID];
                // console.log("COMPONONENTO1", component)
                if ((factory.natureIds.includes("io.openems.edge.ess.api.SymmetricEss"))) {
                    this.components = components;
                    // this.edge.subscribeChannels(this.websocket, StorageModalComponent.SELECTOR + component.id, [

                    // ])
                }

            }
            // this.edgeConfig = config;
            // console.log("config:", config)
            // console.log("components:", this.components)
        })
    }
    //  this.outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
    //     edge.subscribeChannels(this.websocket, ChannelthresholdComponent.SELECTOR + this.componentId, [
    //       this.outputChannel
    //     ]);
    // this.service.getConfig().then(config => {
    //     let controllers = config.getComponentsByFactory("Controller.Evcs");
    //     for (let controller of controllers) {
    //       let properties = controller.properties;
    //       if ("evcs.id" in properties && properties["evcs.id"] === this.componentId) {
    //         // this 'controller' is the Controller responsible for this EVCS
    //         this.controller = controller;
    //         return;
    //       }
    //     }
    //   });

}