import { saveAs } from "file-saver-es";
import { Base64PayloadResponse } from "../../jsonrpc/response/base64PayloadResponse";
/** Helper functions for interacting with files. */
export namespace FileUtils {
    /**
     * Download a JSONRPC Base64PayloadResponse in Excel (XLSX) file format.
     *
     * @param response The Base64PayloadResponse
     * @param filename The filename without .xlsx suffix
     */
    export function downloadXlsx(
        response: Base64PayloadResponse,
        filename: string,
    ) {
        // decode base64 string, remove space for IE compatibility
        // source: https://stackoverflow.com/questions/36036280/base64-representing-pdf-to-blob-javascript/45872086
        const binary = atob(response.result.payload.replace(/\s/g, ""));
        const len = binary.length;
        const buffer = new ArrayBuffer(len);
        const view = new Uint8Array(buffer);
        for (let i = 0; i < len; i++) {
            view[i] = binary.charCodeAt(i);
        }
        const data: Blob = new Blob([view], {
            type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8",
        });

        saveAs(data, filename + ".xlsx");
    }
}
