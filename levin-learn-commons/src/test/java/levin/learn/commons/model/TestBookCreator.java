package levin.learn.commons.model;

public class TestBookCreator {
	public Book createBookUML() {
		Book book = new Book();
		book.setName("UMD Distilled");
		book.setIsbn("9780201657838");
		book.setPrice(34.99);
		book.setLocation("pudong");
		return book;
	}
}
