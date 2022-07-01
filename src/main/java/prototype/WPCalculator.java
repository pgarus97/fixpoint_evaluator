package prototype;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.Function;

import com.google.common.collect.Lists;
//Parser Documentation: https://github.com/mariuszgromada/MathParser.org-mXparser

public class WPCalculator {

private WPCalculatorView mainView;
private ArrayList<LinkedHashMap<String, String>> allSigma = new ArrayList<LinkedHashMap<String, String>>();
private ArrayList<String> whileLoops = new ArrayList<String>();
private LinkedHashMap<String,String> fixpointCache = new LinkedHashMap<String,String>();



	public String wp(String C, String f) {
		//sequential process
		//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "wp["+C+"]("+f+")"); 
		String C1 = getSequentialCut(C);
		System.out.println("C1: " + C1);
		if(!C1.equals(C)) {
			String C2 = C.substring(C1.length()+1);
			//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Sequential process. Breaking down into: wp["+C1+"](wp["+C2+"]("+f+"))"); 
			return wp(C1,(wp(C2,f)));
		}else {
			if(C.startsWith("min{")) {
				//demonic choice process
				String demC1 = getInsideBracket(C.substring(C.indexOf("{")+1));	
				String demC2 = C.substring(C.indexOf(demC1));
				demC2 = getInsideBracket(demC2.substring(demC2.indexOf("{")+1));
				
				System.out.println("demC1= "+demC1); 
				System.out.println("demC2= "+demC2);
				String resultC1 = wp(demC1,f);
				String resultC2 = wp(demC2,f);

				//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Demonic Choice process. Breaking down into: min(" + resultC1 + "," + resultC2 + ")"); 

				return calculation("min(" + resultC1 + "," + resultC2 + ")");

			}
			
			else if(C.startsWith("if")) {
				//conditional process
				System.out.println("Enter conditional process"); 
				String condition = getInsideBracket(C.substring(C.indexOf("{")+1));
				System.out.println("Conditional: "+condition); 
				String ifC1 = C.substring(condition.length()+4);
				ifC1 = getInsideBracket(ifC1.substring(ifC1.indexOf("{")+1));
				String ifC2 = C.substring(C.indexOf(ifC1));
				ifC2 = getInsideBracket(ifC2.substring(ifC2.indexOf("{")+1));
				
				System.out.println("C1= "+ifC1); 
				System.out.println("C2= "+ifC2);
				String resultC1 = wp(ifC1,f);
				String resultC2 = wp(ifC2,f);
				//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Conditional process. Breaking down into: if("+condition+") then "+ resultC1 +" else "+ resultC2); 
				String condResult = calculation(condition);
				if(condResult.equals("1.0")) {
					return calculation(resultC1);
				}
				if(condResult.equals("0.0")) {
					return calculation(resultC2);
				}
				return calculation("if(" + condition + "," + resultC1 + "," + resultC2 + ")");

			}
			
			else if(C.startsWith("{")){
				//probability process
				System.out.println("Enter probability process"); 
				String probC1 = getInsideBracket(C.substring(C.indexOf("{")+1));

				String probC2 = C.substring(probC1.length());
				String probability = probC2.substring(probC2.indexOf("[")+1,probC2.indexOf("]"));
				probC2 = getInsideBracket(probC2.substring(probC2.indexOf("{")+1));

				System.out.println("C1= "+probC1); 
				System.out.println("C2= "+probC2);
				System.out.println("Probability:" + probability);
				Expression negProbability = new Expression ("1-"+probability);
				String resultC1 = wp(probC1,f);
				String resultC2 = wp(probC2,f);
				//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Probability process. Breaking down into: " + probability + " * " + resultC1 +" + "+ negProbability.calculate() + " * " + resultC2); 
				return calculation("(" + probability + " * "+ resultC1 +" + "+ negProbability.calculate() + " * " + resultC2+")");

			}
			else if(C.startsWith("while")){
				//while process
				//TODO implement sigma forward parsing? then we could check condition before doing fixpoint iteration for performance boost
				System.out.println("Enter while process"); 
				String condition = C.substring(C.indexOf("(")+1,C.indexOf("{")-1);
				System.out.println("Condition: "+condition);
				String whileC = C.substring(condition.length());
				whileC = getInsideBracket(whileC.substring(whileC.indexOf("{")+1));
				System.out.println("whileC: "+whileC);
				
				//TODO need to somehow give into the view C and f separately 
				if(!whileLoops.contains(C+" ("+f+")")) {
					whileLoops.add(C+" ("+f+")");
				}
				//TODO while in while cache 
				if(!fixpointCache.containsKey(C+" ("+f+")")) {
					String fixpoint="";
					if (mainView.getAllSigmaIteration().isSelected()) {		 
						fixpoint = fixpointIterationAllSigma(condition, whileC, f,C);
					} else {
						fixpoint = fixpointIterationIterativ(condition, whileC, f); 
					}
					fixpointCache.put(C+" ("+f+")", fixpoint);
					System.out.println("Put into Cache: "+ C+"("+f+")" + " " + fixpoint);
	
					return fixpoint;
						
				}else {
					System.out.println("Skipped because value has been found in fixpoint cache.");
					System.out.println("Cached LFP: "+ fixpointCache.get(C+" ("+f+")"));
					return fixpointCache.get(C+" ("+f+")");
				}
				
				
			}else {
				//variable assignments
				if(C.contains("skip")){
					System.out.println("Enter skip process"); 
					String skipResult = C.replace("skip", f);
					//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Assignment skip process." + skipResult);
					return calculation(skipResult);
				}else {
					System.out.println("Enter assignment process"); 
					String indexC = C.substring(0,1);
					String cutC = C.substring(C.indexOf("=")+1);
					String assignResult = f.replace(indexC, "r(" + cutC + ")");

					
					//if mid calculation optimization
					if(assignResult.startsWith("if") && !assignResult.startsWith("iff")) {
						System.out.println("Enter conditional process"); 
						String condition = getInsideIf(assignResult.substring(3));
						System.out.println("Conditional: "+condition); 
						String assignifC1 = assignResult.substring(condition.length()+4);	
						System.out.println("ifC1= "+assignifC1); 
						
						assignifC1 =  getInsideIf(assignifC1);
						String assignifC2 = assignResult.substring(condition.length()+4+assignifC1.length()+1);
						assignifC2 = assignifC2.substring(0,assignifC2.length()-1);
						System.out.println("assignifC1= "+assignifC1); 
						System.out.println("assignifC2= "+assignifC2);
						if(calculation(condition).equals("1.0")) {
							return calculation(assignifC1);
						}
						if(calculation(condition).equals("0.0")) {
							return calculation(assignifC2);
						}
					}
					
					//TODO detailed log: mainView.getResult().setText(mainView.getResult().getText() + "\n" + "Assignment process." + assignResult);
					return calculation(assignResult);
				}
					
					
			}
			
		}
	
	}
	
