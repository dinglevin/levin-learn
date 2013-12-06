package levin.learn.commons.utils;

import java.util.List;

import levin.learn.commons.model.Book;

public class StaticBookFactory {
	public Book createUMLDistilled() {
		return new Book("UMD Distilled", "9780201657838", 34.99, "pudong");
	}
	
	public Book createOxfordDict() {
		return new Book("OXFORD Dictionary", "7-100-04343-3", 69.00, "pudong");
	}
	
	public Book createDerivatives() {
		return new Book("Options, Futures and Other Derivatives", "978-7-111-25437-9", 78.00, "pudong");
	}
	
	public void init(List<Book> books) {
		books.add(createUMLDistilled());
		books.add(createOxfordDict());
		books.add(createDerivatives());
	}
}
