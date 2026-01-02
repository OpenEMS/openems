export interface GetOneTasksResponse {
    jsonrpc: "2.0";
    id: string;
    result: {
        oneTasks: OneTask[];
    };
}

export interface OneTask {
    "uid": string;
    "start": string;
    "end": string;
    "duration": string;
    "payload": Payload;
}

export interface Payload {
    class: string,
    mode: string,
}