	public String calculation(String exp) {
		Function restrictValue = new Function("r", "min(max(0,x),"+mainView.getRestriction()+")", "x");
		Expression e = new Expression(exp,restrictValue);
		System.out.println("Expression:" + exp);
		Double result = e.calculate();
		if(!result.isNaN()) {
			System.out.println("Calculation Result: " + result);
			return Double.toString(result);
		}else {
			System.out.println("Calculation Result: " + exp);
			return exp;
		}
	}
	
	public String fixpointIterationIterativ(String condition, String C, String f) {
		String caseF = "0"; //X^0 initialization
		for(int i=0; i<mainView.getIterationCount(); i++) {
			String X = wp(C, caseF);
			caseF = "if("+condition+","+X+","+f+")";
		}
		return caseF;
	}
	
	public String fixpointIterationAllSigma(String condition, String C, String f, String whileTerm) {
		LinkedHashMap<String,String> fixpoint = new LinkedHashMap<String, String>();
		
		for(LinkedHashMap<String,String> sigma : allSigma) {
			double sigmaResult = 0.0;
			double previousResult = 0.0;
			String caseF = "0"; //X_0 initialization
			String identifier = "";
			String sigmaCondition = condition;
			for(Map.Entry<String, String> entry : sigma.entrySet()) {
				identifier += "&("+entry.getKey()+"="+entry.getValue()+")"; //creates identifier based on variables and values
				sigmaCondition = sigmaCondition.replace(entry.getKey(), entry.getValue());
			}	
			identifier = identifier.replaceFirst("&","");
			Expression e = new Expression(sigmaCondition);
			if(e.calculate() == 0.0) {
				sigmaResult = calculateConcreteSigma(f,sigma);
			}else {
				for(int i=0; i<mainView.getIterationCount(); i++) {
					String X = wp(C, caseF);
					caseF = "if("+condition+","+X+","+f+")";
					//TODO future improvement: directly input sigma through assignment = f.replace x with sigma x and keep dependency somehow
					sigmaResult = calculateConcreteSigma(caseF,sigma);
					
					if(i > 2) {
						if(sigmaResult-previousResult < Double.parseDouble(mainView.getDeltaInput().getText())) {
							break;
						}else {
							previousResult = sigmaResult;
						}
					}
				}
			}			
			fixpoint.put(identifier, Double.toString(sigmaResult));	
		}
		return fixpointIfConversion(fixpoint);
	}
	
