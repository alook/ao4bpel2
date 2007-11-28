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
package org.apache.ode.bpel.runtime;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.compiler.bom.Bpel20QNames;
import org.apache.ode.bpel.evt.ScopeEvent;
import org.apache.ode.bpel.evt.VariableModificationEvent;
import org.apache.ode.bpel.o.OActivity;
import org.apache.ode.bpel.o.OScope;
import org.apache.ode.bpel.o.OProcess.OProperty;
import org.apache.ode.bpel.o.OScope.Variable;
import org.apache.ode.bpel.runtime.channels.FaultData;
import org.apache.ode.bpel.runtime.extension.ExtensionContext;
import org.w3c.dom.Node;


/**
 * @author Tammo van Lessen (University of Stuttgart)
 */
public class ExtensionContextImpl implements ExtensionContext {
	private static final Log __log = LogFactory.getLog(ExtensionContextImpl.class);
	
	private BpelRuntimeContext _context;
	private ScopeFrame _scopeFrame;
	private ActivityInfo _activityInfo;
	
	private boolean hasCompleted = false;

	public ExtensionContextImpl(ActivityInfo activityInfo, ScopeFrame scopeFrame, BpelRuntimeContext context) {
		_activityInfo = activityInfo;
		_context = context;
		_scopeFrame = scopeFrame;
	}
	
	public Long getProcessId() {
		return _context.getPid();
	}

	public Map<String, Variable> getVisibleVariables() throws FaultException {
		Map<String, Variable> visVars = new HashMap<String, Variable>();
		
        OActivity current = _scopeFrame.oscope;
        while (current != null) {
            if (current instanceof OScope) {
                for (String varName : ((OScope)current).variables.keySet()) {
                	if (!visVars.containsKey(varName)) {
                		visVars.put(varName, ((OScope)current).variables.get(varName));
                	}
                }
            }
            current = current.getParent();
        }
		
		return visVars;
	}

	public String readMessageProperty(Variable variable, OProperty property)
			throws FaultException {
		VariableInstance vi = _scopeFrame.resolve(variable);
		return _context.readProperty(vi, property);
	}

	public Node readVariable(Variable variable)
			throws FaultException {
		VariableInstance vi = _scopeFrame.resolve(variable);
		return _context.fetchVariableData(vi, true);
	}

	public void writeVariable(String variableName, Node value)
			throws FaultException {
		VariableInstance vi = _scopeFrame.resolve(getVisibleVariable(variableName));
		_context.commitChanges(vi, value);
	}

	public Node readVariable(String variableName) throws FaultException {
		VariableInstance vi = _scopeFrame.resolve(getVisibleVariable(variableName));
		return _context.fetchVariableData(vi, true);
	}

	public void writeVariable(Variable variable, Node value)
			throws FaultException {
		VariableInstance vi = _scopeFrame.resolve(variable);
		_context.commitChanges(vi, value);
        VariableModificationEvent vme = new VariableModificationEvent(variable.name);
        vme.setNewValue(value);
        sendEvent(vme);
	}

	private Variable getVisibleVariable(String varName) {
    	return _scopeFrame.oscope.getVisibleVariable(varName);
    }

	public String getActivityName() {
		return _activityInfo.o.name;
	}

	public OActivity getOActivity() {
		return _activityInfo.o;
	}

	public BpelRuntimeContext getBpelRuntimeContext() {
		return _context;
	}
	
	public void sendEvent(ScopeEvent event) {
        if (event.getLineNo() == -1 && _activityInfo.o.debugInfo != null) {
        	event.setLineNo(_activityInfo.o.debugInfo.startLine);
        }
        _scopeFrame.fillEventInfo(event);
        getBpelRuntimeContext().sendEvent(event);
	}
	
	public void complete() {
		if (!hasCompleted) {
			_activityInfo.parent.completed(null, CompensationHandler.emptySet());
			hasCompleted = true;
		} else {
			if (__log.isWarnEnabled()) {
				__log.warn("Activity '" + _activityInfo.o.name + "' has already been completed.");
			}
		}
	}
	
	public void completeWithFault(Throwable t) {
		if (!hasCompleted) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			FaultData fault = new FaultData(new QName(Bpel20QNames.NS_WSBPEL2_0, "subLanguageExecutionFault"), _activityInfo.o, sw.getBuffer().toString());
	        _activityInfo.parent.completed(fault, CompensationHandler.emptySet());
			hasCompleted = true;
		} else {
			if (__log.isWarnEnabled()) {
				__log.warn("Activity '" + _activityInfo.o.name + "' has already been completed.");
			}
		}
	}
	
	public void completeWithFault(FaultException ex) {
		if (!hasCompleted) {
			FaultData fault = new FaultData(ex.getQName(), _activityInfo.o, ex.getMessage());
			_activityInfo.parent.completed(fault, CompensationHandler.emptySet());
			hasCompleted = true;
		} else {
			if (__log.isWarnEnabled()) {
				__log.warn("Activity '" + _activityInfo.o.name + "' has already been completed.");
			}
		}

	}
}
