/*
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/stringprep/Attic/IDNA.java,v $
 * $Date: 2003/08/27 03:09:08 $
 * $Revision: 1.2 $ 
 *
 *****************************************************************************************
 */
package com.ibm.icu.stringprep;

import java.io.IOException;
import java.io.InputStream;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.UCharacterIterator;

/**
 *
 * IDNA API implements the IDNA protocol as defined in the IDNA draft 
 * (http://www.ietf.org/rfc/rfc3490.txt).
 * The draft defines 2 operations: ToASCII and ToUnicode. Domain labels 
 * containing non-ASCII code points are required to be processed by
 * ToASCII operation before passing it to resolver libraries. Domain names
 * that are obtained from resolver libraries are required to be processed by
 * ToUnicode operation before displaying the domain name to the user.
 * IDNA requires that implementations process input strings with Nameprep
 * (http://www.ietf.org/rfc/rfc3491.txt), 
 * which is a profile of Stringprep (http://www.ietf.org/rfc/rfc3454.txt), 
 * and then with Punycode (http://www.ietf.org/rfc/rfc3492.txt). 
 * Implementations of IDNA MUST fully implement Nameprep and Punycode; 
 * neither Nameprep nor Punycode are optional.
 * The input and output of ToASCII and ToUnicode operations are Unicode 
 * and are designed to be chainable, i.e., applying ToASCII or ToUnicode operations
 * multiple times to an input string will yield the same result as applying the operation
 * once.
 * ToUnicode(ToUnicode(ToUnicode...(ToUnicode(string)))) == ToUnicode(string) 
 * ToASCII(ToASCII(ToASCII...(ToASCII(string))) == ToASCII(string).
 * 
 * @author Ram Viswanadha
 */
public final class IDNA {

    /* IDNA ACE Prefix is "xn--" */
    private static char[] ACE_PREFIX = new char[]{ 0x0078,0x006E,0x002d,0x002d } ;
    private static final int ACE_PREFIX_LENGTH  = 4;

    private static final int MAX_LABEL_LENGTH   = 63;
    private static final int HYPHEN             = 0x002D;
    private static final String NAME_PREP_PROFILE = "uidna";
    private static final int CAPITAL_A          = 0x0041;
    private static final int CAPITAL_Z          = 0x005A;
    private static final int LOWER_CASE_DELTA   = 0x0020;
    private static final int FULL_STOP          = 0x002E;

    /** 
     * Option to prohibit processing of unassigned codepoints in the input and
     * do not check if the input conforms to STD-3 ASCII rules.
     * 
     * @see  convertToASCII convertToUnicode
     * @draft ICU 2.8
     */
    public static final int DEFAULT             = 0x0000;
    /** 
     * Option to allow processing of unassigned codepoints in the input
     * 
     * @see  convertToASCII convertToUnicode
     * @draft ICU 2.8
     */
    public static final int ALLOW_UNASSIGNED    = 0x0001;
    /** 
     * Option to check if input conforms to STD-3 ASCII rules
     * 
     * @see convertToASCII convertToUnicode
     * @draft ICU 2.8
     */
    public static final int USE_STD3_RULES      = 0x0002;
    
    private static StringPrep prep  = null;
    
  
    private static synchronized void loadInstance()
                                throws IOException{
        if(prep==null){
           InputStream stream = LocaleUtility.getImplDataResourceAsStream("uidna.spp");
           prep = StringPrep.getInstance(stream);
           stream.close();
        }        
    }
    
    private static boolean startsWithPrefix(StringBuffer src){
        boolean startsWithPrefix = true;

        if(src.length() < ACE_PREFIX_LENGTH){
            return false;
        }
        for(int i=0; i<ACE_PREFIX_LENGTH;i++){
            if(toASCIILower(src.charAt(i)) != ACE_PREFIX[i]){
                startsWithPrefix = false;
            }
        }
        return startsWithPrefix;
    }

