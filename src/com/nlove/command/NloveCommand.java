package com.nlove.command;

public class NloveCommand {

	public static String MAGIC_IDENTIFIER = "<<NLOV>>";
	
	public String search(String term) {
		return MAGIC_IDENTIFIER + "SEARCH " + term;
	}
}
