export interface NetworkInterface {
    dhcp?: boolean,
    linkLocalAddressing?: boolean,
    gateway?: string | null,
    dns?: string | null,
    addresses?: string[]
}