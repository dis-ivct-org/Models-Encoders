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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.nps.moves.dis.DetonationPdu;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.FirePdu;
import edu.nps.moves.dis.Pdu;
import edu.nps.moves.disenum.PduType;
import edu.nps.moves.disutil.PduFactory;

/**
 * Listen on specified port for Dis pdu and cumulate them in a list.
 */
public class DisReceiver {

    public static final int MAX_PDU_SIZE = 8192;

    /**
     * Thread responsible for receiving pdus
     */
    private ReceiverThread receiverThread;
    
    /**
     * List of entity received
     */
    private List<Pdu> receivedPdu = Collections.synchronizedList(new ArrayList<>());

    public DisReceiver(int port) throws SocketException {
        this.receiverThread = new ReceiverThread(port);
        new Thread(receiverThread).start();
    }

    /**
     * @return all the EntityStatePdu received yet
     */
    public List<EntityStatePdu> getReceivedEntityStatePdus() {

        List<EntityStatePdu> entityStatePduList= new ArrayList<>();
        synchronized (receivedPdu) {
            for (Pdu pdu : receivedPdu) {
                if (pdu != null && pdu.getPduType() == PduType.ENTITY_STATE.value) {
                    entityStatePduList.add((EntityStatePdu) pdu);
                }
            }
        }
        return entityStatePduList;
    }

    /**
     * @return all the EntityStatePdu received yet
     */
    public List<FirePdu> getReceivedFirePdus() {

        List<FirePdu> entityStatePduList= new ArrayList<>();
        synchronized (receivedPdu) {
            for (Pdu pdu : receivedPdu) {
                if (pdu != null && pdu.getPduType() == PduType.FIRE.value) {
                    entityStatePduList.add((FirePdu) pdu);
                }
            }
        }
        return entityStatePduList;
    }

    public List<DetonationPdu> getReceivedDetonationPdus() {
        List<DetonationPdu> pduList= new ArrayList<>();

        synchronized (this.receivedPdu) {
            for (Pdu pdu : receivedPdu) {
                if (pdu != null && pdu.getPduType() == PduType.DETONATION.value) {
                    pduList.add((DetonationPdu) pdu);
                }
            }
        }
        return pduList;
    }

//    /**
//     * @return all the pdu received yet
//     */
//    public List<Pdu> getReceivedPdus() {
//        return receivedPdu;
//    }

    /**
     * Remove all element in the list
     */
    public void clearList() {
        synchronized (receivedPdu) {
            receivedPdu.clear();
        }
    }
    
    public void terminate() {
        this.receiverThread.interrupt();
    }



    private class ReceiverThread implements Runnable {

        private boolean interrupted = false;
        private DatagramSocket socket = null;
        private PduFactory pduFactory = new PduFactory();

        public ReceiverThread(int port) throws SocketException {
            socket = new DatagramSocket(port);
        }

        public void run() {
            DatagramPacket packet;
            try {
                while (!interrupted) {
                    byte[] buffer = new byte[MAX_PDU_SIZE];
                    packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);
                    List<Pdu> pduBundle = pduFactory.getPdusFromBundle(packet.getData());

                    synchronized (receivedPdu) {
                        receivedPdu.addAll(pduBundle);
                    }
                }
            } catch (IOException e) {
                this.interrupt();
            }
        }

        public void interrupt() {
            interrupted = true;
            socket.close();
        }
    }

}
