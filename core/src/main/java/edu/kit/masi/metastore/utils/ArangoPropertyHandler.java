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
package edu.kit.masi.metastore.utils;

import java.util.Properties;

/**
 * Data object holding all properties for accessing arangodb.
 * @author vaibhav
 */
public class ArangoPropertyHandler {
  /**
   * Collection name.
   */
	private String collectionName;
  /**
   * Database name.
   */
	private String dbName;
  /** 
   * User name.
   */
	private String username;
  /**
   * Password
   */
	private String password;
  /** 
   * Connection URL
   */
	private String url;
  /**
   * Port number.
   */
	private String port;
  /** 
   * Flag for testing purposes.
   */
	private boolean dbDelete;
  /**
   * Get port number.
   * @return Port number.
   */
	public String getPort() {
		return port;
	}
  /**
   * Get port number as int value.
   * @return Port number.
   */
	public int getIntPort() {
		return Integer.parseInt(port);
	}
   /**
    * Set port number.
    * @param port Port number.
    */
	private void setPort(String port) {
		this.port = port;
	}
  /**
   * Get collection name.
   * @return Collection name.
   */
	public String getCollectionName() {
		return collectionName;
	}
  /**
   * Set collection name.
   * @param collectionName Collection name. 
   */
	private void setCollectionName(String collectionName) {
		this.collectionName = collectionName;
	}
  /**
   * Get database name.
   * @return Database name.
   */
	public String getDbName() {
		return dbName;
	}
  /** 
   * Set database name.
   * @param dbName Database name.
   */
	private void setDbName(String dbName) {
		this.dbName = dbName;
	}
  /**
   * Get password.
   * @return Password.
   */
	public String getPassword() {
		return password;
	}
  /**
   * Set password.
   * @param password  Password.
   */
	private void setPassword(String password) {
		this.password = password;
	}
  /**
   * Get connection URL.
   * @return Connection URL.
   */
	public String getUrl() {
		return url;
	}
  /**
   * Set connection URL.
   * @param url Connection URL
   */
	private void setUrl(String url) {
		this.url = url;
	}
  /** 
   * Get user name.
   * @return User name.
   */
	public String getUsername() {
		return username;
	}
  /**
   * Set user name.
   * @param username User name. 
   */
	private void setUsername(String username) {
		this.username = username;
	}
  /** 
   * Set database delete flag.
   * If set to true all documents were dropped.
   * @return Database delete flag.
   */
	public boolean getDbDelete() {
		return dbDelete;
	}
  /**
   * Set database delete flag.
   * @param dbDelete Database delete flag.
   */
	private void setDbDelete(boolean dbDelete) {
		this.dbDelete = dbDelete;
	}
  /** 
   * Load all properties from file.
   */
	public void loadProperty() {
		try {
			Properties prop = new Properties();
			prop.load(getClass().getResourceAsStream("/DatabaseProperties.properties"));
			setCollectionName(prop.getProperty("collections"));
			setDbName(prop.getProperty("arangoDbName"));
			setUsername(prop.getProperty("username"));
			setPassword(prop.getProperty("password"));
			setUrl(prop.getProperty("arangoIP"));
			setPort(prop.getProperty("arangoPort"));
			setDbDelete(prop.getProperty("dbDelete").equalsIgnoreCase("true")?true:false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
