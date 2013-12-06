package levin.learn.commons.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class Borrower implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String id;
	private String name;
	private String address;
	private String email;
	private List<Book> borrowedBooks;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public List<Book> getBorrowedBooks() {
		ensureBorrowedBooksNotEmpty();
		return Collections.unmodifiableList(borrowedBooks);
	}
	
	public void borrowBook(Book book) {
		ensureBorrowedBooksNotEmpty();
		borrowedBooks.add(book);
	}
	
	public void borrowBooks(Collection<Book> books) {
		ensureBorrowedBooksNotEmpty();
		borrowedBooks.addAll(books);
	}
	
	private void ensureBorrowedBooksNotEmpty() {
		if(borrowedBooks == null) {
			borrowedBooks = Lists.newArrayList();
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(id, name, address, email, borrowedBooks);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Borrower)) {
			return false;
		}
		
		if(this == obj) {
			return true;
		}
		
		Borrower other = (Borrower)obj;
		return Objects.equal(other.id, id) &&
				Objects.equal(other.name, name) &&
				Objects.equal(other.address, address) &&
				Objects.equal(other.email, email) &&
				Objects.equal(other.borrowedBooks, borrowedBooks);
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("id", id)
				.add("name", name)
				.add("address", address)
				.add("email", email)
				.add("borrowedBooks", borrowedBooks)
				.toString();
	}
}
