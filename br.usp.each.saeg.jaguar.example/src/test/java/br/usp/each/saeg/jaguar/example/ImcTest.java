package br.usp.each.saeg.jaguar.example;

import org.junit.Assert;
import org.junit.Test;

import br.usp.each.saeg.jaguar.example.Avg;

/**
 * Unit test for Max.
 */
public class ImcTest {
	@Test
	public void test1() {
		int expected = 0;
		int actual = IMC.imc(55.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
	@Test
	public void test2() {
		int expected = 1;
		int actual = IMC.imc(75.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
	@Test
	public void test3() {
		int expected = 2;
		int actual = IMC.imc(90.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
	@Test
	public void test4() {
		int expected = 3;
		int actual = IMC.imc(100.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
	@Test
	public void test5() {
		int expected = 4;
		int actual = IMC.imc(110.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
	@Test
	public void test6() {
		int expected = 5;
		int actual = IMC.imc(150.0, 1.75);
		Assert.assertEquals(expected, actual);
	}	
}
