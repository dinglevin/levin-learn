package levin.learn.commons.model;

public class TestBorrowerCreator {
	private TestBookCreator bookCreator = new TestBookCreator();
	
	public Borrower createBorrowLevin() {
		Borrower levin = new Borrower();
		levin.setName("levin");
		levin.setId("1234");
		levin.setAddress("pudong-zhangjiang");
		levin.setEmail("levin@gmail.com");
		levin.borrowBook(bookCreator.createBookUML());
		
		return levin;
	}
}
