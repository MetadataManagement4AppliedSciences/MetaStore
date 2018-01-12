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
 * Exception wrapping all exceptions fired during execution.
 * 
 * @author hartmann-v
 */
public class MetaStoreException extends Exception {
  /** 
   * Status of service.
   */
  private int httpStatus = StatusCode.INTERNAL_SERVER_ERROR.getStatusCode();
  /**
   * Constructor with default http status code.
   */
  public MetaStoreException() {
  }
  /**
   * Constructor with message and specific http status code.
    * @param status http status code
   */
  public MetaStoreException(int status) {
    super();
    httpStatus = status;
  }
  /**
   * Constructor with message and default http status code.
   * @param message message
   */
  public MetaStoreException(String message) {
    super(message);
  }
  /**
   * Constructor with message and default http status code.
   * @param cause caused by this exception
   */
  public MetaStoreException(Throwable cause) {
    super(cause);
  }
  /**
   * Constructor with message and specific http status code.
   * @param message message
   * @param cause caused by this exception
   */
  public MetaStoreException(String message, Throwable cause) {
    super(message, cause);
  }
  /**
   * Constructor with message and provided http status code.
   * @param message message
   * @param status http status code
   */
  public MetaStoreException(String message, int status) {
    super(message);
   httpStatus = status;
  }
  /**
   * Constructor with message and provided http status code.
   * @param cause caused by this exception
   * @param status http status code
   */
  public MetaStoreException(Throwable cause, int status) {
    super(cause);
   httpStatus = status;
  }
  /**
   * Constructor with message and provided http status code.
   * @param message message
   * @param cause caused by this exception
   * @param status http status code
   */
  public MetaStoreException(String message, Throwable cause, int status) {
    super(message, cause);
   httpStatus = status;
  }
  /**
   * Get http status code.
   * @return http status code.
   */
  public int getHttpStatus() {
    return httpStatus;
  }
}
