/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.sakaiproject.nakamura.site.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.ModificationType;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.sakaiproject.nakamura.site.SiteAuthz;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 *
 * @scr.service interface="org.apache.sling.servlets.post.SlingPostProcessor"
 * @scr.property name="service.vendor" value="The Sakai Foundation"
 * @scr.component immediate="true" label="SitePostProcessor"
 *                description="Post Processor for Site operations" metatype="no"
 * @scr.property name="service.description" value="Post Processes site operations"
 *
 */
public class SitePostProcessor implements SlingPostProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(SitePostProcessor.class);

  /**
   * Check for changes to properties of interest to the authz handler.
   */
  public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception{
    boolean authzHandled = false;
    for (Modification m : changes) {
      try {
        Session s = request.getResourceResolver().adaptTo(Session.class);
        // Avoid a bogus warning when the deleted item is not found.
        if (m.getType().equals(ModificationType.DELETE)) {
          LOGGER.info("Delete node {}", m.getSource());
        } else {
          if (s.itemExists(m.getSource())) {
            Item item = s.getItem(m.getSource());
            if (item != null && item.isNode()) {
              LOGGER.info("Change to node {}", item);
            } else {
              LOGGER.info("Change to property {}", item);
              if (!authzHandled && SiteAuthz.MONITORED_SITE_PROPERTIES.contains(item.getName())) {
                SiteAuthz authz = new SiteAuthz(item.getParent());
                authz.applyAuthzChanges();
                authzHandled = true;  // Only needed once
              }
            }
          } else {
            LOGGER.info("itemExists was false for Modification source " + m.getSource() + ", " + m.getType());
          }
        }
      } catch (RepositoryException ex) {
        LOGGER.warn("Failed to process on post for {} ", m.getSource(), ex);
      }
    }
  }

}
