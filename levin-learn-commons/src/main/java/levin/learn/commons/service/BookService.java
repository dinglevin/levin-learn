package levin.learn.commons.service;

import java.util.List;

import levin.learn.commons.model.Book;

public interface BookService {
	public List<Book> findBooksByName(String name);
	public Book findBookByIsbn(String isbn);
	public List<Book> findAllBooks();
}
