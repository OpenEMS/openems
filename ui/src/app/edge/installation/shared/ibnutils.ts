import { Interface } from "src/app/shared/interface/interface";
import { ComponentJsonApiRequest } from "src/app/shared/jsonrpc/request/componentJsonApiRequest";
import { Edge, Websocket } from "src/app/shared/shared";
import { GetNetworkConfigRequest } from "../../settings/network/getNetworkConfigRequest";
import { GetNetworkConfigResponse } from "../../settings/network/getNetworkConfigResponse";
import { SetNetworkConfigRequest } from "../../settings/network/setNetworkConfigRequest";
import { AbstractIbn } from "../installation-systems/abstract-ibn";

export class IbnUtils {

    /**
     * Adds the IBN data to session storage safely.
     * This mehtod is specifically implemented to deal with "cyclic object value" exception, which is caused due to Object reference.
     * 
     * @param ibn The IBN
     */
    public static addIbnToSessionStorage(ibn: AbstractIbn) {
        sessionStorage.setItem('ibn', JSON.stringify(ibn, (key, value) => {
            // Do not stringify the translate service
            if (key === 'translate') return undefined;
            return value;
        }));
    }

    /**
     * Adds an ip address to the given interface.
     * Returns false if an error occurs.
     *
     * @param interfaceName Interface default 'eth0'
     * @param ip Ip that should be added
     * @param edge the current edge.
     * @param websocket the websocket connection.
     * @returns the status of adding ip address as boolean.
     */
    public static addIpAddress(interfaceName: string, ip: string, edge: Edge, websocket: Websocket) {
        let iface: Interface;

        edge.sendRequest(
            websocket,
            new ComponentJsonApiRequest({ componentId: '_host', payload: new GetNetworkConfigRequest() })
        ).then((response) => {
            const result = (response as GetNetworkConfigResponse).result;

            // Get interface
            for (const name of Object.keys(result.interfaces)) {
                if (name === interfaceName) {
                    iface = { name, model: result.interfaces[name] };
                }
            }

            // No interface with given name found
            if (!iface) {
                console.log('Network interface with name \'\'' + interfaceName + '\'\' was not found.');
                return false;
            }

            // Unset Gateway and DNS if DHCP is activated
            if (iface.model.dhcp) {
                iface.model.gateway = null;
                iface.model.dns = null;
            }

            // Set the ip in the model of the interface
            // or return if it already exists
            var address = ip.split('/');

            if ('addresses' in iface.model) {
                for (const addr of iface.model.addresses) {
                    if (addr.address == address[0]) {
                        return true;
                    }
                }

                iface.model.addresses.push({
                    label: '',
                    address: address[0],
                    subnetmask: this.getSubnetmaskAsString(address[1]),
                });

            } else {
                iface.model.addresses = new Array({
                    label: '',
                    address: address[0],
                    subnetmask: this.getSubnetmaskAsString(address[1]),
                });
            }

            const params = {
                interfaces: {}
            };
            params.interfaces[iface.name] = iface.model;

            edge.sendRequest(
                websocket,
                new ComponentJsonApiRequest({ componentId: '_host', payload: new SetNetworkConfigRequest(params) })
            ).then(() => true).catch((reason) => {
                console.log(reason);
            });
        }).catch(reason => {
            console.log(reason);
        });
        return false;
    }

    /**
     * Converts the subnetmask to a string address.
     * 
     * @param cidr the CIDR number
     * @returns the subnetmask as a string
     */
    private static getSubnetmaskAsString(cidr: any): string {
        var mask = [];
        for (var i = 0; i < 4; i++) {
            var n = Math.min(cidr, 8);
            mask.push(256 - Math.pow(2, 8 - n));
            cidr -= n;
        }
        return mask.join('.');
    }
}