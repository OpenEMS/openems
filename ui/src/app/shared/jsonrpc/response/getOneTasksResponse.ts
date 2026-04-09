export interface GetOneTasksResponse {
    jsonrpc: "2.0";
    id: string;
    result: {
        oneTasks: OneTask[];
    };
}

export interface OneTask<Payload extends Record<string, unknown> = {}> {
    "uid": string;
    "start": string;
    "end": string;
    "duration": string;
    "payload": Payload;
}
