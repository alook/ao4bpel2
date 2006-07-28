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

/*
 * Licensed under the X license (see http://www.x.org/terms.htm)
 */
package org.opentools.minerva.xml;

import java.lang.reflect.*;
import java.util.*;
import org.w3c.dom.*;

/**
 * Utility functions for parsing XML (DOM).
 *
 * @author Aaron Mulder (ammulder@alumni.princeton.edu)
 */
@SuppressWarnings("unchecked")
public abstract class XMLUtils {
    public static String getChildText(Element parent, String name) {
        Element e = getChildByName(parent, name);
        if(e == null)
            return "";
        return getText(e);
    }

    public static String getText(Element e) {
        NodeList nl = e.getChildNodes();
        int max = nl.getLength();
        for(int i=0; i<max; i++) {
            Node n = nl.item(i);
            if(n.getNodeType() == Node.TEXT_NODE) {
                return n.getNodeValue();
            }
        }
        return "";
    }

    public static Element getChildByName(Element e, String name) {
        Element[] list = getChildrenByName(e, name);
        if(list.length == 0)
            return null;
        if(list.length > 1)
            throw new IllegalStateException("Too many ("+list.length+") '"+name+"' elements found!");
        return list[0];
    }

    public static Element[] getChildrenByName(Element e, String name) {
        NodeList nl = e.getChildNodes();
        int max = nl.getLength();
        LinkedList list = new LinkedList();
        for(int i=0; i<max; i++) {
            Node n = nl.item(i);
            if(n.getNodeType() == Node.ELEMENT_NODE &&
               n.getNodeName().equals(name)) {
                list.add(n);
            }
        }
        return (Element[])list.toArray(new Element[list.size()]);
    }

    public static String[] splitOnWhitespace(String source) {
        int pos = -1;
        LinkedList list = new LinkedList();
        int max = source.length();
        for(int i=0; i<max; i++) {
            char c = source.charAt(i);
            if(Character.isWhitespace(c)) {
                if(i-pos > 1)
                    list.add(source.substring(pos+1, i));
                pos = i;
            }
        }
        return (String[])list.toArray(new String[list.size()]);
    }

    public static Element createChild(Document doc, Element root, String name) {
        Element elem = doc.createElement(name);
        root.appendChild(elem);
        return elem;
    }

    public static void createChildText(Document doc, Element elem, String name, String value) {
        Element child = doc.createElement(name);
        child.appendChild(doc.createTextNode(value == null ? "" : value));
        elem.appendChild(child);
    }

    public static void createOptionalChildText(Document doc, Element elem, String name, String value) {
        if(value == null || value.length() == 0)
            return;
        Element child = doc.createElement(name);
        child.appendChild(doc.createTextNode(value));
        elem.appendChild(child);
    }

    public static Map getProperties(Element root) {
        Map map = new HashMap();
        Element[] list = getChildrenByName(root, "property");
        for(int i=0; i<list.length; i++) {
            String name = list[i].getAttribute("name");
            String type = list[i].getAttribute("type");
            String valueString = getText(list[i]);
            try {
                Class cls = Class.forName(type);
                Constructor con = cls.getConstructor(new Class[]{String.class});
                Object value = con.newInstance(new Object[]{valueString});
                map.put(name, value);
            } catch(Exception e) {
                throw new RuntimeException("Unable to parse property '" + name + "'='" + valueString + "': ", e);
            }
        }
        return map;
    }

    public static void applyProperties(Object o, Element root) {
        Map map = getProperties(root);
        Iterator it = map.keySet().iterator();
        Field[] fields = o.getClass().getFields();
        Method[] methods = o.getClass().getMethods();
        while(it.hasNext()) {
            String name = (String)it.next();
            Object value = map.get(name);
            try {
                for(int i=0; i<fields.length; i++) {
                    if(fields[i].getName().equalsIgnoreCase(name) && isTypeMatch(fields[i].getType(), value.getClass())) {
                        fields[i].set(o, value);
                        break;
                    }
                }
                for(int i=0; i<methods.length; i++) {
                    if(methods[i].getName().equalsIgnoreCase("set"+name) && methods[i].getParameterTypes().length == 1 && isTypeMatch(methods[i].getParameterTypes()[0], value.getClass())) {
                        methods[i].invoke(o, new Object[]{value});
                        break;
                    }
                }
            } catch(Exception e) {
                System.out.println("Unable to apply property '"+name+"': "+e);
            }
        }
    }

    private static boolean isTypeMatch(Class one, Class two) {
        if(one.equals(two))
            return true;
        if(one.isPrimitive()) {
            if(one.getName().equals("int") && two.getName().equals("java.lang.Integer"))
                return true;
            if(one.getName().equals("long") && two.getName().equals("java.lang.Long"))
                return true;
            if(one.getName().equals("float") && two.getName().equals("java.lang.Float"))
                return true;
            if(one.getName().equals("double") && two.getName().equals("java.lang.Double"))
                return true;
            if(one.getName().equals("char") && two.getName().equals("java.lang.Character"))
                return true;
            if(one.getName().equals("byte") && two.getName().equals("java.lang.Byte"))
                return true;
            if(one.getName().equals("short") && two.getName().equals("java.lang.Short"))
                return true;
            if(one.getName().equals("boolean") && two.getName().equals("java.lang.Boolean"))
                return true;
        }
        return false;
    }
}
