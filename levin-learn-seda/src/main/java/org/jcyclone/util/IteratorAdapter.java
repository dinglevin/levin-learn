package org.jcyclone.util;

import java.util.Enumeration;
import java.util.Iterator;

/**
 * This class maps the Enumeration interface onto
 * Iterator so that they can work together.
 *
 * @author Jean Morissette
 */
public class IteratorAdapter implements Iterator {

	Enumeration e;

	public IteratorAdapter(Enumeration e) {
		this.e = e;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public boolean hasNext() {
		return e.hasMoreElements();
	}

	public Object next() {
		return e.nextElement();
	}
}