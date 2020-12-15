import { JsonrpcResponseSuccess } from '../base';

export class UpdateSoftwareAdminResponse extends JsonrpcResponseSuccess {

    public constructor(
        public readonly id: string,
        public readonly result: {
            Success: number,
            Error: number
        }
    ) {
        super(id, result);
    }

}