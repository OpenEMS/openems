// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Edge, Service, EdgeConfig, Websocket, ChannelAddress, EdgePermission } from '../../../../../shared/shared';
import { ModalController } from '@ionic/angular';
import { Channel } from 'src/app/shared/jsonrpc/response/getChannelsOfComponentResponse';
import { ComponentJsonApiRequest } from 'src/app/shared/jsonrpc/request/componentJsonApiRequest';
import { JsonrpcRequest, JsonrpcResponseSuccess } from 'src/app/shared/jsonrpc/base';

@Component({
    selector: 'Io_Api_DigitalInputModal',
    templateUrl: './modal.component.html',
})
export class Io_Api_DigitalInput_ModalComponent implements OnInit, OnDestroy {
    private static readonly SELECTOR = "Io_Api_DigitalInput_ModalComponent";

    @Input({ required: true }) public edge!: Edge;
    @Input({ required: true }) public ioComponents!: EdgeConfig.Component[];

    protected digitalInputChannelsPerComponent: { componentId: string, componentAlias: string, channels: Channel[] }[];

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private websocket: Websocket,
    ) { }

    ngOnInit(): void {
        this.getDigitalInputChannels().then(channelsPerComponent => {
            this.digitalInputChannelsPerComponent = channelsPerComponent;
            const channels = this.digitalInputChannelsPerComponent.reduce((p, c) => {
                return [...p, ...c.channels.map(e => new ChannelAddress(c.componentId, e.id))];
            }, []);
            this.edge.subscribeChannels(this.websocket, Io_Api_DigitalInput_ModalComponent.SELECTOR, channels);
        });
    }

    ngOnDestroy(): void {
        this.edge.unsubscribeChannels(this.websocket, Io_Api_DigitalInput_ModalComponent.SELECTOR);
    }

    private async getDigitalInputChannels(): Promise<{ componentId: string, componentAlias: string, channels: Channel[] }[]> {
        if (EdgePermission.hasChannelsInEdgeConfig(this.edge)) {
            return this.ioComponents.map(e => {
                return {
                    componentId: e.id,
                    componentAlias: e.alias,
                    channels: Object.entries(e.channels)
                        .filter(([key, value]) => {
                            if (value.accessMode !== 'RO') {
                                return false;
                            }
                            if (value.type !== 'BOOLEAN') {
                                return false;
                            }
                            if (key === '_PropertyEnabled') {
                                return false;
                            }
                            return true;
                        })
                        .map(([key, value]) => {
                            return { id: key, ...value };
                        }),
                };
            });
        }

        const response = await this.edge.sendRequest<GetDigitalInputChannelsOfComponentsResponse>(this.websocket, new ComponentJsonApiRequest({
            componentId: '_componentManager',
            payload: new GetDigitalInputChannelsOfComponentsRequest({ componentIds: this.ioComponents.map(e => e.id) }),
        }));
        return response.result.channelsPerComponent.map(e => {
            return {
                componentAlias: this.ioComponents.find(c => c.id == e.componentId)?.alias ?? e.componentId,
                ...e,
            };
        });
    }

}

export class GetDigitalInputChannelsOfComponentsRequest extends JsonrpcRequest {

    private static METHOD: string = "getDigitalInputChannelsOfComponents";

    public constructor(
        public override readonly params: {
            componentIds: string[],
        },
    ) {
        super(GetDigitalInputChannelsOfComponentsRequest.METHOD, params);
    }

}

export class GetDigitalInputChannelsOfComponentsResponse extends JsonrpcResponseSuccess {

    public constructor(
        public override readonly id: string,
        public override readonly result: {
            channelsPerComponent: { componentId: string, channels: Channel[] }[],
        },
    ) {
        super(id, result);
    }

}
