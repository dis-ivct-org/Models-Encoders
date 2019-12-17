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

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import ca.drdc.ivct.fom.warfare.MunitionDetonation;
import de.fraunhofer.iosb.tc_lib.BaseModel;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;
import edu.nps.moves.dis.DetonationPdu;
import edu.nps.moves.dis.EntityStatePdu;
import edu.nps.moves.dis.FirePdu;
import edu.nps.moves.dis.Pdu;
import edu.nps.moves.disenum.PduType;

/**
 * Manage connection to a DIS simulation network
 *
 */
public class DisManager implements BaseModel {

    private DisReceiver receiver;
    private DisSender sender;

    public DisManager(DisTcParam param) throws TcInconclusive {
        try {
            this.sender = new DisSender(param.getSutHostName(), param.getSutPort());
            this.receiver = new DisReceiver(param.getListeningPort());
        } catch (UnknownHostException | SocketException e) {
            throw new TcInconclusive("Connection error : "+e.getMessage(), e);
        }
    }

    
    public void sendPdu(EntityStatePdu entityStatePdu) {
        sender.sendPdu(entityStatePdu);
    }
    
    public List<EntityStatePdu> getReceivedEntities() {
        return this.receiver.getReceivedEntityStatePdus();
    }

    public List<FirePdu> getReceivedFirePdu() {
        return this.receiver.getReceivedFirePdus();
    }

    public List<DetonationPdu> getReceivedDetonationPdus() {
        return this.receiver.getReceivedDetonationPdus();
    }

//    public List<Pdu> getPdus() {
//        return this.receiver.getReceivedPdus();
//    }

    @Override
    public void terminateRti() {
        receiver.terminate();
        sender.terminate();
        receiver = null;
        sender = null;
    }
}
