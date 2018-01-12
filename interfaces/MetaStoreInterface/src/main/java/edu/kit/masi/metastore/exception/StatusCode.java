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
package edu.kit.masi.metastore.exception;

/**
 *  Enumeration holding all HTTP status codes.
 * This class was introduced to separate library from
 * jersey code. 
 * Nevertheless this should be in sync with 
 * javax.ws.rs.core.Response.Status.
 * @author hartmann-v
 */
public enum StatusCode {
  BAD_REQUEST(400),
  NOT_FOUND(404),
  CONFLICT(409),
  INTERNAL_SERVER_ERROR(500),
  SERVICE_UNAVAILABLE(503);
  /** 
   * Status code.
   */
  private final int statusCode;
  /**
   * Constructor
   * @param status status code
   */
  StatusCode(int status) {
    statusCode = status;
  }
  /**
   * Get status code.
   * @return status code.
   */
  public int getStatusCode() {
    return statusCode;
  }
}
