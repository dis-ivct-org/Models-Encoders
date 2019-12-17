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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import de.fraunhofer.iosb.tc_lib.TcInconclusive;

public class DisTcParam {

    private String sutHostName;
    private Integer sutPort;
    private Integer listeningPort;
    private Integer waitingPeriod;
    private List<URL> fadUrls;
    private Map<String, Double> spatialValueThreshold;
    
    public DisTcParam(String tcParamJsonString) throws TcInconclusive {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject;
        try {
            jsonObject = (JSONObject) jsonParser.parse(tcParamJsonString);

            listeningPort = Integer.parseInt((String) jsonObject.get("listeningPort"));
            if (listeningPort == null) {
                throw new TcInconclusive("The key 'listeningPort' was not found in the configuration");
            }
            
            sutHostName = (String) jsonObject.get("sutHostName");
            if (sutHostName == null) {
                throw new TcInconclusive("The key 'sutHostName' was not found in the configuration");
            }
            
            sutPort = Integer.parseInt((String) jsonObject.get("sutPort"));
            if (sutPort == null) {
                throw new TcInconclusive("The key 'sutPort' was not found in the configuration");
            }

            waitingPeriod = Integer.parseInt(String.valueOf(jsonObject.get("waitingPeriod")));
            if (waitingPeriod == null) {
                // default to 10 seconds
                waitingPeriod = 10;
            }
            // get FOM files list from the JSON object
            JSONArray fadArray = (JSONArray) jsonObject.get("fadFiles");
            if (fadArray == null) {
                throw new TcInconclusive("The key fadFiles was not found");
            } else {
                this.setFadUrls(jsonArrayToUrlList(fadArray));
            }

            spatialValueThreshold = new HashMap<>();


            JSONObject thresholds = (JSONObject) jsonObject.get("thresholds");
            if (thresholds != null) {
                List<String> thresholdsKey = Arrays.asList("worldLocation", "orientation", "velocity", "acceleration", "angularVelocity");
                for (String key : thresholdsKey) {
                    spatialValueThreshold.put(key, (Double) thresholds.get(key));
                }
            }
        } catch (ParseException | NumberFormatException e) {
            throw new TcInconclusive("Invalid configuration file", e);
        }
    }

    public String getSutHostName() {
        return sutHostName;
    }

    public void setSutHostName(String sutHostName) {
        this.sutHostName = sutHostName;
    }

    public Integer getSutPort() {
        return sutPort;
    }

    public void setSutPort(Integer sutPort) {
        this.sutPort = sutPort;
    }

    public Integer getListeningPort() {
        return listeningPort;
    }

    public void setListeningPort(Integer listeningPort) {
        this.listeningPort = listeningPort;
    }
    
    public List<URL> jsonArrayToUrlList(JSONArray jsonArray) throws TcInconclusive {
        List<URL> urls = new ArrayList<>();
        Iterator<?> iter = jsonArray.iterator();
        while (iter.hasNext()) {
            JSONObject element = (JSONObject) iter.next();
            String fileName = (String) element.get("fileName");
            // add FOM file in url array
            try {
                URI uri = (new File(fileName)).toURI();
                urls.add(uri.toURL());
            } catch (MalformedURLException e) {
                throw new TcInconclusive("Could not open the fom file :" + fileName, e);
            }
        }
        return urls;
    }

    public List<URL> getFadUrls() {
        return fadUrls;
    }

    public void setFadUrls(List<URL> fadUrls) {
        this.fadUrls = fadUrls;
    }
    
    public Map<String, Double> getSpatialValueThreshold(){
        return spatialValueThreshold;
    }

    public Integer getWaitingPeriod() {
        return waitingPeriod;
    }

    public void setWaitingPeriod(Integer waitingPeriod) {
        this.waitingPeriod = waitingPeriod;
    }
}
