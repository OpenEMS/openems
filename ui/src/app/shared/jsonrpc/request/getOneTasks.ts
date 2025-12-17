import { JsonrpcRequest } from "../base";

export class GetOneTasks extends JsonrpcRequest {

    private static METHOD: string = "getOneTasks";

    public constructor(
        private from: string,
        private to: string,
    ) {
        super(GetOneTasks.METHOD, {
            to: to,
            from: from,
        });
    }
}
