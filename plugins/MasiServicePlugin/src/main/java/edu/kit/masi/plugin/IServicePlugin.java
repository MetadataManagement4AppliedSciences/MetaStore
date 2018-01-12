/*
 * Copyright 2016 Karlsruhe Institute of Technology.
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
package edu.kit.masi.plugin;

import org.apache.commons.configuration.Configuration;

/**
 * Each service plugin should support at least the two given attributes to
 * identify and configure them.
 *
 * @author hartmann-v
 */
public interface IServicePlugin {

  /**
   * Name of the service.
   * The name of the service should be the class name of the implementing
   * class.
   * @return name of the service/plugin.
   */
  String getName();

  /**
   * Version of the service.
   * @return version number of the service.
   */
  String getVersion();
  /**
   * Configure service.
   * @param pConfig Configuration of service or null if no exists.
   */
  void configureService(Configuration pConfig);
}
