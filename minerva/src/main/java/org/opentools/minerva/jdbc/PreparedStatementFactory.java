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
package org.opentools.minerva.jdbc;

import java.sql.*;
import org.opentools.minerva.cache.CachedObjectFactory;

/**
 * Creates PreparedStatements for a PS cache.  Doesn't yet handle
 * different isolation levels, etc.
 *
 * @author Aaron Mulder ammulder@alumni.princeton.edu
 */
public class PreparedStatementFactory extends CachedObjectFactory {
    private Connection con;

    public PreparedStatementFactory(Connection con) {
        this.con = con;
    }

    /**
     * Creates a PreparedStatement from a Connection & SQL String.
     */
    public Object createObject(Object sqlString) {
        PreparedStatementArgs args = (PreparedStatementArgs)sqlString;
        try {
          if(args.autoGeneratedKeys != null)
            return con.prepareStatement(args.sql, args.autoGeneratedKeys.intValue());
          else
            return con.prepareStatement(args.sql);
        } catch(SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Closes a PreparedStatement.
     */
    public void deleteObject(Object pooledObject) {
        try {
            ((PreparedStatement)pooledObject).close();
        } catch(SQLException e) {}
    }
}
