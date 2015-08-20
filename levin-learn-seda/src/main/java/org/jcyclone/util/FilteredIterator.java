package org.jcyclone.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class skip over elements that are not accepted
 * from the specified Iterator.
 *
 * @author Jean Morissette
 */
public abstract class FilteredIterator implements Iterator {

	final Iterator iterator;
	Object nextElement = null;

	public FilteredIterator(Iterator iterator) {
		this.iterator = iterator;
	}

	public boolean hasNext() {
		// If we are at the end of the list, there can't be
		// any more elements to iterate through.
		if (!iterator.hasNext() && nextElement == null) {
			return false;
		}
		// Otherwise, see if nextElement is null.
		// If so, try to load the next element to make sure it exists.
		if (nextElement == null) {
			nextElement = getNextElement();
			if (nextElement == null) {
				return false;
			}
		}
		return true;
	}

	public Object next() throws NoSuchElementException {
		Object element = null;
		if (nextElement != null) {
			element = nextElement;
			nextElement = null;
		} else {
			element = getNextElement();
			if (element == null) {
				throw new NoSuchElementException();
			}
		}
		return element;
	}

	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	private Object getNextElement() {
		while (iterator.hasNext()) {
			Object element = iterator.next();
			if (element != null) {
				if (accept(element)) {
					return element;
				}
			}
		}
		return null;
	}

	protected abstract boolean accept(Object obj);

}
