package de.tud.stg.ao4ode.runtime.facts;

import org.apache.ode.bpel.o.OBase;

import de.tud.stg.ao4ode.facts.ReadVariableFact;

/**
 * @author A. Look
 */
public class ODEReadVariableFact extends ODEDynamicFact implements ReadVariableFact {

	private static final long serialVersionUID = 5477212048248084442L;
	private OBase src;
	private String varName;

	public ODEReadVariableFact(OBase src, String varName) {
		super(src);
		this.src = src;
		this.varName = varName;
	}

	public String getVarName() {
		return varName;
	}

	public String getXPath() {
		return src.getXPath();
	}

}
