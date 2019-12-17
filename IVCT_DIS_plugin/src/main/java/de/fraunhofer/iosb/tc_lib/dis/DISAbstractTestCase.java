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

import org.slf4j.Logger;

import de.fraunhofer.iosb.tc_lib.BaseModel;
import de.fraunhofer.iosb.tc_lib.GenericTestCase;
import de.fraunhofer.iosb.tc_lib.TcInconclusive;

public abstract class DISAbstractTestCase extends GenericTestCase {

    protected DisManager disManager;
    protected DisTcParam param;
    
    protected DisManager getDisManager() throws TcInconclusive {
        if (disManager == null) {throw new TcInconclusive("DisManager is not initialized");}
        return this.disManager;
    }

    protected BaseModel getBaseModel(final String tcParamJson, final Logger logger) throws TcInconclusive {
        this.param = new DisTcParam(tcParamJson);
        disManager = new DisManager(param);
        return disManager;
    }
    
    protected void postambleAction(Logger logger) throws TcInconclusive {
        disManager.terminateRti();
    }
}