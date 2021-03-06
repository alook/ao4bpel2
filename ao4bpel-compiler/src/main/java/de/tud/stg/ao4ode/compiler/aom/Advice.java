package de.tud.stg.ao4ode.compiler.aom;

import java.util.List;

import org.apache.ode.bpel.compiler.bom.Process;
import org.w3c.dom.Element;

/**
 * @author A. Look
 */
public class Advice extends Process {

	public Advice(Element el) {
		super(el);
	}
	
	public List<Pointcut> getPointcuts() {
		return this.getChildren(Pointcut.class);
	}
	
	public String getAdviceType() {
		return getAttribute("type",null);
	}

}
