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
package org.apache.ode.bpel.elang.xpath10.runtime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ode.bpel.common.FaultException;
import org.apache.ode.bpel.elang.xpath10.o.OXPath10Expression;
import org.apache.ode.bpel.explang.ConfigurationException;
import org.apache.ode.bpel.explang.EvaluationContext;
import org.apache.ode.bpel.explang.EvaluationException;
import org.apache.ode.bpel.explang.ExpressionLanguageRuntime;
import org.apache.ode.bpel.o.OAdvice;
import org.apache.ode.bpel.o.OExpression;
import org.apache.ode.bpel.o.OMessageVarType;
import org.apache.ode.bpel.o.OScope.Variable;
import org.apache.ode.bpel.runtime.ACTIVITYGUARD;
import org.apache.ode.utils.DOMUtils;
import org.apache.ode.utils.ISO8601DateParser;
import org.apache.ode.utils.xsd.Duration;
import org.apache.ode.utils.xsl.XslTransformHandler;
import org.jaxen.Context;
import org.jaxen.ContextSupport;
import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import de.tud.stg.ao4ode.runtime.aspectmanager.AspectManager;

import javax.xml.transform.TransformerFactory;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XPath 1.0 Expression Language run-time subsytem.
 */
public class XPath10ExpressionRuntime implements ExpressionLanguageRuntime {
    /** Class-level logger. */
    private static final Log __log = LogFactory.getLog(XPath10ExpressionRuntime.class);

    /** Compiled expression cache. */
    private final Map<String, XPath> _compiledExpressions = new HashMap<String, XPath>();

    /** Registered extension functions. */
    private final Map _extensionFunctions = new HashMap();

    public void initialize(Map properties) throws ConfigurationException {
        TransformerFactory trsf = new net.sf.saxon.TransformerFactoryImpl();
        XslTransformHandler.getInstance().setTransformerFactory(trsf);
    }

    public String evaluateAsString(OExpression cexp, EvaluationContext ctx) throws FaultException, EvaluationException {
        try {
            return compile((OXPath10Expression) cexp).stringValueOf(createContext((OXPath10Expression) cexp, ctx));
        } catch (JaxenException e) {
            handleJaxenException(e);
        }
        throw new AssertionError("UNREACHABLE");
    }

    public boolean evaluateAsBoolean(OExpression cexp, EvaluationContext ctx) throws FaultException,
            EvaluationException {
        try {
            return compile((OXPath10Expression) cexp).booleanValueOf(createContext((OXPath10Expression) cexp, ctx));
        } catch (JaxenException e) {
            handleJaxenException(e);
        }
        throw new AssertionError("UNREACHABLE");
    }

    public Number evaluateAsNumber(OExpression cexp, EvaluationContext ctx) throws FaultException, EvaluationException {
        try {
            return compile((OXPath10Expression) cexp).numberValueOf(createContext((OXPath10Expression) cexp, ctx));
        } catch (JaxenException e) {
            handleJaxenException(e);
        }
        throw new AssertionError("UNREACHABLE");
    }

    public List evaluate(OExpression cexp, EvaluationContext ctx) throws FaultException, EvaluationException {
        try {
        	
            XPath compiledXPath = compile((OXPath10Expression) cexp);
            Context context = createContext((OXPath10Expression) cexp, ctx);

            List retVal = compiledXPath.selectNodes(context);

            if ((retVal.size() == 1) && !(retVal.get(0) instanceof Node)) {
                Document d = DOMUtils.newDocument();
                // Giving our node a parent just in case it's an LValue
                // expression
                Element wrapper = d.createElement("wrapper");
                Text text = d.createTextNode(retVal.get(0).toString());
                wrapper.appendChild(text);
                d.appendChild(wrapper);
                retVal = Collections.singletonList(text);
            }

            return retVal;

        } catch (JaxenException je) {
            handleJaxenException(je);
        }
        throw new AssertionError("UNREACHABLE");
    }

    public Node evaluateNode(OExpression cexp, EvaluationContext ctx) throws FaultException, EvaluationException {
        List retVal = evaluate(cexp, ctx);
        if (retVal.size() == 0)
            throw new FaultException(cexp.getOwner().constants.qnSelectionFailure, "No results for expression: " + cexp);
        if (retVal.size() > 1)
            throw new FaultException(cexp.getOwner().constants.qnSelectionFailure, "Multiple results for expression: "
                    + cexp);
        return (Node) retVal.get(0);
    }

