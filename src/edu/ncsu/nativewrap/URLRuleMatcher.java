/*
 * Copyright (c) 2014, North Carolina State University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of North Carolina State University nor the names of
 * its contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

package edu.ncsu.nativewrap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;



public class URLRuleMatcher extends DefaultHandler{

	String targetURLString="";
	URL targetURL=null;
	boolean hostMatched=false;
	URLRuleMatcher()
	{}
	URLRuleMatcher(String urlstring, URL url)
	{
		targetURLString=urlstring;
		targetURL=url;
	}
	
	public static String[] getForceHTTPSUrl( String targetURLString, InputStream is)throws IOException, SAXException,
    ParserConfigurationException
	{
		//Create a "parser factory" for creating SAX parsers
        SAXParserFactory spfac = SAXParserFactory.newInstance();

        //Now use the parser factory to create a SAXParser object
        SAXParser sp = spfac.newSAXParser();
        
        //String urlstring= "http://www.1lotstp.com/";
        URL targetURL = new URL(targetURLString);
        /*if(targetURL.getProtocol().equals("https"))
        	return null;
        */
        //The result obtained after parsing, i.e. the resultant URLString and the rule
    		String resultURLString=null;
    		String ruleTo=null;
    		String ruleFrom=null;
        //Create an instance of this class; it defines all the handler methods
        URLRuleMatcher handler = new URLRuleMatcher(targetURLString, targetURL);
        
        //Finally, tell the parser to parse the input and notify the handler
        //sp.parse("1LotSTP.com.xml", handler);
        //First get the ruleset in an InputStream from assets
        try{
     	   //sp.parse("1LotSTP.com.xml", handler);
     	   //sp.parse("ruleset.xml", handler);
     	   sp.parse(is, handler);
        }
        catch(SAXTerminationException e)
        {
     	   if(!e.found()){
     		   //System.out.println("No Rule Match found for "+targetURLString);
     		   resultURLString=null;
     	   }
     	   else
     	   {	
     		   resultURLString=e.newURLString();
     		   ruleTo=e.ruleTo();
     		   ruleFrom=e.ruleFrom();
     	   }
        }
        //System.out.println(handler.hostMatched);
        //System.out.println(resultURLString);
	
		return new String[]{resultURLString, ruleFrom, ruleTo};
		
	}
	
	
	/*
     * When the parser encounters plain text (not XML elements),
     * it calls(this method, which accumulates them in a string buffer
     */
	public void characters(char[] buffer, int start, int length) {
		//temp = new String(buffer, start, length);
	}

/*
 * Every time the parser encounters the beginning of a new element,
 * it calls this method, which resets the string buffer
 */ 
	public void startElement(String uri, String localName,
			String qName, Attributes attributes) throws SAXException {		
		if (qName.equalsIgnoreCase("target")) {
			String hostAttr= attributes.getValue("host");
			//System.out.print("Target host="+attributes.getValue("host")+"..Actual host="+targetURL.getHost());
			if(MatchHost(targetURL.getHost(),hostAttr))
			{
				hostMatched=true;
				//System.out.println("Host Match!");
			}
			else
			{
				//System.out.println("No Host Match");
			}		
		}else if(qName.equalsIgnoreCase("exclusion") && hostMatched){
			String regexString = attributes.getValue("pattern");
			System.out.println("exclusion pattern : " + regexString + " for host : " + targetURLString);
			Pattern exclusionPattern = Pattern.compile(regexString);
			if(targetURLString.matches(regexString)) {
				//System.out.println("******exlcusion pattern matched**********");
				hostMatched=false;
			}
		}
		else if(qName.equalsIgnoreCase("rule"))
		{
			  if(!hostMatched)
				 return;
			  //System.out.println("Matching regex rule now");
			  //System.out.println(attributes.getValue("from"));
	     	             
	     	  //Target value changed for test cases only
	     	  //targetURLString="http://1lotstp.com/";
	     	  String resultURLString=null;
	     	  resultURLString=targetURLString.replaceAll(attributes.getValue("from"),attributes.getValue("to"));
	     	  if(targetURLString.equals(resultURLString))
	           {
	     		  //since replace returns the original string in case of a mismatch with the regex
	         	  //System.out.println("Rule Mismatch!");
	     	  }
	     	  else
	     	  {
	     		  //System.out.println("Rule Matched");
	     		  throw new SAXTerminationException(true, resultURLString, attributes.getValue("from"), attributes.getValue("to"));
	     	  }
	       }
	}
	public void endDocument() throws SAXException
	{
		   throw new SAXTerminationException(false, null, null, null);
	}
	
	public void endElement(String uri, String localName, String qName)
	        throws SAXException {
	 if (qName.equalsIgnoreCase("ruleset")) {
	        hostMatched=false;
	 }	
	}
	
	//SaxTerminationException, for stopping once we receive the desired rule.
	public class SAXTerminationException extends SAXException
	{
		 //found=true if rule matches, false if End of file is reached.  
		 boolean found=false;
		 String ruleFrom;
		 String ruleTo;
		 String newURLString;
		/**
		 * Auto Generated ID
		 */
		private static final long serialVersionUID = 1L;
		SAXTerminationException(boolean found, String newURLString, String from, String to)
		{
			this.found=found;
			this.newURLString=newURLString;
			this.ruleFrom=from;
			this.ruleTo=to;
		}
		
		//getters for this exception
		boolean found()
		{
			return found;
		}
		String ruleFrom()
		{
			return ruleFrom;
		}
		String ruleTo()
		{
			return ruleTo;
		}
		String newURLString()
		{
			return newURLString;
		}
	}
	//Matching the "target" part of the ruleset, i.e. the host
	//for which forceHTTPS is desired.
	public static boolean MatchHost(String text, String pattern)
	{
	    String [] parts = pattern.split("\\*");
	
	    // Iterate over the cards.
	    for (String part : parts)
	    {
	        int idx = text.indexOf(part);
	        
	        // Part not detected in the text.
	        if(idx == -1)
	        {
	            return false;
	        }
	        
	        // Move ahead, towards the right of the text.
	        text = text.substring(idx + part.length());
	    }
	    
	    return true;
	}
}