	//TODO write tests
	public void evaluateFixpoint(String currentWhile, String fixpoint, String delta, int iterationCount, LinkedHashSet<String> sigmaSet) {
		
		LinkedHashMap<String,String> Xslash = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> phihashX = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> phihashXslash = new LinkedHashMap<String,String>();
		
		String currentC = currentWhile.split(" ")[0];
		String currentF = currentWhile.split(" ")[1];
		currentF = currentF.substring(1,currentF.length()-1);
		String condition = currentC.substring(currentC.indexOf("(")+1,currentC.indexOf("{")-1);
		String whileC = currentC.substring(condition.length());
		whileC = getInsideBracket(whileC.substring(whileC.indexOf("{")+1));

		//TODO implement check if witness = fixpoint
		LinkedHashMap<String, String> X = fixpointToMap(fixpoint);
		

		
		if(iterationCount == 1) {
			for(Map.Entry<String, String> entry : X.entrySet()) {
				if(!entry.getValue().equals("0") && !entry.getValue().equals("0.0")) {
					sigmaSet.add(entry.getKey());

				}
			}
		}
		
		//save current set for iteration comparison
		LinkedHashSet<String> previousSigmaSet = new LinkedHashSet<String>();
		for(String copiedSigma : sigmaSet) {
			previousSigmaSet.add(copiedSigma);
		}
		
		for(Map.Entry<String, String> entry : X.entrySet()) {
			String XslashValue = "";
			if(!sigmaSet.contains(entry.getKey())) {
				XslashValue = entry.getValue();
				phihashX.put(entry.getKey(), entry.getValue()); //TODO this is wrong I think, needs to be calculated phi hash needs to be calculated on its own
			}else {
				String concreteSigma = entry.getKey().replace("&", ";");
				concreteSigma = concreteSigma.replace("(", "");
				concreteSigma = concreteSigma.replace(")", "");
				XslashValue = calculation("r("+entry.getValue()+"-"+delta+")");
				phihashX.put(entry.getKey(), calculation(wp(concreteSigma+";"+whileC,fixpoint)));
			}
			Xslash.put(entry.getKey(),XslashValue);
		}
		for(Map.Entry<String, String> entry : Xslash.entrySet()) {
			if(entry.getValue().equals("0") || entry.getValue().equals("0.0")) { //TODO this is wrong I think, needs to be calculated
				phihashXslash.put(entry.getKey(), entry.getValue());
			}else {
				String entryF = currentF;
				String entryCondition = condition;
				String concreteSigma = entry.getKey().replace("&", ";");;
				concreteSigma = concreteSigma.replace("(", "");
				concreteSigma = concreteSigma.replace(")", "");
				String[] entryVariables = concreteSigma.split(";");
				for(int i = 0; i < entryVariables.length; i++) {
					String index = entryVariables[i].substring(0,1);
					String cut = entryVariables[i].substring(entryVariables[i].indexOf("=")+1);
					entryF = entryF.replace(index, cut);
					entryCondition = entryCondition.replace(index, cut);
				}
				if(calculation(entryCondition).equals("0.0")) {
					phihashXslash.put(entry.getKey(), calculation(entryF));
				}else {
					phihashXslash.put(entry.getKey(), calculation(wp(concreteSigma+";"+whileC,fixpointIfConversion(Xslash))));
				}
			}
		}
		
		
		for(Map.Entry<String, String> entry : X.entrySet()) {
			double entryResult = Double.parseDouble(calculation(phihashX.get(entry.getKey())+"-"+ phihashXslash.get(entry.getKey()) +">=" +delta));
			if(entryResult == 0.0) {
				sigmaSet.remove(entry.getKey());
			}
		}
		
		//outputting the result
		mainView.getResult().append("\n\n" + "-----------------------------------");
		mainView.getResult().append("\n\n" + "Hash-Function Results: (Iteration " + iterationCount + ")");
		mainView.getResult().append("\n\n" + "X: " + X);
		mainView.getResult().append("\n" + "X': " + Xslash);
		mainView.getResult().append("\n" + "Phi-Hash (X): " + phihashX);
		mainView.getResult().append("\n" + "Phi-Hash (X'): " + phihashXslash);

		if(sigmaSet.isEmpty()) {
			mainView.getResult().append("\n\n" + "The hash-function's result is an empty set. This means the witness is already the least fixpoint." );
		}else {
			mainView.getResult().append("\n\n" + "The hash-function's result is not an empty set. This means the witness is above the least fixpoint." );
			mainView.getResult().append("\n" + "Following states are still in the result set: " );
			for(String state : sigmaSet) {
				mainView.getResult().append(state + ",");
			}
			mainView.getResult().append(" therefore continuing iteration.");
			if(!previousSigmaSet.toString().equals(sigmaSet.toString())) {
				evaluateFixpoint(currentWhile, fixpoint, delta, (iterationCount+1), sigmaSet);
			}
		}

	}
	
	
	public LinkedHashMap<String,String> fixpointToMap(String fixpoint) {
		LinkedHashMap<String,String> convFixpoint = new LinkedHashMap<String,String>();
		fixpoint = fixpoint.substring(4,fixpoint.length()-1);
		fixpoint += ";";
		System.out.println("Fixpoint: "+fixpoint);

		while(fixpoint.length()>0) {
			String identifier = fixpoint.substring(0,fixpoint.indexOf(","));
			System.out.println("Id: "+identifier);
			fixpoint = fixpoint.substring(identifier.length()+1);
			String value = fixpoint.substring(0,fixpoint.indexOf(";"));
			System.out.println("Value: "+value);
			convFixpoint.put(identifier, value);
			fixpoint = fixpoint.substring(value.length()+1);
		}
		
		return convFixpoint;
	}
	
