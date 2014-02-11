/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.CharacterIterator;
import java.util.BitSet;
import java.util.Deque;

import com.ibm.icu.impl.CharacterIteration;

abstract class DictionaryBreakEngine implements LanguageBreakEngine {
    
    /* Helper class for improving readability of the Thai/Lao/Khmer word break
     * algorithm.
     */
    static class PossibleWord {
        // List size, limited by the maximum number of words in the dictionary
        // that form a nested sequence.
        private final static int POSSIBLE_WORD_LIST_MAX = 20;
        //list of word candidate lengths, in increasing length order
        private int lengths[];
        private int count[];    // Count of candidates
        private int prefix;     // The longest match with a dictionary word
        private int offset;     // Offset in the text of these candidates
        private int mark;       // The preferred candidate's offset
        private int current;    // The candidate we're currently looking at

        // Default constructor
        public PossibleWord() {
            lengths = new int[POSSIBLE_WORD_LIST_MAX];
            count = new int[1]; // count needs to be an array of 1 so that it can be pass as reference
            offset = -1;
        }

        // Fill the list of candidates if needed, select the longest, and return the number found
        public int candidates(CharacterIterator fIter, DictionaryMatcher dict, int rangeEnd) {
            int start = fIter.getIndex();
            if (start != offset) {
                offset = start;
                prefix = dict.matches(fIter, rangeEnd - start, lengths, count, lengths.length);
                // Dictionary leaves text after longest prefix, not longest word. Back up.
                if (count[0] <= 0) {
                    fIter.setIndex(start);
                }
            }
            if (count[0] > 0) {
                fIter.setIndex(start + lengths[count[0]-1]);
            }
            current = count[0] - 1;
            mark = current;
            return count[0];
        }

        // Select the currently marked candidate, point after it in the text, and invalidate self
        public int acceptMarked(CharacterIterator fIter) {
            fIter.setIndex(offset + lengths[mark]);
            return lengths[mark];
        }

        // Backup from the current candidate to the next shorter one; return true if that exists
        // and point the text after it
        public boolean backUp(CharacterIterator fIter) {
            if (current > 0) {
                fIter.setIndex(offset + lengths[--current]);
                return true;
            }
            return false;
        }

        // Return the longest prefix this candidate location shares with a dictionary word
        public int longestPrefix() {
            return prefix;
        }

        // Mark the current candidate as the one we like
        public void markCurrent() {
            mark = current;
        }
    }
    
    
    
    UnicodeSet fSet = new UnicodeSet();
    private BitSet fTypes = new BitSet(32);

    /**
     * @param breakTypes The types of break iterators that can use this engine.
     *  For example, BreakIterator.KIND_LINE 
     */
    public DictionaryBreakEngine(Integer... breakTypes) {
        for (Integer type: breakTypes) {
            fTypes.set(type);
        }
    }

    public boolean handles(int c, int breakType) {
        return fTypes.get(breakType) &&  // this type can use us
                fSet.contains(c);        // we recognize the character
    }

    public int findBreaks(CharacterIterator text, int startPos, int endPos, 
            boolean reverse, int breakType, Deque<Integer> foundBreaks) {
         int result = 0;
       
         // Find the span of characters included in the set.
         //   The span to break begins at the current position int the text, and
         //   extends towards the start or end of the text, depending on 'reverse'.

        int start = text.getIndex();
        int current;
        int rangeStart;
        int rangeEnd;
        int c = CharacterIteration.current32(text);
        if (reverse) {
            boolean isDict = fSet.contains(c);
            while ((current = text.getIndex()) > startPos && isDict) {
                c = CharacterIteration.previous32(text);
                isDict = fSet.contains(c);
            }
            rangeStart = (current < startPos) ? startPos :
                                                current + (isDict ? 0 : 1);
            rangeEnd = start + 1;
        } else {
            while ((current = text.getIndex()) < endPos && fSet.contains(c)) {
                CharacterIteration.next32(text);
                c = CharacterIteration.current32(text);
            }
            rangeStart = start;
            rangeEnd = current;
        }

        result = divideUpDictionaryRange(text, rangeStart, rangeEnd, foundBreaks);
        text.setIndex(current);

        return result;
    }
    
    void setCharacters(UnicodeSet set) {
        fSet = new UnicodeSet(set);
        fSet.compact();
    }

    /**
     * <p>Divide up a range of known dictionary characters handled by this break engine.</p>
     *
     * @param text A UText representing the text
     * @param rangeStart The start of the range of dictionary characters
     * @param rangeEnd The end of the range of dictionary characters
     * @param foundBreaks Output of break positions. Positions are pushed.
     *                    Pre-existing contents of the output stack are unaltered.
     * @return The number of breaks found
     */
     abstract int divideUpDictionaryRange(CharacterIterator text,
                                          int               rangeStart,
                                          int               rangeEnd,
                                          Deque<Integer>    foundBreaks );
}
