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
package org.apache.ode.sax.fsa.bpel_2_0;

import org.apache.ode.bom.api.Variable;
import org.apache.ode.bom.impl.nodes.VariableImpl;
import org.apache.ode.sax.fsa.*;
import org.apache.ode.sax.evt.StartElement;
import org.apache.ode.sax.evt.XmlAttributes;
import org.apache.ode.sax.evt.attspec.FilterSpec;
import org.apache.ode.sax.evt.attspec.OrSpec;
import org.apache.ode.sax.evt.attspec.XmlAttributeSpec;


class BpelVariableState extends BaseBpelState {

  private static final StateFactory _factory = new Factory();
  private VariableImpl _v;
  
  private static final XmlAttributeSpec MESSAGETYPE = new FilterSpec(
      new String[] {"name","messageType"},
      new String[] {});
  private static final XmlAttributeSpec ELEMENTTYPE = new FilterSpec(
      new String[] {"name","element"},
      new String[] {});
  private static final XmlAttributeSpec TYPE = new FilterSpec(
      new String[] {"name","type"},
      new String[] {});

  private static final XmlAttributeSpec VALID = new OrSpec(MESSAGETYPE,
      new OrSpec(TYPE,ELEMENTTYPE));
  
  BpelVariableState(StartElement se, ParseContext pc) throws ParseException {
    super(pc);
    XmlAttributes atts = se.getAttributes();
    if (!VALID.matches(atts)){
      getParseContext().parseError(ParseError.ERROR,se,"",
          "Invalid attributes on variable declaration.");
    }
    _v = new VariableImpl();
    _v.setNamespaceContext(se.getNamespaceContext());
    _v.setLineNo(se.getLocation().getLineNumber());
    _v.setName(atts.getValue("name"));
    if (MESSAGETYPE.matches(atts)) {
      _v.setMessageType(se.getNamespaceContext().derefQName(atts.getValue("messageType")));
    } else if (TYPE.matches(atts)) {
      _v.setSchemaType(se.getNamespaceContext().derefQName(atts.getValue("type")));
    } else if (ELEMENTTYPE.matches(atts)) {
      _v.setElementType(se.getNamespaceContext().derefQName(atts.getValue("element")));
    }
  }
  
  public Variable getVariable() {
    return _v;
  }
  
  /**
   * @see org.apache.ode.sax.fsa.State#getFactory()
   */
  public StateFactory getFactory() {
    return _factory;
  }

  /**
   * @see org.apache.ode.sax.fsa.State#getType()
   */
  public int getType() {
    return BPEL_VARIABLE;
  }
  
  static class Factory implements StateFactory {
    
    public State newInstance(StartElement se, ParseContext pc) throws ParseException {
      return new BpelVariableState(se,pc);
    }
  }
}
