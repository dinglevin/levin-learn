package levin.learn.commons.service.impl;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import levin.learn.commons.model.Book;
import levin.learn.commons.service.BookService;
import levin.learn.commons.utils.StaticBookFactory;

public class BookServiceImpl implements BookService {
	private List<Book> books;
	
	public BookServiceImpl() {
		this.books = Lists.newArrayList();
		
		new StaticBookFactory().init(books);
	}

	public List<Book> findBooksByName(String name) {
		List<Book> sameNameBooks = Lists.newArrayList();
		for(Book book : books) {
			if(book.getName().equalsIgnoreCase(name)) {
				sameNameBooks.add(book);
			}
		}
		return sameNameBooks;
	}

	public Book findBookByIsbn(String isbn) {
		for(Book book : books) {
			if(book.getIsbn().equals(isbn)) {
				return book;
			}
		}
		return null;
	}

	public List<Book> findAllBooks() {
		return Collections.unmodifiableList(books);
	}
}
