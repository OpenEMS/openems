// @ts-strict-ignore
import { Component, Input, OnDestroy, OnInit } from "@angular/core";
import { ModalController } from "@ionic/angular";
import { JsonrpcRequest, JsonrpcResponseSuccess } from "src/app/shared/jsonrpc/base";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Channel } from "src/app/shared/jsonrpc/response/getChannelsOfComponentResponse";
import { ChannelAddress, Edge, EdgeConfig, EdgePermission, Service, Websocket } from "../../../../../shared/shared";

@Component({
    selector: "Io_Api_DigitalInputModal",
    templateUrl: "./MODAL.COMPONENT.HTML",
    standalone: false,
})
export class Io_Api_DigitalInput_ModalComponent implements OnInit, OnDestroy {
    private static readonly SELECTOR = "Io_Api_DigitalInput_ModalComponent";

    @Input({ required: true }) public edge!: Edge;
    @Input({ required: true }) public ioComponents!: EDGE_CONFIG.COMPONENT[];

    protected digitalInputChannelsPerComponent: { componentId: string, componentAlias: string, channels: Channel[] }[];

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
        private websocket: Websocket,
    ) { }

    ngOnInit(): void {
        THIS.GET_DIGITAL_INPUT_CHANNELS().then(channelsPerComponent => {
            THIS.DIGITAL_INPUT_CHANNELS_PER_COMPONENT = channelsPerComponent;
            const channels = THIS.DIGITAL_INPUT_CHANNELS_PER_COMPONENT.REDUCE((p, c) => {
                return [...p, ...C.CHANNELS.MAP(e => new ChannelAddress(C.COMPONENT_ID, E.ID))];
            }, []);
            THIS.EDGE.SUBSCRIBE_CHANNELS(THIS.WEBSOCKET, Io_Api_DigitalInput_ModalComponent.SELECTOR, channels);
        });
    }

    ngOnDestroy(): void {
        THIS.EDGE.UNSUBSCRIBE_CHANNELS(THIS.WEBSOCKET, Io_Api_DigitalInput_ModalComponent.SELECTOR);
    }

    private async getDigitalInputChannels(): Promise<{ componentId: string, componentAlias: string, channels: Channel[] }[]> {
        if (EDGE_PERMISSION.HAS_CHANNELS_IN_EDGE_CONFIG(THIS.EDGE)) {
            return THIS.IO_COMPONENTS.MAP(e => {
                return {
                    componentId: E.ID,
                    componentAlias: E.ALIAS,
                    channels: OBJECT.ENTRIES(E.CHANNELS)
                        .filter(([key, value]) => {
                            if (VALUE.ACCESS_MODE !== "RO") {
                                return false;
                            }
                            if (VALUE.TYPE !== "BOOLEAN") {
                                return false;
                            }
                            if (key === "_PropertyEnabled") {
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

        const response = await THIS.EDGE.SEND_REQUEST<GetDigitalInputChannelsOfComponentsResponse>(THIS.WEBSOCKET, new ComponentJsonApiRequest({
            componentId: "_componentManager",
            payload: new GetDigitalInputChannelsOfComponentsRequest({ componentIds: THIS.IO_COMPONENTS.MAP(e => E.ID) }),
        }));
        return RESPONSE.RESULT.CHANNELS_PER_COMPONENT.MAP(e => {
            return {
                componentAlias: THIS.IO_COMPONENTS.FIND(c => C.ID == E.COMPONENT_ID)?.alias ?? E.COMPONENT_ID,
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
        super(GET_DIGITAL_INPUT_CHANNELS_OF_COMPONENTS_REQUEST.METHOD, params);
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
