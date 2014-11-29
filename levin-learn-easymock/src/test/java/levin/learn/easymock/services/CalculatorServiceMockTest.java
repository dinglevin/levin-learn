package levin.learn.easymock.services;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

public class CalculatorServiceMockTest {
	private CalculatorService calculator;
	
	@Before
	public void setup() {
		calculator = EasyMock.createMock(CalculatorService.class);
	}
	
	@Test
	public void testAdd() {
		EasyMock.expect(calculator.add(1, 1)).andReturn(2);
		EasyMock.expect(calculator.add(2, 2)).andReturn(4);
		
		EasyMock.replay(calculator);
		
		Assert.assertEquals(4, calculator.add(2, 2));
		Assert.assertEquals(2, calculator.add(1, 1));
		
		EasyMock.verify(calculator);
	}
}
