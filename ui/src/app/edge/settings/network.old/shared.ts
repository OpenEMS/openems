export interface NetworkInterface {
    dhcp?: boolean,
    linkLocalAddressing?: boolean,
    gateway?: string,
    dns?: string,
    addresses?: string[]
}
