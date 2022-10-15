/*
*   EDCOM 8.1 is a java cross platform library for communication with 10kW
*   hybrid Inverter (Katek Memmingen GmbH).
*   Copyright (C) 2022 Katek Memmingen GmbH
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU Lesser General Public License as published by
*   the Free Software Foundation, either version 3 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*   
*   You should have received a copy of the GNU Lesser General Public License
*   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.ed.test;

import com.ed.data.BatteryData;
import com.ed.data.DemoData;
import com.ed.data.EdDate;
import com.ed.data.EnergyMeter;
import com.ed.data.ErrorLog;
import com.ed.data.History;
import com.ed.data.InverterData;
import com.ed.data.Settings;
import com.ed.data.Status;
import com.ed.data.SystemInfo;
import com.ed.data.GridData;
import com.ed.edcom.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Calendar;
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.jmdns.ServiceInfo;

/**
 * Simple demo application
 * <ul>
 * <li> Library initialization</li>
 * <li> Search over local network (mDNS)</li>
 * <li> Read inverter data</li>
 * </ul>
 *
 */
public class EdcomTest {

    Client cl;
    boolean demoMode;

    DemoData demoData = new DemoData();
    EdDate dTime = new EdDate(1000);
    EnergyMeter energyMeter = new EnergyMeter();
    SystemInfo sysInfo = new SystemInfo();
    BatteryData battery = new BatteryData();
    InverterData inverter = new InverterData();
    GridData gridData = new GridData();
    History log = new History();
    Settings set = new Settings();
    Status stat = new Status();
    ErrorLog elog = new ErrorLog();

    private SortedMap<Date, SortedMap<String, Float>> historymap = new TreeMap<>();
    private SortedMap<Date, SortedMap<String, float[]>> hourMap = new TreeMap<>();

    private int historyState;
    private static long refreshRate = 1000;
    public static byte[] identKey = new byte[8];

    private EdcomTest(String inverterAddress, InetAddress localHost, String userKey, boolean demo) throws Exception {
        demoMode = demo;
        // Prepare communication
        cl = new Client(InetAddress.getByName(inverterAddress), localHost, 1);
        // Set user password
        if (userKey != null) {
            cl.setUserPass(userKey);
        }
        // Register data
        dTime.registerData(cl);
        if (demo) {
            demoData.registerData(cl);
        } else {
            //energyMeter.registerData(cl);
            sysInfo.registerData(cl);
            battery.registerData(cl);
            inverter.registerData(cl);
            gridData.registerData(cl);
            log.registerData(cl);
            set.registerData(cl);
            stat.registerData(cl);
            elog.registerData(cl);
        }
        // Start communication
        cl.start();
    }

    /**
     * Stop test
     *
     * @throws IOException
     */
    void stop() throws IOException {
        cl.close(); // Stop communication (!)
    }

    int i = 0;

    static byte[] encryptIdentKey(byte[] randomKey, byte[] identKey, int len) {
        byte[] tmp = new byte[len];
        System.arraycopy(identKey, 0, tmp, 0, len);
        // apply key
        for (int i = 0; i < tmp.length && i < randomKey.length; i++) {
            tmp[i] += randomKey[i];
        }
        // simple mix
        for (int i = 0; i < 99; i++) {
            tmp[i % len] += 1;
            tmp[i % len] += tmp[(i + 10) % len];
            tmp[(i + 3) % len] *= tmp[(i + 11) % len];
            tmp[i % len] += tmp[(i + 7) % len];
        }

        return tmp;
    }

