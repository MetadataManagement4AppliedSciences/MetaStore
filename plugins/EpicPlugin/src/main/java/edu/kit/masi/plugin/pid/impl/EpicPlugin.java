/*
 * Copyright 2017 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.masi.plugin.pid.impl;

import com.sun.jersey.api.client.ClientResponse;
import edu.kit.masi.plugin.AbstractServicePlugin;
import java.util.List;
import org.apache.commons.configuration.Configuration;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.kit.masi.plugin.pid.IPidPlugin;
import java.util.logging.Level;

/**
 * Creates a global PID using EPIC plugin. To use a different setup 
 * extend this class by override only the method 'getServiceName()'.
 * @author hartmann-v
 */
public class EpicPlugin extends AbstractServicePlugin implements IPidPlugin {

  /**
   * Logger for debugging purposes.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(EpicPlugin.class);
  /** 
   * Name of the service.
   */
  private static final String SERVICE_NAME = "EPIC";
  /**
   * Key for user name.
   */
  private static final String KEY_USERNAME = "username";
  /**
   * Key for password.
   */
  private static final String KEY_PASSWORD = "password";
  /**
   * Key for base path of landing page.
   */
  private static final String KEY_LANDING_PAGE = "prefixLandingPage";
  /**
   * Key for prefix of institute.
   */
  private static final String KEY_PREFIX = "prefixEpic";
  /**
   * Key for prefix of institution.
   */
  private static final String KEY_INSTITUTE_CODE = "instituteCode";
  /**
   * Key for service url.
   */
  private static final String KEY_SERVICE_URL = "serviceUrl";
  /**
   * Key for prefix of PID url. e.g.: https://hdl.handle.net/
   */
  private static final String KEY_PREFIX_PID_URL = "prefixPIDUrl";
  /**
   * Credential for epic service - username.
   */
  private String userName;
  /**
   * Credential for epic service - password. 
   */
  private String password;
  
  /**
   * Base URL of landing  page.
   */
  private String prefixLandingPage;
  /**
   * Prefix of PID. e.g.: "11022"
   */
  private String prefix;
  /**
   * Institution code for the service. e.g.: "1015"
   */
  private String instituteCode;
          
  /**
   * Service URL. e.g.: "http://hdl.handle.net/"
   * GDWG: "https://pid.gwdg.de/handles/"
   */
  private String serviceUrl;
  /**
   * Prefix of PID URL. 
   * e.g.: "http://hdl.handle.net/"
   */
  private String prefixPidUrl;
  /**
   * Handler for managing PIDs.
   */
  private EpicHandler client;
  

  @Override
  public String getVersion() {
    return "1.0";
  }

  @Override
  public void configureService(Configuration pConfig) {
    userName = pConfig.getString(KEY_USERNAME);
    password = pConfig.getString(KEY_PASSWORD);
    prefixLandingPage = pConfig.getString(KEY_LANDING_PAGE);
    prefix = pConfig.getString(KEY_PREFIX);
    serviceUrl = pConfig.getString(KEY_SERVICE_URL);
    prefixPidUrl = pConfig.getString(KEY_PREFIX_PID_URL);
    instituteCode = pConfig.getString(KEY_INSTITUTE_CODE);
    LOGGER.debug("Configure EPIC: url: {}\nusername: {}, password(length): {}, landingpage: {}, prefix: {}, institution: {}, prefixPidUrl: {}", serviceUrl, userName, password.length(), prefixLandingPage, prefix, instituteCode, prefixPidUrl);
    
    client = new EpicHandler(serviceUrl, instituteCode);
    client.setBasicAuthFilter(userName, password);
  }

  @Override
  public String getServiceName() {
    return SERVICE_NAME;
  }

  @Override
  public String createUniqueIdentifier(String digitalObjectIdentifier) {
    JSONArray ja = client.addAttribute(null, "CREATOR", "KIT Data Manager 1.5");
    String landingPage = prefixLandingPage + digitalObjectIdentifier;
    ja = client.addAttribute(ja, "URL", landingPage);
    String createPid = client.createPid(prefix, ja);
//    //update Url
//    addAttribute(createPid, "URL", landingPage);
    
    LOGGER.debug("Create PID (EPIC):\nPID = {}", createPid);
    
    return prefixPidUrl + createPid;
  }

  @Override
  public void addAttribute(String pPid, String pAttribute, String pValue) {
    String pidOnly = stripPrefixPidURL(pPid);
    LOGGER.debug("addAttribute to '{}': {} = {}",pidOnly, pAttribute, pValue);
    JSONArray jArray = client.addAttribute(client.getPid(pidOnly), pAttribute, pValue);
    ClientResponse updatePid = null;
    int j = 0;
    do {
     if (updatePid != null) {
       try {
         Thread.sleep(100);
       } catch (InterruptedException ex) {
         LOGGER.error(null, ex);
       }
       LOGGER.warn("Sleep for 100 ms because PID is not available yet!");
     } 
     j = j + 1;
     if (j > 30) {
       break;
     }
     updatePid = client.updatePid(pidOnly, jArray);
    } while (updatePid.getStatus() == 404);
    LOGGER.debug("Add attribute to PID: Status: {}: {}", updatePid.getStatusInfo().toString(), updatePid.getStatusInfo().getReasonPhrase());
  }

  @Override
  public List<String> getAttributeValues(String pPid, String pKey) {
    String pidOnly = stripPrefixPidURL(pPid);
    return client.getAttributeValues(pidOnly, pKey);
  }
   
  @Override
  public List<String> getAttributeKeys(String pPid) {
    String pidOnly = stripPrefixPidURL(pPid);
    return client.getAttributeKeys(pidOnly);
  }
   
  @Override
  public void updateAttribute(String pPid, String pAttribute, String pOldValue, String pNewValue) {
    String pidOnly = stripPrefixPidURL(pPid);
    LOGGER.debug("updateAttribute of '{}': {} from {} to {}",pidOnly, pAttribute, pOldValue, pNewValue);
    JSONArray replaceAttribute = client.replaceAttribute(client.getPid(pidOnly), pAttribute, pOldValue, pNewValue);
    ClientResponse updatePid = client.updatePid(pidOnly, replaceAttribute);
    LOGGER.debug("Update PID: Status: {}: {}", updatePid.getStatusInfo().toString(), updatePid.getStatusInfo().getReasonPhrase());
  }

  @Override
  public String getPrefixPidUrl() {
    return prefixPidUrl;
  }

  /** Extract PID from URL.
   * e.g.: http://hdl.handle.net/11022/12345
   * --> 11022/12345
   * @param pPidUrl PID as URL
   * @return only PID without URL.
   */
  private String stripPrefixPidURL(String pPidUrl) {
    String pid = pPidUrl;
    if (pPidUrl.startsWith(prefixPidUrl)) {
      pid = pPidUrl.substring(prefixPidUrl.length());
    }
    return pid;
  }
}
