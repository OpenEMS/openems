export interface GetAllTasksResponse {
    jsonrpc: "2.0";
    id: string;
    result: {
        tasks: SingleTask[];
    };
}

export interface SingleTask {
    "@type": "Task";
    "uid": string;
    "updated": string;            // ISO timestamp string
    "start": string;              // e.g. "08:30:00"
    "duration": string;           // e.g. "PT15M"

    "recurrenceRules": RecurrenceRule[];

    /** Your backend uses this key: "openems.io:payload" */
    ["openems.io:payload"]: ManualPayload;
}

export interface RecurrenceRule {
    frequency: string;   // your serializer uses enum RecurrenceFrequency
    until?: string | null;
    byDay?: string[];    // lowercase day strings: ["mo"], ["tu"], ...
}

export interface ManualPayload {
    class: string;       // "Manual"
    mode: string;        // "FORCE", "MANUAL", etc.
}