    /**
     * Read and print inverter data
     */
    void test() {

        if (cl.isConnected()) {

            if (cl.getAccessFeedb() != 1) {
                if (!cl.isIdAccepted()) {
                    System.out.println("Wrong ident key!");
                }
                if (!cl.isPasswordAccepted()) {
                    System.out.println("Wrong user password!");
                }
            }

            // Clients ID list
            if (cl.getClientIdListRefreshTime() > 0) {
                System.out.println(String.format("Connected clients IDs: %d %d %d %d \n",
                        cl.getClientId(0), cl.getClientId(1),
                        cl.getClientId(2), cl.getClientId(3)));
            }

            if (this.historymap.isEmpty()) {
                if (cl.isPasswordAccepted()) {
                    System.out.println("Retrieving History Values...");
                    try {
                        this.historymap = log.getHistoryYear();
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    System.out.println("Done\n");
                }

            } else {
                String history = "HISTORY YEAR:\t";
                Date date = historymap.lastKey();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(date);
                history += calendar.get(Calendar.YEAR) + "\n";
                SortedMap<String, Float> values = historymap.get(date);
                for (String param : values.keySet()) {
                    history += param + "\t" + values.get(param) + "\n";
                }
                System.out.println(history + "\n");
            }

            // Demo data
            if (demoData.dataReady()) {
                System.out.println(dTime);
                System.out.println(demoData);
                demoData.refresh();
            }
            // Energy meter
            if (energyMeter.dataReady()) {
                System.out.println(dTime);
                System.out.println(energyMeter);
                energyMeter.refresh();
            }
            // System informationen
            if (!demoMode) {

                System.out.println(sysInfo);
            }
            if (sysInfo.dataReady()) {
                sysInfo.refresh();
                System.out.println(dTime);
            }
            // Battery measurements
            if (battery.dataReady()) {
                System.out.println(battery);
                battery.refresh();
            }
            // Inverter measurements
            if (inverter.dataReady()) {
                System.out.println(inverter);
                System.out.println("refresh time : " + inverter.pL1.refreshTime() + "\n");
                inverter.refresh();
            }
            // VECTIS measurements
            if (gridData.dataReady()) {
                System.out.println(gridData);
                gridData.refresh();
            }
            // Inverter set
            if (set.dataReady()) {
                System.out.println("Count: " + i);
                if (i == 5) {
                    //set.setBatUsageLimit(100);
                    //set.setBatSocLimit(100);
                }
                System.out.println(set);
                set.refresh();
                i++;
            }
            // Inverter status
            if (stat.dataReady()) {
                System.out.println(stat);
                stat.refresh();
            }
            // Error log
            if (elog.dataReady()) {
                System.out.println(elog);
                elog.refresh();
            }
        }
    }

    /**
     * Main
     *
     * @param args application command line parameters (see Help)
     */
    public static void main(String args[]) {
        try {
            InetAddress lHost = null;
            int argIx = 0;
            /* Help */
            if (args.length == 0) {
                printHelp();
            }

            while (argIx < args.length) { // process user input

                if (args[argIx].equals("-h") || args[argIx].equals("-help")
                        || args[argIx].equals("-?")) {
                    printHelp();
                }

                /* Initialize library */
                Util.getInstance().init();
                if (args[argIx].equals("-l")) {
                    if (args[argIx + 1].equals("demo")) {
                        System.out.println("DEMO mode unavailable!");
                        printHelp();
                        
                    } else {
                        identKey = args[argIx + 1].getBytes();

                    }
                    Util.getInstance().setListener(new ClientListener() {
                        @Override
                        public byte[] updateIdentKey(byte[] randomKey) {
                            return encryptIdentKey(randomKey, EdcomTest.identKey, 8);

                        }
                    });
                    lHost = InetAddress.getLocalHost();
                    System.out.println("Host: " + lHost.getHostAddress());
                    argIx += 1;
                }
                /*  Network interfaces info */
                if (args[argIx].equals("-i")) {
                    ClientFactory.printNetworkInterfaces(false);
                }
                /*  Select Network Interface */
                if (args[argIx].equals("-d")) {
                    NetworkInterface ni = NetworkInterface.getByName(args[argIx + 1]);
                    if (ni != null) {
                        lHost = ni.getInetAddresses().nextElement();
                        System.out.println("Network interface name : " + args[argIx + 1]);
                    } else {
                        throw new IOException("Selected network interface does not exist");
                    }
                    argIx += 1;
                }
                /*  Set Refresh Rate */
                if (args[argIx].equals("-r")) {
                    refreshRate = Long.parseLong(args[argIx + 1]);
                    System.out.println(String.format("Refresh rate : %d [ms]\n", refreshRate));
                    argIx += 1;
                }
                /*  Search local inverters */
                if (args[argIx].equals("-s")) {
                    System.out.println("Start mDNS oparation and show all inverters found...");
                    Discovery nd = Discovery.getInstance(InetAddress.getByName("0.0.0.0"));
                    ServiceInfo[] sl = nd.refreshInverterList();
                    int i = 1;
                    if (sl.length == 0) {
                        System.out.println("No inverters found. \n");
                    } else {
                        for (ServiceInfo s : sl) {
                            System.out.println("Inverter " + (i++) + " @ IP: " + s.getHostAddress() + " NetBIOS: " + s.getName() + " SN: " + s.getTextString());
                        }
                        System.out.print("\n");
                    }
                    nd.close();
                }
                /*  Connect by IP or NetBIOS name */
                if (args[argIx].equals("-c")) {
                    System.out.println("Try to connect to " + args[argIx + 1] + " inverter...");
                    EdcomTest testClient = new EdcomTest(args[argIx + 1], lHost, args[argIx + 2], false);
                    System.in.skip(System.in.available());
                    while (true) {
                        testClient.test();
                        System.out.println("Sleep " + refreshRate);
                        Thread.sleep(refreshRate);
                        if (System.in.available() > 0) {
                            System.in.read();
                            break;
                        }  // press enter to exit
                    }
                    testClient.stop();  // stop test
                    argIx += 2;
                }
                /*  Search and connect */
                if (args[argIx].equals("-sc")) {
                    ServiceInfo sl = null;
                    Discovery nd = Discovery.getInstance(InetAddress.getByName("0.0.0.0"));
                    if (args[argIx + 1].equals("any") || args[argIx + 1].equals("ANY")) {
                        System.out.println("Connect to random inverter ...");
                        ServiceInfo[] al = nd.refreshInverterList();
                        if (al.length > 0) {
                            sl = al[0];
                        } // get first entry found
                    } // connect random inverter ?
                    else {
                        System.out.println("Resolve name and connect to inverter by mDNS name or Serial Number " + args[argIx + 1] + " ...");
                        sl = nd.getByMac(args[argIx + 1]);
                        if (sl == null) {
                            sl = nd.getBySerialNumber(args[argIx + 1]);
                        }
                    } // connect specified inverter ?
                    nd.close();
                    if (sl != null) {
                        InetAddress inverterAddres = InetAddress.getByName(sl.getHostAddress());
                        EdcomTest testClient = new EdcomTest(inverterAddres.getHostAddress(), lHost, args[argIx + 2], false);
                        System.in.skip(System.in.available());
                        while (true) {
                            testClient.test();
                            Thread.sleep(refreshRate);
                            if (System.in.available() > 0) {
                                System.in.read();
                                break;
                            }  // press enter to exit
                        }
                        testClient.stop(); // stop test
                    } // inverter found ?
                    else {
                        System.out.println("No inverter " + args[argIx + 1] + " found");
                    }
                    argIx += 2;
                }
                argIx++;
            }
            waitForExit();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("exit");
        }
    }

    /**
     * Print help text
     */
    private static void printHelp() {
        System.out.println("EDCOM " + Util.getInstance().getEdcomVersion());
        System.out.println("Communication API for Centurio inverter (Katek Memmingen GmbH)");
        System.out.println("Simple Demo Application running...");
        System.out.println("\n");
        System.out.println("Syntax: EdcomDemoApp.exe    [-l library_password] [-i] [-d interface-name]");
        System.out.println("                            [-r refresh-rate] [-s]");
        System.out.println("                            [-c inverter-address user-password]");
        System.out.println("                            [-sc inverter-name user-password]");
        System.out.println("Options:");
        System.out.println(" -l     API Library activation");
        System.out.println("            library_password : please contact Katek Memmingen GmbH");
        System.out.println(" -i     Show available Network Interfaces");
        System.out.println(" -d     Select current Network Interface");
        System.out.println("            interface-name      : selected network interface to use for future operations");
        System.out.println(" -r     Set refresh rate");
        System.out.println("            refresh-rate        : data refresh rate [ms] (default refresh rate is 1000 [ms])");
        System.out.println(" -s     Start mDNS oparation and show all inverters found");
        System.out.println(" -c     Connect to selected inverter (local access only!)");
        System.out.println("            inverter-address    : current inverter IPv4 addres,");
        System.out.println("                                  NetBIOS name as string representation of inverter MAC address (Hexadecimal digits only)");
        System.out.println("                                  or default IPv4 (192.168.100.115) for Network without a DHCP server");
        System.out.println("            user-password       : inverter password set by user");
        System.out.println("                                (Press ENTER to stop current operation)");
        System.out.println(" -sc    Resolve name and connect to inverter by mDNS name or Serial Number");
        System.out.println("            inverter-name       : string representation of inverter MAC address (same as NetBIOS name)");
        System.out.println("                                  or Serial Number of inverter");
        System.out.println("                                  or use keyword 'ANY' to connect random inverter");
        System.out.println("            user-password       : inverter password set by user");
    }

    private static void waitForExit() throws IOException {
        System.out.println("\nPress ENTER to exit");
        boolean wait = true;

        while (wait) {
            if (System.in.available() > 0) {
                System.in.read();
                wait = false;
            }  // press enter to exit
        }
    }
}
