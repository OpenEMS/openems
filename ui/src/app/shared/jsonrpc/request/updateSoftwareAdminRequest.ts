import { JsonrpcRequest } from '../base';

export class UpdateSoftwareAdminRequest extends JsonrpcRequest {

    static METHOD: string = "updateSoftwareAdmin";

    public constructor(
        public readonly params: {
            updateEdge: boolean,
            updateUi: boolean
        }
    ) {
        super(UpdateSoftwareAdminRequest.METHOD, params);
    }

}