	public String fixpointIfConversion(LinkedHashMap<String,String> fixpoint) {
		String result = "iff(";
		for(Map.Entry<String, String> entry : fixpoint.entrySet()) {
			result += ";" + entry.getKey()+","+entry.getValue();
		}
		result = result.replaceFirst(";", "");
		result += ")";
		return result;
	}
	
	//TODO add other possibility of calculating concrete sigma: wp("sigma=x=1;c=1";caseF,null); = Xi
	public Double calculateConcreteSigma(String f, LinkedHashMap<String,String> sigma) {
		System.out.println("Concrete Sigma f : " + f);
		for(Map.Entry<String, String> entry : sigma.entrySet()) {
			f = f.replace(entry.getKey(), entry.getValue());
		}
		System.out.println("Concrete Sigma f after replace : " + f);

		Function restrictValue = new Function("r", "min(max(0,x),"+mainView.getRestriction()+")", "x");
		Expression e = new Expression(f,restrictValue);

		Double result = e.calculate();
		System.out.println("Result: " + result);

		if(result.isNaN()) {
			//throw exception and break + log
			System.out.println("There are unknown variables in the formula!");
			return null;
		}else {
			return result;
		}
	}
	
	//fills allSigma with all possibilities of variable and value combinations
	public ArrayList<LinkedHashMap<String,String>> fillAllSigma(String varInput) {
		allSigma.clear();
		
		List<List<Integer>> preCartesianValues = new ArrayList<List<Integer>>(); 
		
		//TODO only goes from 1-9 since character
		List<Integer> restrictedList = new ArrayList<Integer>();
		for (int i = 0 ; i < mainView.getRestriction()+1; i++) {
			restrictedList.add(i);
		}
		
		for(int i = 0 ; i < varInput.length() ; i++) {	
			preCartesianValues.add(restrictedList);
		}

		List<List<Integer>> postCartesianValues = Lists.cartesianProduct(preCartesianValues);
		
		for(int i = 0 ; i < postCartesianValues.size(); i++){
			LinkedHashMap<String, String> tempMap = new LinkedHashMap<String,String>();
			for(int j = 0 ; j < postCartesianValues.get(i).size(); j++){
			tempMap.put(String.valueOf(varInput.charAt(j)), postCartesianValues.get(i).get(j).toString());
			}
			allSigma.add(tempMap);
		}
		return allSigma;
	}		
	
