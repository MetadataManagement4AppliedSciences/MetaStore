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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default base class for all plugins. If it is not possible to inherit from
 * this class please copy 'getName()' method from this class.
 *
 * @author hartmann-v
 */
public abstract class AbstractServicePlugin implements IServicePlugin {

  /**
   * The logger
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractServicePlugin.class);

  @Override
  public final String getName() {
    return getClass().getName();
  }
  
  @Override
  public void configureService(Configuration pConfig) {
    if (pConfig != null) {
      LOGGER.warn("{}:Configuration exists but won't be parsed!", this.getClass().toString());
    }
  }

}
