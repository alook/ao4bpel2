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
package org.apache.ode.bom.impl.nodes;

import org.apache.ode.bom.api.Correlation;
import org.apache.ode.utils.stl.CollectionsX;
import org.apache.ode.utils.stl.MemberOfFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for managing corrations.
 */
class CorrelationHelperImpl {
  private final ArrayList<Correlation> _correlations = new ArrayList<Correlation>();

  public void addCorrelation(Correlation correlation) {
    _correlations.add(correlation);
  }

  public List<Correlation> getCorrelations(final short patternMask) {
    List<Correlation> retVal = new ArrayList<Correlation>(_correlations);
    CollectionsX.remove_if(retVal, new MemberOfFunction<Correlation>() {
      public boolean isMember(Correlation c) {
        return ((c.getPattern() & patternMask) == 0);
      }
    });
    return retVal;
  }

  public List<Correlation> getCorrelations() {
    return _correlations;
  }

}
