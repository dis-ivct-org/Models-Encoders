/*******************************************************************************
 * Copyright (C) Her Majesty the Queen in Right of Canada, 
 * as represented by the Minister of National Defence, 2018
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package de.fraunhofer.iosb.tc_lib.dis;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import edu.nps.moves.dis.Pdu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.nps.moves.dis.EntityStatePdu;

/**
 * Send EntityStatePdu to the specified broadcasting address and port
 */
public class DisSender {
    
    private static Logger logger = LoggerFactory.getLogger(DisSender.class);
 
    private DatagramSocket socket = null;

    private InetAddress bcastAddress;

    private int sPort;

    /** The multicast address we plan to send on, and the port, in string format 
     * @param bcast hostname or textual representation of a hostname of the host to send to
     * @param sPort the port to send to
     * @throws UnknownHostException if the broadcast host is malformed
     * @throws SocketException if the socket could not be opened, or the socket could not bind to the specified local port.
     */
    public DisSender(String bcast, int sPort) throws UnknownHostException, SocketException {
        
        this.sPort = sPort;
        this.bcastAddress = InetAddress.getByName(bcast);
        socket = new DatagramSocket();

    }

    /**
     * Send any Dis Pdu
     * @param pdu any DIS Pdu
     */
    public void sendPdu(Pdu pdu) {

        logger.info("Sending pdu "+pdu);
        // The marshalling process here will update the timestamp. 
        //marshall according to the IEEE 1278.1 DIS formatted byte array
        byte[] data = pdu.marshalWithDisAbsoluteTimestamp();

        // Stuff that byte array into a UDP packet and send it
        DatagramPacket packet = new DatagramPacket(data, data.length, bcastAddress, sPort);
        try {
            socket.send(packet);
        } catch (IOException e) {
            logger.error("Error while sending pdu type :{}", pdu.getPduTypeEnum(), e);
        }
    }
    
    /**
     * Terminate the connection 
     */
    public void terminate() {
        this.socket.close();
    }
}
