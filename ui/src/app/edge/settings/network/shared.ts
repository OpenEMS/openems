export type NetworkInterface = {
    dhcp?: boolean,
    gateway?: string,
    dns?: string,
    linkLocalAddressing?: boolean,
    addresses?: IpAddress[]
}

export type IpAddress = {
    label: string,
    address: string,
    subnetmask: string
}