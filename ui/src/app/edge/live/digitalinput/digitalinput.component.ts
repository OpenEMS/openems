import { Component } from '@angular/core';
import { Edge, EdgeConfig, Service, Websocket, ChannelAddress } from 'src/app/shared/shared';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'digitalinput',
    templateUrl: './digitalinput.component.html'
})

export class DigitalInputComponent {

    private static readonly SELECTOR = "digitalinput";

    public edge: Edge = null;
    public config: EdgeConfig = null;
    public ioComponents: EdgeConfig.Component[] = null;

    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        protected translate: TranslateService,
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });

        this.service.getConfig().then(config => {
            this.config = config;
            let channels = [];

            this.ioComponents = config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput").filter(component => component.isEnabled);
            for (let component of this.ioComponents) {

                for (let channel in component.channels) {
                    channels.push(
                        new ChannelAddress(component.id, channel)
                    );
                }



            }
            this.edge.subscribeChannels(this.websocket, DigitalInputComponent.SELECTOR, channels);

        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, DigitalInputComponent.SELECTOR);
        }
    }

}