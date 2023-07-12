export type Edges = [{
    id: string,
    comment: string,
    producttype: string,
    version: string
    role: "admin" | "installer" | "owner" | "guest",
    isOnline: boolean,
    lastmessage: Date
}];

export type User = {
    id: string,
    name: string,
    globalRole: "admin" | "installer" | "owner" | "guest",
    language: string,
    hasMultipleEdges: boolean
};