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
package edu.kit.masi.rest.service;

import com.sun.jersey.api.core.HttpContext;
import java.util.List;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;

/**
 * Base class for implementing helper methods used by all services.
 *
 * @author hartmann-v
 */
public class BaseService {

  /**
   * Transform message to XML or JSON string.
   * If content type is not MediaType.APPLICATION_XML_TYPE
   * string will be formatted as JSON.
   *
   * @param pMessage exception
   * @param pMediaType Content type of the response.
   * @return Message of exception as JSON or XML
   */
  protected String messageToFormattedString(String  pMessage, MediaType pMediaType) {
    String returnValue;
       if (pMediaType == MediaType.APPLICATION_XML_TYPE) {
        returnValue = messageToXml(pMessage);
      } else {
        returnValue = messageToJson(pMessage);
      }
    return returnValue;
  }
  /**
   * Transform exception message to XML or JSON string.
   * If content type is not MediaType.APPLICATION_XML_TYPE
   * string will be formatted as JSON.
   *
   * @param ex exception
   * @param mediaType Content type of the response.
   * @return Message of exception as JSON or XML
   */
  protected String exceptionToFormattedString(Exception ex, MediaType mediaType) {
    String returnValue;
       if (mediaType == MediaType.APPLICATION_XML_TYPE) {
        returnValue = exceptionToXml(ex);
      } else {
        returnValue = exceptionToJson(ex);
      }
    return returnValue;
  }
  /**
   * Transform exception message to JSON string.
   *
   * @param ex exception
   * @return Message of exception as JSON
   */
  protected String exceptionToJson(Exception ex) {
    return getJsonString("ErrorMsg", ex.getMessage());
  }
  /**
   * Transform message to JSON string.
   *
   * @param pMessage message
   * @return Message of exception as JSON
   */
  protected String messageToJson(String pMessage) {
    return getJsonString("Message", pMessage);
  }
  /**
   * Transform message to JSON string.
   *
   * @param pKey key
   * @param pMessage message
   * @return Message of exception as JSON
   */
  protected String getJsonString(String pKey, String pMessage) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(pKey, pMessage);
    return jsonObject.toString();
  }
  /**
   * Transform exception message to XML string.
   *
   * @param ex exception
   * @return Message of exception as XML
   */
  protected String exceptionToXml(Exception ex) {
    return getXmlString("ErrorMsg", ex.getMessage());
  }
  /**
   * Transform message to XML string.
   *
   * @param pMessage message
   * @return Message of as XML
   */
  protected String messageToXml(String pMessage) {
    return getXmlString("Message", pMessage);
  }
  /**
   * Transform message to XML string.
   *
   * @param pKey key
   * @param pMessage message
   * @return Message of exception as JSON
   */
  protected String getXmlString(String pKey, String pMessage) {
    String xmlValue = StringEscapeUtils.escapeXml10(pMessage);
    String xmlString = String.format("<%s>%s</%s>", pKey, xmlValue, pKey);
    return xmlString;
  }
  /**
   * Determine acceptable media type.
   * @param pContext Context defining all acceptable types.
   * @param possibleType Array of all types supported by service.
   * @return Media type which is selected.
   */
  protected MediaType getAcceptableMediaType(HttpContext pContext, MediaType... possibleType) {
    MediaType selectedType = MediaType.APPLICATION_JSON_TYPE;  // define this as fall back.
    List<MediaType> acceptableMediaTypes = pContext.getRequest().getAcceptableMediaTypes();
    for (MediaType type : possibleType) {
      if (acceptableMediaTypes.contains(type)) {
        selectedType = type;
        break;
      }
    }
    return selectedType;    
  }
}
