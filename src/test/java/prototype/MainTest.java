package prototype;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

class MainTest {
	
	//TODO make systematic border tests etc.
	
    LinkedHashMap<String, String> variables = new LinkedHashMap<String,String>();
	WPCalculator mainCalculator = new WPCalculator();
	WPCalculatorView mainView = new WPCalculatorView();
	
	MainTest(){
		variables.put("x", "0");
		variables.put("y", "0");
		mainCalculator.setVariables(variables);
		mainCalculator.linkView(mainView);
		mainView.linkCalculator(mainCalculator);
		mainView.setRestriction(10); //default case
		
	}
	
	@Test
	void testCalculation() {
		assertEquals("-1.0", mainCalculator.calculation("(5-6)"));
		assertEquals("2.0", mainCalculator.calculation("min(2,4)"));
		assertEquals("if(1=1,x+1,x)", mainCalculator.calculation("if(1=1,x+1,x)"));
		assertEquals("20.0", mainCalculator.calculation("if(r(11)=10,20,30)"));
		assertEquals("20.0", mainCalculator.calculation("if(r(-1)=0,20,30)"));
		assertEquals("30.0", mainCalculator.calculation("if(r(4)=10,20,30)"));
	}

	
	@Test
	void testTruncate() {
		
		assert mainCalculator.truncate("#{1}").equals("1.0");
		assert mainCalculator.truncate("#{-1}").equals("0");
		assert mainCalculator.truncate("#{11}").equals("10.0");
		
		assert mainCalculator.truncate("#{x+1}").equals("#{x+1}");
		assert mainCalculator.truncate("#{1+1}").equals("2.0");
		assert mainCalculator.truncate("#{x+#{1+3}}").equals("#{x+4.0}");
		assert mainCalculator.truncate("#{2+#{1+3}}").equals("6.0");
		assert mainCalculator.truncate("#{x+#{y+3}}").equals("#{x+#{y+3}}");
	}
	