    private static char toASCIILower(char ch){
        if(CAPITAL_A <= ch && ch <= CAPITAL_Z){
            return (char)(ch + LOWER_CASE_DELTA);
        }
        return ch;
    }

    private static StringBuffer toASCIILower(StringBuffer src){
        StringBuffer dest = new StringBuffer();
        for(int i=0; i<src.length();i++){
            dest.append(toASCIILower(src.charAt(i)));
        }
        return dest;
    }

    private static int compareCaseInsensitiveASCII(StringBuffer s1, StringBuffer s2){
        char c1,c2;
        int rc;
        for(int i =0;/* no condition */;i++) {
            /* If we reach the ends of both strings then they match */
            if(i == s1.length()) {
                return 0;
            }

            c1 = s1.charAt(i);
            c2 = s2.charAt(i);
        
            /* Case-insensitive comparison */
            if(c1!=c2) {
                rc=(int)toASCIILower(c1)-(int)toASCIILower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
        }
    }
    private static int compareCaseInsensitiveASCII(String s1, String s2){
        char c1,c2;
        int rc;
        for(int i =0;/* no condition */;i++) {
            /* If we reach the ends of both strings then they match */
            if(i == s1.length()) {
                return 0;
            }

            c1 = s1.charAt(i);
            c2 = s2.charAt(i);
        
            /* Case-insensitive comparison */
            if(c1!=c2) {
                rc=(int)toASCIILower(c1)-(int)toASCIILower(c2);
                if(rc!=0) {
                    return rc;
                }
            }
        }
    }   
    private static int getSeparatorIndex(char[] src,int start, int limit)
                       throws IOException{
        loadInstance();
        for(; start<limit;start++){
            if(prep.isLabelSeparator(src[start])){
                return start;
            }
        }
        // we have not found the separator just return length
        return start;
    }
    
    private static boolean isLDHChar(int ch){
        // high runner case
        if(ch>0x007A){
            return false;
        }
        //[\\u002D \\u0030-\\u0039 \\u0041-\\u005A \\u0061-\\u007A]
        if( (ch==0x002D) || 
            (0x0030 <= ch && ch <= 0x0039) ||
            (0x0041 <= ch && ch <= 0x005A) ||
            (0x0061 <= ch && ch <= 0x007A)
          ){
            return true;
        }
        return false;
    }
    
    /* private constructor to prevent construction of the object */
    private IDNA(){}
    
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */    
    public static StringBuffer convertToASCII(String src, int options)
        throws ParseException, IOException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToASCII(iter,options);
    }
    
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertToASCII(StringBuffer src, int options)
        throws ParseException, IOException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToASCII(iter,options);
    }
    
    /**
     * This function implements the ToASCII operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * ASCII names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     *
     * @param src       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertToASCII(UCharacterIterator srcIter, int options)
                throws ParseException, IOException{
        //load the data
        loadInstance();
        
        boolean[] caseFlags = null;
    
        // the source contains all ascii codepoints
        boolean srcIsASCII  = true;
        // assume the source contains all LDH codepoints
        boolean srcIsLDH = true; 

        //get the options
        boolean useSTD3ASCIIRules = (boolean)((options & USE_STD3_RULES) != 0);
    
        int failPos = -1;
        // step 2
        StringBuffer processOut = prep.prepare(srcIter,options);
        int poLen = processOut.length();
        StringBuffer dest = new StringBuffer();
        // step 3 & 4
        for(int j=0;j<poLen;j++ ){
            char ch=processOut.charAt(j);
            if(ch > 0x7F){
                srcIsASCII = false;
            }
            // here we do not assemble surrogates
            // since we know that LDH code points
            // are in the ASCII range only
            if(isLDHChar(ch)==false){
                srcIsLDH = false;
                failPos = j;
            }
        }
    
        if(useSTD3ASCIIRules == true){
            // verify 3a and 3b
            if( srcIsLDH == false /* source contains some non-LDH characters */
                || processOut.charAt(0) ==  HYPHEN 
                || processOut.charAt(processOut.length()-1) == HYPHEN){

                /* populate the parseError struct */
                if(srcIsLDH==false){
                     throw new ParseException( "The input does not conform to the STD 3 ASCII rules",
                                              ParseException.STD3_ASCII_RULES_ERROR,
                                              processOut.toString(),
                                             (failPos>0) ? (failPos-1) : failPos);
                }else if(processOut.charAt(0) == HYPHEN){
                    throw new ParseException("The input does not conform to the STD 3 ASCII rules",
                                              ParseException.STD3_ASCII_RULES_ERROR,processOut.toString(),0);
     
                }else{
                     throw new ParseException("The input does not conform to the STD 3 ASCII rules",
                                              ParseException.STD3_ASCII_RULES_ERROR,
                                              processOut.toString(),
                                              (poLen>0) ? poLen-1 : poLen);

                }
            }
        }
        if(srcIsASCII){
            dest =  processOut;
        }else{
            // step 5 : verify the sequence does not begin with ACE prefix
            if(!startsWithPrefix(processOut)){

                //step 6: encode the sequence with punycode
                caseFlags = new boolean[poLen];

                StringBuffer punyout = Punycode.encode(processOut,caseFlags);

                // convert all codepoints to lower case ASCII
                StringBuffer lowerOut = toASCIILower(punyout);

                //Step 7: prepend the ACE prefix
                dest.append(ACE_PREFIX,0,ACE_PREFIX_LENGTH);
                //Step 6: copy the contents in b2 into dest
                dest.append(lowerOut);
            }else{

                throw new ParseException("The input does not start with the ACE Prefix.",
                                         ParseException.ACE_PREFIX_ERROR,processOut.toString(),0);
            }
        }
        if(dest.length() > MAX_LABEL_LENGTH){
            throw new ParseException("The labels in the input are too long. Length > 64.", 
                                     ParseException.LABEL_TOO_LONG_ERROR,dest.toString(),0);
        }
        return dest;
    }
        
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param iter      The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNtoASCII(UCharacterIterator iter,int options)
            throws ParseException, IOException{
        return convertIDNToASCII(iter.getText(), options);          
    }
    
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNtoASCII(StringBuffer str,int options)
            throws ParseException, IOException{
            return convertIDNToASCII(str.toString(), options);          
    }
    
    /**
     * Convenience function that implements the IDNToASCII operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     * It is important to note that this operation can fail. If it fails, then the input 
     * domain name cannot be used as an Internationalized Domain Name and the application
     * should have methods defined to deal with the failure.
     * 
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNToASCII(String src,int options)
            throws ParseException, IOException{
        //load the data
        loadInstance();
        char[] srcArr = src.toCharArray();
        StringBuffer result = new StringBuffer();
        int sepIndex=0;
        int oldSepIndex=0;
        for(;;){
            sepIndex = getSeparatorIndex(srcArr,sepIndex,srcArr.length);
            UCharacterIterator iter = UCharacterIterator.getInstance(new String(srcArr,oldSepIndex,sepIndex-oldSepIndex));
            result.append(convertToASCII(iter,options));
            if(sepIndex==srcArr.length){
                break;
            }
            // increment the sepIndex to skip past the separator
            sepIndex++;
            oldSepIndex = sepIndex;
            result.append((char)FULL_STOP);
        }
        return result;
    }

    
    /**
     * This function implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertToUnicode(String src, int options)
           throws ParseException, IOException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToUnicode(iter,options);
    }
    
    /**
     * This function implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertToUnicode(StringBuffer src, int options)
           throws ParseException, IOException{
        UCharacterIterator iter = UCharacterIterator.getInstance(src);
        return convertToUnicode(iter,options);
    }
       
    /**
     * This function implements the ToUnicode operation as defined in the IDNA RFC.
     * This operation is done on <b>single labels</b> before sending it to something that expects
     * Unicode names. A label is an individual part of a domain name. Labels are usually
     * separated by dots; for e.g." "www.example.com" is composed of 3 labels 
     * "www","example", and "com".
     * 
     * @param iter       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertToUnicode(UCharacterIterator iter, int options)
           throws ParseException, IOException{
        //load the data
        loadInstance();
        
        boolean[] caseFlags = null;
        
        // the source contains all ascii codepoints
        boolean srcIsASCII  = true;
        // assume the source contains all LDH codepoints
        boolean srcIsLDH = true; 
        
        //get the options
        boolean useSTD3ASCIIRules = (boolean)((options & USE_STD3_RULES) != 0);
        
        int failPos = -1;
        int ch;
        int saveIndex = iter.getIndex();
        // step 1: find out if all the codepoints in src are ASCII  
        while((ch=iter.next())!= UCharacterIterator.DONE){
            if(ch>0x7F){
                srcIsASCII = false;
            }
            if((srcIsLDH = isLDHChar(ch))==false){
                failPos = iter.getIndex();
            }
        }
        StringBuffer processOut;
        
        if(srcIsASCII == false){
            // step 2: process the string
            iter.setIndex(saveIndex);
            processOut = prep.prepare(iter,options);

        }else{
            //just point to source
            processOut = new StringBuffer(iter.getText());
        }
        // TODO:
        // The RFC states that 
        // <quote>
        // ToUnicode never fails. If any step fails, then the original input
        // is returned immediately in that step.
        // </quote>
        
        //step 3: verify ACE Prefix
        if(startsWithPrefix(processOut)){

            //step 4: Remove the ACE Prefix
            String temp = processOut.substring(ACE_PREFIX_LENGTH,processOut.length());

            //step 5: Decode using punycode
            StringBuffer decodeOut = Punycode.decode(new StringBuffer(temp),caseFlags);
        
            //step 6:Apply toASCII
            StringBuffer toASCIIOut = convertToASCII(decodeOut, options);

            //step 7: verify
            if(compareCaseInsensitiveASCII(processOut, toASCIIOut) !=0){
                throw new ParseException("The verification step prescribed by the RFC 3491 failed",
                                         ParseException.VERIFICATION_ERROR); 
            }

            //step 8: return output of step 5
            return decodeOut;
            
        }else{
            // verify that STD3 ASCII rules are satisfied
            if(useSTD3ASCIIRules == true){
                if( srcIsLDH == false /* source contains some non-LDH characters */
                    || processOut.charAt(0) ==  HYPHEN 
                    || processOut.charAt(processOut.length()-1) == HYPHEN){
    
                    if(srcIsLDH==false){
                        throw new ParseException("The input does not conform to the STD 3 ASCII rules",
                                                 ParseException.STD3_ASCII_RULES_ERROR,processOut.toString(),
                                                 (failPos>0) ? (failPos-1) : failPos);
                    }else if(processOut.charAt(0) == HYPHEN){
                        throw new ParseException("The input does not conform to the STD 3 ASCII rules",
                                                 ParseException.STD3_ASCII_RULES_ERROR,
                                                 processOut.toString(),0);
         
                    }else{
                        throw new ParseException("The input does not conform to the STD 3 ASCII rules",
                                                 ParseException.STD3_ASCII_RULES_ERROR,
                                                 processOut.toString(),
                                                 processOut.length());
    
                    }
                }
            }
            // just return the source
            return new StringBuffer(iter.getText());
        }  
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param iter       The input string as UCharacterIterator to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(UCharacterIterator iter, int options)
        throws ParseException, IOException{
        return convertIDNToUnicode(iter.getText(), options);
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string as StringBuffer to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(StringBuffer str, int options)
        throws ParseException, IOException{
        return convertIDNToUnicode(str.toString(), options);
    }
    
    /**
     * Convenience function that implements the IDNToUnicode operation as defined in the IDNA RFC.
     * This operation is done on complete domain names, e.g: "www.example.com". 
     *
     * <b>Note:</b> IDNA RFC specifies that a conformant application should divide a domain name
     * into separate labels, decide whether to apply allowUnassigned and useSTD3ASCIIRules on each, 
     * and then convert. This function does not offer that level of granularity. The options once  
     * set will apply to all labels in the domain name
     *
     * @param src       The input string to be processed
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return StringBuffer the converted String
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    public static StringBuffer convertIDNToUnicode(String src, int options)
        throws ParseException, IOException{
            
        char[] srcArr = src.toCharArray();
        StringBuffer result = new StringBuffer();
        int sepIndex=0;
        int oldSepIndex=0;
        for(;;){
            sepIndex = getSeparatorIndex(srcArr,sepIndex,srcArr.length);
            UCharacterIterator iter = UCharacterIterator.getInstance(new String(srcArr,oldSepIndex,sepIndex-oldSepIndex));
            result.append(convertToUnicode(iter,options));
            if(sepIndex==srcArr.length){
                break;
            }
            // increment the sepIndex to skip past the separator
            sepIndex++;
            oldSepIndex =sepIndex;
            result.append((char)FULL_STOP);
        }
        return result;
    }
    
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string as StringBuffer
     * @param s2        Second IDN string as StringBuffer
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED    Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES      Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if s1 > s2 and < 0 if s1 < s2
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    //  TODO: optimize
    public static int compare(StringBuffer s1, StringBuffer s2, int options)
        throws ParseException, IOException{
        if(s1==null || s2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(s1.toString(),options);
        StringBuffer s2Out = convertIDNToASCII(s2.toString(), options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
    
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string 
     * @param s2        Second IDN string
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED    Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES      Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if s1 > s2 and < 0 if s1 < s2
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    //  TODO: optimize
    public static int compare(String s1, String s2, int options)
        throws ParseException, IOException{
        if(s1==null || s2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(s1, options);
        StringBuffer s2Out = convertIDNToASCII(s2, options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
    /**
     * Compare two IDN strings for equivalence.
     * This function splits the domain names into labels and compares them.
     * According to IDN RFC, whenever two labels are compared, they are 
     * considered equal if and only if their ASCII forms (obtained by 
     * applying toASCII) match using an case-insensitive ASCII comparison.
     * Two domain names are considered a match if and only if all labels 
     * match regardless of whether label separators match.
     * 
     * @param s1        First IDN string as UCharacterIterator
     * @param s2        Second IDN string as UCharacterIterator
     * @param options   A bit set of options:
     *  - IDNA.DEFAULT              Use default options, i.e., do not process unassigned code points
     *                              and do not use STD3 ASCII rules
     *                              If unassigned code points are found the operation fails with 
     *                              ParseException.
     *
     *  - IDNA.ALLOW_UNASSIGNED     Unassigned values can be converted to ASCII for query operations
     *                              If this option is set, the unassigned code points are in the input 
     *                              are treated as normal Unicode code points.
     *                          
     *  - IDNA.USE_STD3_RULES       Use STD3 ASCII rules for host name syntax restrictions
     *                              If this option is set and the input does not satisfy STD3 rules,  
     *                              the operation will fail with ParseException
     * @return 0 if the strings are equal, > 0 if i1 > i2 and < 0 if i1 < i2
     * @throws ParseException
     * @throws IOException
     * @draft ICU 2.8
     */
    //  TODO: optimize
    public static int compare(UCharacterIterator i1, UCharacterIterator i2, int options)
        throws ParseException, IOException{
        if(i1==null || i2 == null){
            throw new IllegalArgumentException("One of the source buffers is null");
        }
        StringBuffer s1Out = convertIDNToASCII(i1.getText(), options);
        StringBuffer s2Out = convertIDNToASCII(i2.getText(), options);
        return compareCaseInsensitiveASCII(s1Out,s2Out);
    }
}
