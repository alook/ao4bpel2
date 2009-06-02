/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ode.bpel.iapi;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;

import org.apache.ode.bpel.evt.BpelEvent;
import org.apache.ode.bpel.rapi.ProcessModel;
import org.apache.ode.utils.CronExpression;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Deployed process configuration. IMPORTANT: Implementations of this class <em>MUST BE IMMUTABLE</em>,
 * otherwise the engine will get confused.  
 */
public interface ProcessConf {

    /**
     * Get the process id, generally the same as the type.
     * @return process id.
     */
    QName getProcessId();
    
    /**
     * Get the process type (BPEL definition name).
     * @return
     */
    QName getType();

    /**
     * Get the process version.
     * @return version
     */
    long getVersion();

    /**
     * Is this a <em>transient</em> process? Transient processes are not persisted in the store.
     * @return <code>true</code> if this is a transient process.
     */
    boolean isTransient();

    /**
     * Indicates whether this process implements REST-style resources.
     */
    boolean isRestful();

    /**
     * Get the CBP stream. 
     * @return new stream to the CBP file.
     */
    InputStream getCBPInputStream();

    /**
     * Returns the process model is it's already been loaded. Implementations of ProcessConf aren't required to
     * implement this method, it's only present for optimization purposes. Users should default to getCBPInputStream()
     * when this method isn't supported.
     * @throw UnsupportedOperationException
     */
    ProcessModel getProcessModel();

    /**
     * Get the path of the BPEL document, relative to its deployment unit 
     * @return Relative path of BPEL document
     */
    String getBpelDocument();
   
    /**
     * Get the base URL for resolving resources.  
     */
    URI getBaseURI();
    
    /**
     * Get the date of deployment.
     * @return
     */
    Date getDeployDate();

    /**
     * Get the state of the process. 
     * @return process state.
     */
    ProcessState getState();
    
    /**
     * Get the files associated with the deployment.
     * @return
     */
    List<File> getFiles();

    /**
     * Get the process properties. 
     * @return
     */
    Map<QName, Node> getProcessProperties();

    /**
     * Gets the name of the package into which the process is deployed.
     * @return package name
     */
    String getPackage();
    
    /**
     * Gets the WSDL definition used in a process into which a service is defined.
     * @param serviceName
     * @return definition
     */
    Definition getDefinitionForService(QName serviceName);

    /**
     * Gets the list of endpoints a process should provide.
     * @return map of partner link names and associated enpoints
     */
    Map<String, Endpoint> getProvideEndpoints();

    /**
     * Gets the list of endpoints a process invokes.
     * @return map of partner link names and associated enpoints
     */
    Map<String, Endpoint> getInvokeEndpoints();
    
    /**
     * Tells if the service is shareable
     * @param serviceName
     * @return true if the given service can be shared by processes
     */
    boolean isSharedService(QName serviceName);
    
    /**
     * Generic facility to get additional stuff out of the process descriptor. 
     * @param qname name of the extension element.
     * @return list of extension elements 
     */
    List<Element> getExtensionElement(QName qname);

    boolean isEventEnabled(List<String> scopeNames, BpelEvent.TYPE type);

    /**
     * Returns a list of properties associtated to this endpoint.
     * @param epr
     * @return map of property/value pairs
     */
    public Map<String, String> getEndpointProperties(EndpointReference epr);

    boolean isCleanupCategoryEnabled(boolean instanceSucceeded, CLEANUP_CATEGORY category);
    
    Set<CLEANUP_CATEGORY> getCleanupCategories(boolean instanceSucceeded);

    List<CronJob> getCronJobs();

    public enum CLEANUP_CATEGORY {
        INSTANCE,
        VARIABLES,
        MESSAGES,
        CORRELATIONS,
        EVENTS;
        
        public static CLEANUP_CATEGORY fromString(String lowerCase) {
            return valueOf(CLEANUP_CATEGORY.class, lowerCase.toUpperCase());
        }
    }

    public class CronJob {
        private CronExpression _cronExpression;
        
        private final List<Map<String,Object>> runnableDetailList = new ArrayList<Map<String,Object>>();
        
        public void setCronExpression(CronExpression _cronExpression) {
            this._cronExpression = _cronExpression;
        }
        
        public CronExpression getCronExpression() {
            return _cronExpression;
        }
        
        public List<Map<String,Object>> getRunnableDetailList() {
            return runnableDetailList;
        }
        
        public String toString() {
            StringBuffer buf = new StringBuffer();
            
            buf.append("Cron[");
            buf.append(_cronExpression);
            buf.append("] ");
            buf.append(runnableDetailList);
            
            return buf.toString();
        }
    }
    
    public class CleanupInfo {
        private List<String> _filters = new ArrayList<String>();
        
        private final Set<CLEANUP_CATEGORY> _categories = EnumSet.noneOf(CLEANUP_CATEGORY.class);
        
        public void setFilters(List<String> filters) {
            _filters = filters;
        }
        
        public List<String> getFilters() {
            return _filters;
        }
        
        public Set<CLEANUP_CATEGORY> getCategories() {
            return _categories;
        }
        
        public String toString() {
            StringBuffer buf = new StringBuffer();
            
            buf.append("CleanupInfo: filters=");
            buf.append(_filters);
            buf.append(", categories=");
            buf.append(_categories);
            
            return buf.toString();
        }
    }
}