	@Test
	void testAssignments() {
		
		assert mainCalculator.calculation(mainCalculator.wp("x=5", "x^2")).equals("25.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5 ; x=10", "x^2")).equals("100.0");	
		assert mainCalculator.calculation(mainCalculator.wp("x=5 ; x=10 ; y=2", "x^2")).equals("100.0");	
		assert mainCalculator.calculation(mainCalculator.wp("x=5 ; y=10", "x^2")).equals("25.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5 ; y=10", "y^2")).equals("100.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5 ; y=10", "x+y")).equals("15.0");
	}
	
	@Test
	void testProbability() {
		
		assert mainCalculator.calculation(mainCalculator.wp("{x=5}[4/5]{x=10}","x^2")).equals("40.0");
		assert mainCalculator.calculation(mainCalculator.wp("{x=5}[1/2]{x=10};{x=3}[1/2]{x=4}","x")).equals("3.5");
		assert mainCalculator.calculation(mainCalculator.wp("x=0;y=0;{skip}[1/2]{x=x+2}","x")).equals("1.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5;{skip}[1/2]{x=x+2}","x")).equals("6.0");
		assert mainCalculator.calculation(mainCalculator.wp("{x=5}[1/2]{x=10};{x=3}[1/2]{x=4};x=6","x")).equals("6.0");
	}
	
	@Test
	void testConditional() {

		assert mainCalculator.calculation(mainCalculator.wp("x=5;if {x<5} {x=x+1} else {x=x-1}", "x^2")).equals("16.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5;if {x<5} {x=x+1} else {x=x-1};x=8", "x^2")).equals("64.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5;{x=3}[1/2]{x=10};if {x<5} {x=x+1} else {x=x-1}", "x")).equals("6.5");
		assert mainCalculator.calculation(mainCalculator.wp("x=5;if {x<5} {x=x+1} else {if{x=5}{x=3}else{x=8}}", "x")).equals("3.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=5;if {x<5} {x=x+1} else {min{x=x+1}{x=3}", "x")).equals("3.0");
	}
	
	@Test
	void testDemonicChoice() {

		assert mainCalculator.calculation(mainCalculator.wp("x=1;min{x=x+1}{x=3}","x")).equals("2.0");
		assert mainCalculator.calculation(mainCalculator.wp("x=3;min{x=x+1}{x=3}","x")).equals("3.0");
	}
	
	@Test
	void testWhile() {	

		mainView.setRestriction(10);
		mainView.setIterationCount(10);

		assertEquals("1.0", mainCalculator.calculation(mainCalculator.wp("c=0;x=1; while(c=1){{x=x+1}[1/2]{c=0}}", "x")));
		assertEquals("1.978515625", mainCalculator.calculation(mainCalculator.wp("c=1;x=1; while(c=1){{x=x+1}[1/2]{c=0}}", "x"))); 
	}
	
	@Test
	void testAllSigmaWhile() {	

		mainView.setRestriction(2);
		mainView.setIterationCount(10);
		mainView.getAllSigmaIteration().setSelected(true);
		mainCalculator.fillAllSigma("xc");

		assertEquals("1.0", mainCalculator.calculation(mainCalculator.wp("c=0;x=1; while(c=1){{x=x+1}[1/2]{c=0}}", "x")));
		assertEquals("1.4921875", mainCalculator.calculation(mainCalculator.wp("c=1;x=1; while(c=1){{x=x+1}[1/2]{c=0}}", "x"))); 
		
		mainView.getDeltaInput().setText("0.001");
		mainView.setIterationCount(100);
		assertEquals("1.4990234375", mainCalculator.calculation(mainCalculator.wp("c=1;x=1; while(c=1){{x=x+1}[1/2]{c=0}}", "x"))); 
	}
	
	@Test
	void testfillAllSigma() {

		mainView.setRestriction(1); //var from {0,1}
		
		ArrayList<LinkedHashMap<String,String>> allSigma = mainCalculator.fillAllSigma("xy");
		
		assert allSigma.get(0).get("x").equals("0");
		assertEquals("0",allSigma.get(0).get("x")); //TODO change all tests to assertEquals notation
		assert allSigma.get(0).get("y").equals("0");
		
		assert allSigma.get(1).get("x").equals("0");
		assert allSigma.get(1).get("y").equals("1");
		
		assert allSigma.get(2).get("x").equals("1");
		assert allSigma.get(2).get("y").equals("0");
		
		assert allSigma.get(3).get("x").equals("1");
		assert allSigma.get(3).get("y").equals("1");
	}
	
	@Test
	void testCalculateConcreteSigma() {
		
		mainView.setRestriction(1); //var from {0,1}
		ArrayList<LinkedHashMap<String,String>> allSigma = mainCalculator.fillAllSigma("xy");

		assertEquals(1.0,mainCalculator.calculateConcreteSigma("if(x=0,1,y)", allSigma.get(0)));
		assertEquals(1.0,mainCalculator.calculateConcreteSigma("if(x=0,1,y)", allSigma.get(1)));
		assertEquals(0.0,mainCalculator.calculateConcreteSigma("if(x=0,1,y)", allSigma.get(2)));
		assertEquals(1.0,mainCalculator.calculateConcreteSigma("if(x=0,1,y)", allSigma.get(3)));
		assertEquals(null,mainCalculator.calculateConcreteSigma("if(x=0,1,z)", allSigma.get(3)));
	}
	
	@Test
	void testFixpointIfConversion() {
		LinkedHashMap<String,Double> sigma = new LinkedHashMap<String,Double>();
		sigma.put("(x=0)&(c=0)", 0.0);
		sigma.put("(x=0)&(c=1)", 1.0);
		sigma.put("(x=1)&(c=0)", 1.0);
		sigma.put("(x=1)&(c=1)", 2.0);

		assertEquals("iff((x=0)&(c=0),0.0;(x=0)&(c=1),1.0;(x=1)&(c=0),1.0;(x=1)&(c=1),2.0)", mainCalculator.fixpointIfConversion(sigma));
	}
}
