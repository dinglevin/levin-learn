package levin.learn.commons.model;

import java.io.Serializable;

import com.google.common.base.Objects;

public class Book implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String isbn;
	private double price;
	private String location;
	
	public Book() {
	}
	
	public Book(String name, String isbn, double price, String location) {
		this.name = name;
		this.isbn = isbn;
		this.price = price;
		this.location = location;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getIsbn() {
		return isbn;
	}
	public void setIsbn(String isbn) {
		this.isbn = isbn;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(name, isbn, price, location);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Book)) {
			return false;
		}
		
		if(this == obj) {
			return true;
		}
		
		Book other = (Book)obj;
		return Objects.equal(other.name, name) &&
				Objects.equal(other.isbn, isbn) &&
				Objects.equal(other.price, price) &&
				Objects.equal(other.location, location);
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("name", name)
				.add("isbn", isbn)
				.add("price", price)
				.add("location", location)
				.toString();
	}
}
