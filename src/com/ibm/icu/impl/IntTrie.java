/*
******************************************************************************
* Copyright (C) 1996-2003, International Business Machines Corporation and   *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/IntTrie.java,v $
* $Date: 2003/06/03 18:49:32 $
* $Revision: 1.10 $
*
******************************************************************************
*/

package com.ibm.icu.impl;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import com.ibm.icu.text.UTF16;

/**
 * Trie implementation which stores data in int, 32 bits.
 * @author synwee
 * @see com.ibm.icu.util.Trie
 * @since release 2.1, Jan 01 2002
 */
public class IntTrie extends Trie
{
    // public constructors ---------------------------------------------

    /**
    * <p>Creates a new Trie with the settings for the trie data.</p>
    * <p>Unserialize the 32-bit-aligned input stream and use the data for the 
    * trie.</p>
    * @param inputStream file input stream to a ICU data file, containing 
    *                    the trie
    * @param dataManipulate, object which provides methods to parse the char 
    *                        data
    * @exception IOException thrown when data reading fails
    * @draft 2.1
    */
    public IntTrie(InputStream inputStream, DataManipulate datamanipulate)
                                                    throws IOException
    {
        super(inputStream, datamanipulate);
        if (!isIntTrie()) {
            throw new IllegalArgumentException(
                               "Data given does not belong to a int trie.");
        }
    }

    // public methods --------------------------------------------------

    /**
    * Gets the value associated with the codepoint.
    * If no value is associated with the codepoint, a default value will be
    * returned.
    * @param ch codepoint
    * @return offset to data
    * @draft 2.1
    */
    public final int getCodePointValue(int ch)
    {
        int offset = getCodePointOffset(ch);
        return (offset >= 0) ? m_data_[offset] : m_initialValue_;
    }

    /**
    * Gets the value to the data which this lead surrogate character points
    * to.
    * Returned data may contain folding offset information for the next
    * trailing surrogate character.
    * This method does not guarantee correct results for trail surrogates.
    * @param ch lead surrogate character
    * @return data value
    * @draft 2.1
    */
    public final int getLeadValue(char ch)
    {
        return m_data_[getLeadOffset(ch)];
    }

    /**
    * Get the value associated with the BMP code point.
    * Lead surrogate code points are treated as normal code points, with
    * unfolded values that may differ from getLeadValue() results.
    * @param ch the input BMP code point
    * @return trie data value associated with the BMP codepoint
    * @draft 2.1
    */
    public final int getBMPValue(char ch)
    {
        return m_data_[getBMPOffset(ch)];
    }

    /**
    * Get the value associated with a pair of surrogates.
    * @param lead a lead surrogate
    * @param trail a trail surrogate
    * @param trie data value associated with the surrogate characters
    * @draft 2.1
    */
    public final int getSurrogateValue(char lead, char trail)
    {
        if (!UTF16.isLeadSurrogate(lead) || !UTF16.isTrailSurrogate(trail)) {
            throw new IllegalArgumentException(
                "Argument characters do not form a supplementary character");
        }
        // get fold position for the next trail surrogate
        int offset = getSurrogateOffset(lead, trail);

        // get the real data from the folded lead/trail units
        if (offset > 0) {
            return m_data_[offset];
        }

        // return m_initialValue_ if there is an error
        return m_initialValue_;
    }

    /**
    * Get a value from a folding offset (from the value of a lead surrogate)
    * and a trail surrogate.
    * @param leadvalue the value of a lead surrogate that contains the
    *        folding offset
    * @param trail surrogate
    * @return trie data value associated with the trail character
    * @draft 2.1
    */
    public final int getTrailValue(int leadvalue, char trail)
    {
        if (m_dataManipulate_ == null) {
            throw new NullPointerException(
                             "The field DataManipulate in this Trie is null");
        }
        int offset = m_dataManipulate_.getFoldingOffset(leadvalue);
        if (offset > 0) {
	        return m_data_[getRawOffset(offset,
 	                                    (char)(trail & SURROGATE_MASK_))];
        }
        return m_initialValue_;
    }
    
	/**
     * <p>Gets the latin 1 fast path value.</p>
     * <p>Note this only works if latin 1 characters have their own linear 
     * array.</p>
     * @param ch latin 1 characters
     * @return value associated with latin character
     */
    public final int getLatin1LinearValue(char ch) 
    {
    	return m_data_[INDEX_STAGE_3_MASK_ + 1 + ch];
    }

    /**
     * Checks if the argument Trie has the same data as this Trie
     * @param other Trie to check
     * @return true if the argument Trie has the same data as this Trie, false
     *         otherwise
     */
    public boolean equals(Object other) 
    {
        boolean result = super.equals(other);
        if (result && other instanceof IntTrie) {
            IntTrie othertrie = (IntTrie)other;
            if (m_initialValue_ != othertrie.m_initialValue_
                || !Arrays.equals(m_data_, othertrie.m_data_)) {
                return false;
            }
            return true;
        }
        return false;
    }
    
    // protected methods -----------------------------------------------

    /**
    * <p>Parses the input stream and stores its trie content into a index and
    * data array</p>
    * @param inputStream data input stream containing trie data
    * @exception IOException thrown when data reading fails
    */
    protected final void unserialize(InputStream inputStream) 
                                                    throws IOException
    {
        super.unserialize(inputStream);
        // one used for initial value
        m_data_               = new int[m_dataLength_];
        DataInputStream input = new DataInputStream(inputStream);
        for (int i = 0; i < m_dataLength_; i ++) {
            m_data_[i] = input.readInt();
        }
        m_initialValue_ = m_data_[0];
    }
    
    /**
    * Gets the offset to the data which the surrogate pair points to.
    * @param lead lead surrogate
    * @param trail trailing surrogate
    * @return offset to data
    * @draft 2.1
    */
    protected final int getSurrogateOffset(char lead, char trail)
    {
        if (m_dataManipulate_ == null) {
            throw new NullPointerException(
                             "The field DataManipulate in this Trie is null");
        }
        // get fold position for the next trail surrogate
        int offset = m_dataManipulate_.getFoldingOffset(getLeadValue(lead));

        // get the real data from the folded lead/trail units
        if (offset > 0) {
            return getRawOffset(offset, (char)(trail & SURROGATE_MASK_));
        }

        // return -1 if there is an error, in this case we return the default
        // value: m_initialValue_
        return -1;
    }
    
    /**
    * Gets the value at the argument index.
    * For use internally in com.ibm.icu.util.TrieEnumeration.
    * @param index value at index will be retrieved
    * @return 32 bit value
    * @see com.ibm.icu.util.TrieEnumeration
    * @draft 2.1
    */
    protected final int getValue(int index)
    {
      return m_data_[index];
    }
    
    /**
    * Gets the default initial value
    * @return 32 bit value 
    * @draft 2.1
    */
    protected final int getInitialValue()
    {
        return m_initialValue_;
    }

    // package private methods -----------------------------------------
    
    /**
     * Internal constructor for builder use
     * @param index the index array to be slotted into this trie
     * @param data the data array to be slotted into this trie
     * @param initialvalue the initial value for this trie
     * @param options trie options to use
     * @param datamanipulate folding implementation
     */
    IntTrie(char index[], int data[], int initialvalue, int options,
            DataManipulate datamanipulate)
    {
        super(index, options, datamanipulate);
        m_index_ = index;
        m_data_ = data;
        m_dataLength_ = m_data_.length;
        m_initialValue_ = initialvalue;
    }
    
    // private data members --------------------------------------------

    /**
    * Default value
    */
    private int m_initialValue_;
    /**
    * Array of char data
    */
    private int m_data_[];
}