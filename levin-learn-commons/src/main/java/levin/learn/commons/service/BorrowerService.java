package levin.learn.commons.service;

import java.util.List;

import levin.learn.commons.model.Borrower;

public interface BorrowerService {
	public List<Borrower> findBorrowersByName(String name);
	public Borrower findBorrowerById(String borrowerId);
	public List<Borrower> findAllBorrowers();
}
