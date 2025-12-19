import { JsonrpcRequest } from "../base";

export class GetAllTasks extends JsonrpcRequest {

    private static METHOD: string = "getAllTasks";

    public constructor(
    ) {
        super(GetAllTasks.METHOD, {});
    }

}
