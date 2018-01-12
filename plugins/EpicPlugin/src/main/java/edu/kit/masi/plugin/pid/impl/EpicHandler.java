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

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.urlconnection.HTTPSProperties;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import edu.kit.dama.rest.base.exceptions.SSLContextException;
import edu.kit.dama.rest.util.RestClientUtils;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author ca7610
 */
public class EpicHandler {

  /**
   * The logger
   */
  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(EpicHandler.class);
  /**
   * Key for PID.
   */
  private static final String EPIC_PID = "epic-pid";
  /**
   * Key for type.
   */
  private static final String KEY_TYPE = "type";
  /**
   * Key for PID.
   */
  private static final String KEY_DATA = "parsed_data";

  // <editor-fold defaultstate="collapsed" desc="resources for query parameters">
  // </editor-fold>
  // <editor-fold defaultstate="collapsed" desc="error messages">
  /**
   * Error message ssl context
   */
  private static final String ERROR_SSL_CONTEXT = "Failed initialize SSL context for REST client";
  /**
   * Error creating REST client
   */
  private static final String ERROR_REST_CLIENT = "Failed to create REST client";
  /**
   * Error null argument.
   */
  private static final String ERROR_ARGUMENT = "Argument '%s' must not be null!";
  // </editor-fold>
  private Client client;
  private WebResource webResource;
  private MultivaluedMap<String, String> queryParams = null;
  private final static X509TrustManager TRUST_MANAGER = new X509TrustManager() {
    @Override
    public X509Certificate[] getAcceptedIssuers() {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] xcs, String string) throws java.security.cert.CertificateException {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] xcs, String string) throws java.security.cert.CertificateException {
    }
  };

  private final static HostnameVerifier VERIFIER = new HostnameVerifier() {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };

  /**
   * Create an instance of a client.
   *
   * @param pServiceUrl The base URL of the REST service.
   */
  protected EpicHandler(String pServiceUrl) {
    this(pServiceUrl, null);
  }

  /**
   * Create an instance of a client.
   *
   * @param pServiceUrl The base URL of the REST service.
   * @param pInstitution The identifier for the institution
   */
  protected EpicHandler(String pServiceUrl, String pInstitution) {

    if (pServiceUrl == null) {
      throw new IllegalArgumentException(String.format(ERROR_ARGUMENT, "pServiceUrl"));
    }

    //client = Client.create();
    LOGGER.debug("Creating client for service URL {} and no context ", pServiceUrl);
    ClientConfig config = new DefaultClientConfig();
    //String authentication = "Basic " + encodeCredentialsBasic("<<User>>", "<<Password>>");

    try {
      LOGGER.debug("Initializing TLS");
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, new TrustManager[]{TRUST_MANAGER}, new SecureRandom());
      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(VERIFIER, ctx));
      client = Client.create(config);
    } catch (NoSuchAlgorithmException | KeyManagementException ex) {
      throw new SSLContextException(ERROR_SSL_CONTEXT, ex);
    }
    LOGGER.debug("Creating service URI resource from URL {}", pServiceUrl);
    //  client = Client.create();
    URI resourceUri = null;
    try {
      resourceUri = new URL(pServiceUrl).toURI();
    } catch (MalformedURLException | URISyntaxException ex) {
      throw new SSLContextException(ERROR_REST_CLIENT, ex);
    }

    // <editor-fold defaultstate="collapsed" desc="Configure institution for queries.">
    queryParams = new MultivaluedMapImpl();
    if (pInstitution != null) {
      queryParams.add("INST", pInstitution);
    }
    // </editor-fold>

    LOGGER.debug("Creating webresource instance for resource URI {}", resourceUri);
    webResource = client.resource(resourceUri);
    // webResource.path("").header(AUTHENTICATION_HEADER, authentication).get(DownloadInformationResult.class);
    LOGGER.debug("Client initialized.");
  }

  /**
   * Create webresource for access via REST.
   *
   * @param path relative path
   * @return new webresource for given (relative) path
   */
  protected final WebResource getWebResource(String path) {
    return webResource.path(path);
  }

  public void setBasicAuthFilter(String pUsername, String pPassword) {
    client.addFilter(new HTTPBasicAuthFilter(pUsername, pPassword));
  }

  /**
   * Update attributes for PID.
   *
   * @param pid PID which should be updated.
   * @param parameter All attributes (also the existing ones.)
   * @return Response of web service.
   */
  public ClientResponse updatePid(String pid, JSONArray parameter) {
    WebResource webResource = RestClientUtils.prepareWebResource(getWebResource(pid), queryParams);
    LOGGER.debug("Update '{}' with '{}'", pid, parameter.toString());
    ClientResponse returnValue = webResource.
            accept(MediaType.APPLICATION_JSON).
            type(MediaType.APPLICATION_JSON).
            put(ClientResponse.class, parameter.toString());
    switch (returnValue.getStatus()) {
      case 201:
        // Error!? PID created instead of an update.
        break;
      case 204:
        // Success
        break;
      case 401:
      // Unauthorized: Your username or your password is wrong
      case 405:
      // Method Not Allowed:
      // You are trying to create a new handle in the main url of the server either (https://epic.grnet.gr/handles/11239/) or (https://epic.grnet.gr/handles). You have not specified a unique name for your handle. (or)
      // You are trying to create a new handle with manual generation of suffix name via POST instead of PUT. POST supports automatic generation of suffix name.
      case 412:
        // Precondition failed: You have used the precondition (HTTP’s If-Match: * or If-None-Match:*) in the request-header fields. The precondition given, evaluated to false when it was tested on the server and prevented the requested method from being applied.
        break;
      case 415:
        // Unsupported Media Type: You haven’t specify the correct headers for your request. The service supports Json representation so you must define the content-type of the request.
        break;
      default:
    }
    return returnValue;
  }

  /**
   * Create new PID.
   *
   * Response: {"epic-pid":"xxxxx/xxxx-xxxx-xxxx-x"}
   *
   * @param pInst Prefix of PID.
   * @param parameter All attributes associated with this PID.
   * @return Response of web service.
   */
  public String createPid(String pInst, JSONArray parameter) {
    String pid = null;

    webResource = RestClientUtils.prepareWebResource(getWebResource(pInst), queryParams);
    ClientResponse returnValue = webResource.
            accept(MediaType.APPLICATION_JSON).
            type(MediaType.APPLICATION_JSON).
            post(ClientResponse.class, parameter.toString());
    switch (returnValue.getStatus()) {
      case 201:
        // success
        String data = null;
        try {
          data = new String(IOUtils.toByteArray(returnValue.getEntityInputStream()));
          LOGGER.trace("createPID response: '{}'", data);
          JSONObject jo = new JSONObject(data);
          pid = jo.getString(EPIC_PID);
        } catch (IOException ex) {
          LOGGER.error("Error while creating PID!", ex);
        }
        break;
      case 204:
        // No-Content: The local name already exists , and instead of creating a new one you’ve just updated the values of an existing one.update instead of creation!?
        // !? Makes no sense for a put
        break;
      case 401:
      // Unauthorized: Your username or your password is wrong
      case 405:
      // Method Not Allowed:
      // You are trying to create a new handle in the main url of the server either (https://epic.grnet.gr/handles/11239/) or (https://epic.grnet.gr/handles). You have not specified a unique name for your handle. (or)
      // You are trying to create a new handle with manual generation of suffix name via POST instead of PUT. POST supports automatic generation of suffix name.
      case 412:
        // Precondition failed: You have used the precondition (HTTP’s If-Match: * or If-None-Match:*) in the request-header fields. The precondition given, evaluated to false when it was tested on the server and prevented the requested method from being applied.
        break;
      case 415:
        // Unsupported Media Type: You haven’t specify the correct headers for your request. The service supports Json representation so you must define the content-type of the request.
        break;
      default:
    }
    return pid;
  }

  /**
   * Get all attributes of given PID.
   *
   * @param pid PID
   * @return All attributes of given PID.
   */
  public JSONArray getPid(String pid) {
    JSONArray attributes = null;
    WebResource webResource2 = RestClientUtils.prepareWebResource(getWebResource(pid), queryParams);
    ClientResponse returnValue = webResource2.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
    switch (returnValue.getStatus()) {
      case 200:
        // success
        String data;
        try {
          data = new String(IOUtils.toByteArray(returnValue.getEntityInputStream()));
          LOGGER.trace("getPID response: '{}'", data);
          attributes = new JSONArray(data);
        } catch (IOException ex) {
          LOGGER.error("Error while creating PID!", ex);
        }
        break;
      case 401:
      // Unauthorized: Your username or your password is wrong
      case 404:
        // URL not found!
        break;
      default:
    }
    return attributes;
  }

  /**
   * Get all attributes values of PID with given key.
   *
   * @param pPid PID as URL
   * @param pKey Key of the attribute.
   * @return All attributes of given PID.
   */
  public List<String> getAttributeValues(String pPid, String pKey) {
    List<String> values = new ArrayList<>();
    JSONArray attributes = getPid(pPid);
    for (int index = 0; index < attributes.length(); index++) {
      JSONObject jo = attributes.optJSONObject(index);
      if (jo != null) {
        if (jo.get(KEY_TYPE).equals(pKey)) {
          values.add(jo.get(KEY_DATA).toString());
        }
      }
    }
    return values;
  }

  /**
   * Get keys of all attributes of PID.
   *
   * @param pPid PID as URL
   * @return All attributes of given PID.
   */
  public List<String> getAttributeKeys(String pPid) {
    List<String> values = new ArrayList<>();
    JSONArray attributes = getPid(pPid);
    for (int index = 0; index < attributes.length(); index++) {
      JSONObject jo = attributes.optJSONObject(index);
      if (jo != null) {
        values.add(jo.get(KEY_TYPE).toString());
      }
    }
    return values;
  }

  public static void main(String[] args) {

    //HTTPBasicAuthFilter baf = new HTTPBasicAuthFilter("1015-01", "Heedah2a");
    EpicHandler client = new EpicHandler("https://pid.gwdg.de/handles/");
    client.setBasicAuthFilter("1015-01", "Heedah2a");

    JSONArray addAttribute = client.addAttribute(null, "URL", "http://landingpage.de");
    client.addAttribute(addAttribute, "author", "Volker Hartmann");
    String createPid = client.createPid("11022", addAttribute);
    JSONArray pid = client.getPid(createPid);
    String newUrl = "http://episteme.ipe.kit.edu:8080/KITDM/?landing&oid=" + createPid;
    client.replaceAttribute(pid, "URL", newUrl);
    JSONArray newAttributes = client.addAttribute(null, "URL", newUrl);
    newAttributes = client.addAttribute(newAttributes, "author2", "Thomas Jejkal");
    ClientResponse updatePid = client.updatePid(createPid, newAttributes);
    client.printResult(updatePid);
  }

  /**
   * Add an attribute to an JSON array.
   *
   * @param pJsonArray null to create a new one otherwise add to this array.
   * @param pKey Key of JSONObject which should be added.
   * @param pValue Value of JSONObject which should be added.
   * @return Array with added attributes.
   */
  JSONArray addAttribute(JSONArray pJsonArray, String pKey, String pValue) {
    if (pJsonArray == null) {
      pJsonArray = new JSONArray();
    }
    JSONObject jsonObject = new JSONObject();
    //"type":"URL"
    jsonObject.put(KEY_TYPE, pKey);
    jsonObject.put(KEY_DATA, pValue);
    return pJsonArray.put(jsonObject);
  }

  /**
   * Replace an existing attribute inside a JSON array.
   *
   * @param pJsonArray null to create a new one otherwise add to this array.
   * @param pKey Key of JSONObject which should be added.
   * @param pValue Value of JSONObject which should be added.
   * @return Array with added attributes.
   */
  JSONArray replaceAttribute(JSONArray pJsonArray, String pKey, String pValue) {
    return replaceAttribute(pJsonArray, pKey, null, pValue);
  }

  /**
   * Replace an existing attribute inside an JSON array. If no old value is
   * given the first occurrence will be updated.
   *
   * @param pJsonArray null to create a new one otherwise add to this array.
   * @param pKey Key of JSONObject which should be added.
   * @param pOldValue Value of JSONObject which should be replaced.
   * @param pNewValue Value of JSONObject which should be set.
   * @return Array with added attributes.
   */
  JSONArray replaceAttribute(JSONArray pJsonArray, String pKey, String pOldValue, String pNewValue) {
    boolean replace = false;
    if (pJsonArray == null) {
      pJsonArray = new JSONArray();
    } else {
      for (int index = 0; index < pJsonArray.length(); index++) {
        JSONObject jo = pJsonArray.optJSONObject(index);
        if (jo != null) {
          if (jo.get(KEY_TYPE).equals(pKey)) {
            if (pOldValue != null) {
              if (!jo.get(KEY_DATA).equals(pOldValue)) {
                continue;
              }
            }
            jo.put(KEY_DATA, pNewValue);
            replace = true;
            break;
          }
        }
      }
    }
    if (!replace) {
      pJsonArray = addAttribute(pJsonArray, pKey, pNewValue);
    }
    return pJsonArray;
  }

  /**
   * For testing purposes only.
   *
   * @param pResponse
   */
  private void printResult(ClientResponse pResponse) {
    System.out.println("****************************************************************************");
    System.out.println("**************     RESPONSE              ***********************************");
    System.out.println("****************************************************************************");
    System.out.println("Status: " + pResponse.getStatus());
    String data = null;
    try {
      data = new String(IOUtils.toByteArray(pResponse.getEntityInputStream()));
      System.out.println("****************************************************************************");
      System.out.println(data);
      JSONArray result = new JSONArray(data);
      System.out.println("****************************************************************************");
      System.out.println(result.toString(2));
    } catch (IOException ex) {
      LOGGER.error(null, ex);
    }
  }

}