    public Calendar evaluateAsDate(OExpression cexp, EvaluationContext context) throws FaultException,
            EvaluationException {

        String literal = evaluateAsString(cexp, context);
        try {
            return ISO8601DateParser.parseCal(literal);
        } catch (Exception ex) {
            String errmsg = "Invalid date: " + literal;
            __log.error(errmsg, ex);
            throw new FaultException(cexp.getOwner().constants.qnInvalidExpressionValue, errmsg);
        }
    }

    public Duration evaluateAsDuration(OExpression cexp, EvaluationContext context) throws FaultException,
            EvaluationException {
        String literal = this.evaluateAsString(cexp, context);
        try {
            Duration duration = new org.apache.ode.utils.xsd.Duration(literal);
            return duration;
        } catch (Exception ex) {
            String errmsg = "Invalid duration: " + literal;
            __log.error(errmsg, ex);
            throw new FaultException(cexp.getOwner().constants.qnInvalidExpressionValue, errmsg);
        }
    }

    // AO4ODE: Replace ThisJP* in expressions
    private void replaceVariableName(String oldName, String newName, OXPath10Expression expression) {
    	Variable jpvar = expression.vars.get(oldName);
    	expression.vars.put(newName, jpvar);    		
    	expression.xpath = expression.xpath.replaceAll(oldName, newName);
    }
    
    private Context createContext(OXPath10Expression oxpath, EvaluationContext ctx) {
    	
    	// AO4ODE: TODO: Replace ThisJP* in xpath expressions with real names
    	if(oxpath.getOwner() instanceof OAdvice) {
    		AspectManager am = AspectManager.getInstance();
    		ACTIVITYGUARD ag = am.getJPActivity(ctx.getProcessId());
    		OAdvice oadvice = (OAdvice)oxpath.getOwner();
    	
    		// AO4ODE: TODO: Xpath for ThisJP
    		if(oxpath.xpath.contains("ThisJP(")) {
    			Pattern thisProcessPattern = Pattern.compile("ThisJP\\((.*?)\\)");
    			Matcher m = thisProcessPattern.matcher(oxpath.xpath);
    			while (m.find()) {
    			    String name = m.group(1);
    			    replaceVariableName("ThisJP(" + name + ")", name, oxpath);
    			}
    		}
    		if(((OAdvice)oxpath.getOwner()).getOutputVar() != null
    			&& (oxpath.xpath.contains("ThisJPOutVariable"))) {
    			replaceVariableName("ThisJPOutVariable", oadvice.getOutputVar().name, oxpath);
    		}
    		if(((OAdvice)oxpath.getOwner()).getInputVar() != null
        		&& (oxpath.xpath.contains("ThisJPInVariable"))) {
        		replaceVariableName("ThisJPInVariable", oadvice.getInputVar().name, oxpath);
    		}    		
    	}
    	
    	JaxenContexts bpelSupport = new JaxenContexts(oxpath, _extensionFunctions, ctx);
        ContextSupport support = new ContextSupport(new JaxenNamespaceContextAdapter(oxpath.namespaceCtx), bpelSupport,
                bpelSupport, new BpelDocumentNavigator(ctx.getRootNode()));
        Context jctx = new Context(support);

        if (ctx.getRootNode() != null)
            jctx.setNodeSet(Collections.singletonList(ctx.getRootNode()));

        return jctx;
    }

    private XPath compile(OXPath10Expression exp) throws JaxenException {
        XPath xpath = _compiledExpressions.get(exp.xpath);
        if (xpath == null) {
            xpath = new DOMXPath(exp.xpath);
            synchronized (_compiledExpressions) {
                _compiledExpressions.put(exp.xpath, xpath);
            }
        }
        return xpath;
    }

    private void handleJaxenException(JaxenException je) throws EvaluationException, FaultException {
        if (je instanceof WrappedFaultException) {
            throw ((WrappedFaultException) je).getFaultException();
        } else if (je.getCause() instanceof WrappedFaultException) {
            throw ((WrappedFaultException) je.getCause()).getFaultException();
        } else {
            throw new EvaluationException(je.getMessage(), je);
        }

    }
}