	//start with C in one index after first appearance of start char
	public String getInsideBracket(String C) {
		int bracketCount = 1;
		String result = "";
		for(int i = 0; i < C.length(); i++) {
			char character = C.charAt(i);
			if(character == '{') {
				bracketCount++;
			}
			if(character == '}') {
				bracketCount--;
			}
			if(bracketCount != 0) {
				result = result + character;
			}else {
				break;
			}
		}
		return result;
	}
	
	
	public int getIffCount(String term) {
		int bracketCount = 1;
		int commaCount = 0;
		for(int i = 0; i < term.length(); i++) {
			char character = term.charAt(i);
			if(character == '(') {
				bracketCount++;
			}
			if(character == ')') {
				bracketCount--;
			}
			if(character == ',') {
				commaCount++;
			}
			if(bracketCount == 0) {
				break;
			}
			
		}
		return commaCount;	
	}
	
	public String getInsideIf(String C) {
		int commaCount = 1;
		String result = "";
		for(int i = 0; i < C.length(); i++) {
			char character = C.charAt(i);
			if(character == 'i') {
				if (C.charAt(i+2) != 'f') {
					//if inside if case
					commaCount += 2;	
				}else {
					//iff inside if case
					commaCount += getIffCount(C.substring(i+4));
				}
			}
			//min inside if case
			if(character == 'm') {
				commaCount += 1;
			}
			if(character == ',') {
				commaCount--;
			}
			if(commaCount != 0) {
				result = result + character;
			}else {
				break;
			}
		}
		return result;
	}
	
	public String getSequentialCut(String C) {
		int bracketCount = 0;
		String result = "";
		for(int i = 0; i < C.length(); i++) {
			char character = C.charAt(i);
			if(character == '{') {
				bracketCount++;
			}
			if(character == '}') {
				bracketCount--;
			}
			if(character == ';' && bracketCount == 0) {
				break;
			}else {
				result = result + character;
			}
		}
		return result;
	}
	
 //Deprecated method to calculate restrictions on variables without MathParser
  public String truncate(String input) {
	String result ="";
	for(int i = 0; i < input.length(); i++) {
		char character = input.charAt(i);
		if(character == '#') {
			String inside = getInsideBracket(input.substring(i+2));
			String insideCalc = calculation(inside);
			if(NumberUtils.isCreatable(insideCalc)) {
				double insideValue = Double.parseDouble(insideCalc);
				if(insideValue <= 0) {
					input = input.replace("#{"+inside+"}", "0");
					i--;
				}else {
					String truncatedValue = Double.toString(NumberUtils.min(insideValue,mainView.getRestriction()));							
					input = input.replace("#{"+inside+"}", truncatedValue);
					i--;
				}
			}else {
				if(inside.contains("#")) {
					String subterm = truncate(inside);
					String replacedInput = input.replace("#{"+inside+"}", "#{"+subterm+"}");
					if(!replacedInput.equals(input)) {
						input = replacedInput;
						i--;
					}else {
						result = result + "#";
					}
				}else {
					result = result + "#";
				}
			}				
		}else {
			result = result + character;
		}
	}
		return result;
  	}

	public ArrayList<String> getWhileLoops() {
		return whileLoops;
	}

	public void setWhileLoops(ArrayList<String> whileLoops) {
		this.whileLoops = whileLoops;
	}
	
	public LinkedHashMap<String, String> getFixpointCache() {
		return fixpointCache;
	}
	
	public void setFixpointCache(LinkedHashMap<String, String> fixpointCache) {
		this.fixpointCache = fixpointCache;
	}
	
	public void flushWhileLoops() {
		whileLoops.clear();
	}
	
	public void clearFixpointCache() {
		fixpointCache.clear();
	}
	
	public void linkView(WPCalculatorView mainView) {
		this.mainView = mainView;
	}
	
	
}
