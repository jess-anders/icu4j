/**
 * @(#)$RCSFile$ $Revision: 1.1 $ $Date: 2000/03/01 18:32:31 $
 *
 * (C) Copyright IBM Corp. 1998.  All Rights Reserved.
 *
 * The program is provided "as is" without any warranty express or
 * implied, including the warranty of non-infringement and the implied
 * warranties of merchantibility and fitness for a particular purpose.
 * IBM will not be liable for any damages suffered by you as a result
 * of using the Program. In no event will IBM be liable for any
 * special, indirect or consequential damages or lost profits even if
 * IBM has been advised of the possibility of their occurrence. IBM
 * will not be liable for any third party claims against you.
 */

package com.ibm.test.calendar;

import com.ibm.test.*;
import java.util.*;
import java.text.*;
import com.ibm.util.*;

/**
 * Tests for the <code>HebrewCalendar</code> class.
 */
public class HebrewTest extends CalendarTest {
    public static void main(String args[]) throws Exception {
        new HebrewTest().run(args);
    }

    // Constants to save typing.
    public static final int TISHRI  = HebrewCalendar.TISHRI;
    public static final int HESHVAN = HebrewCalendar.HESHVAN;
    public static final int KISLEV  = HebrewCalendar.KISLEV;
    public static final int TEVET   = HebrewCalendar.TEVET;
    public static final int SHEVAT  = HebrewCalendar.SHEVAT;
    public static final int ADAR_1  = HebrewCalendar.ADAR_1;
    public static final int ADAR    = HebrewCalendar.ADAR;
    public static final int NISAN   = HebrewCalendar.NISAN;
    public static final int IYAR    = HebrewCalendar.IYAR;
    public static final int SIVAN   = HebrewCalendar.SIVAN;
    public static final int TAMUZ   = HebrewCalendar.TAMUZ;
    public static final int AV      = HebrewCalendar.AV;
    public static final int ELUL    = HebrewCalendar.ELUL;

    /**
     * Test the behavior of HebrewCalendar.roll
     * The only real nastiness with roll is the MONTH field, since a year can
     * have a variable number of months.
     */
    public void TestRoll() {
        int[][] tests = new int[][] {
            //       input                roll by          output
            //  year  month     day     field amount    year  month     day
    
            {   5759, HESHVAN,   2,     MONTH,   1,     5759, KISLEV,    2 },   // non-leap years
            {   5759, SHEVAT,    2,     MONTH,   1,     5759, ADAR,      2 },
            {   5759, SHEVAT,    2,     MONTH,   2,     5759, NISAN,     2 },
            {   5759, SHEVAT,    2,     MONTH,  12,     5759, SHEVAT,    2 },

            {   5757, HESHVAN,   2,     MONTH,   1,     5757, KISLEV,    2 },   // leap years
            {   5757, SHEVAT,    2,     MONTH,   1,     5757, ADAR_1,    2 },
            {   5757, SHEVAT,    2,     MONTH,   2,     5757, ADAR,      2 },
            {   5757, SHEVAT,    2,     MONTH,   3,     5757, NISAN,     2 },
            {   5757, SHEVAT,    2,     MONTH,  12,     5757, TEVET,     2 },
            {   5757, SHEVAT,    2,     MONTH,  13,     5757, SHEVAT,    2 },
            
            {   5757, KISLEV,    1,     DATE,   30,     5757, KISLEV,    2 },   // 29-day month
            {   5758, KISLEV,    1,     DATE,   31,     5758, KISLEV,    2 },   // 30-day month
            
            // Try some other fields too
            {   5757, TISHRI,    1,     YEAR,    1,     5758, TISHRI,    1 },
   

            // Try some rolls that require other fields to be adjusted
            {   5757, TISHRI,   30,     MONTH,   1,     5757, HESHVAN,  29 },
            {   5758, KISLEV,   30,     YEAR,   -1,     5757, KISLEV,   29 },
        };
       
        HebrewCalendar cal = new HebrewCalendar(UTC, Locale.getDefault());

        doRollAdd(ROLL, cal, tests);
    }
    
    /**
     * Test the behavior of HebrewCalendar.roll
     * The only real nastiness with roll is the MONTH field, since a year can
     * have a variable number of months.
     */
    public void TestAdd() {
        int[][] tests = new int[][] {
            //       input                add by          output
            //  year  month     day     field amount    year  month     day
            {   5759, HESHVAN,   2,     MONTH,   1,     5759, KISLEV,    2 },   // non-leap years
            {   5759, SHEVAT,    2,     MONTH,   1,     5759, ADAR,      2 },
            {   5759, SHEVAT,    2,     MONTH,   2,     5759, NISAN,     2 },
            {   5759, SHEVAT,    2,     MONTH,  12,     5760, SHEVAT,    2 },

            {   5757, HESHVAN,   2,     MONTH,   1,     5757, KISLEV,    2 },   // leap years
            {   5757, SHEVAT,    2,     MONTH,   1,     5757, ADAR_1,    2 },
            {   5757, SHEVAT,    2,     MONTH,   2,     5757, ADAR,      2 },
            {   5757, SHEVAT,    2,     MONTH,   3,     5757, NISAN,     2 },
            {   5757, SHEVAT,    2,     MONTH,  12,     5758, TEVET,     2 },
            {   5757, SHEVAT,    2,     MONTH,  13,     5758, SHEVAT,    2 },
            
            {   5757, KISLEV,    1,     DATE,   30,     5757, TEVET,     2 },   // 29-day month
            {   5758, KISLEV,    1,     DATE,   31,     5758, TEVET,     2 },   // 30-day month
        };
       
        HebrewCalendar cal = new HebrewCalendar(UTC, Locale.getDefault());

        doRollAdd(ADD, cal, tests);
    }

    /**
     * A huge list of test cases to make sure that computeTime and computeFields
     * work properly for a wide range of data.
     */
    public void TestCases() {
        doTestCases(testCases, new HebrewCalendar());
    }

    static final TestCase[] testCases = {
        //
        // Most of these test cases were taken from the back of
        // "Calendrical Calculations", with some extras added to help
        // debug a few of the problems that cropped up in development.
        //
        // The months in this table are 1-based rather than 0-based,
        // because it's easier to edit that way.
        //
        //         Julian Day  Era  Year  Month Day  WkDay Hour Min Sec
        new TestCase(1507231.5,  0,  3174,   12,  10,  SUN,   0,  0,  0),
        new TestCase(1660037.5,  0,  3593,    3,  25,  WED,   0,  0,  0),
        new TestCase(1746893.5,  0,  3831,    1,   3,  WED,   0,  0,  0),
        new TestCase(1770641.5,  0,  3896,    1,   9,  SUN,   0,  0,  0),
        new TestCase(1892731.5,  0,  4230,    4,  18,  WED,   0,  0,  0),
        new TestCase(1931579.5,  0,  4336,   10,   4,  MON,   0,  0,  0),
        new TestCase(1974851.5,  0,  4455,    2,  13,  SAT,   0,  0,  0),
        new TestCase(2091164.5,  0,  4773,    9,   6,  SUN,   0,  0,  0),
        new TestCase(2121509.5,  0,  4856,    9,  23,  SUN,   0,  0,  0),
        new TestCase(2155779.5,  0,  4950,    8,   7,  FRI,   0,  0,  0),
        new TestCase(2174029.5,  0,  5000,    7,   8,  SAT,   0,  0,  0),
        new TestCase(2191584.5,  0,  5048,    8,  21,  FRI,   0,  0,  0),
        new TestCase(2195261.5,  0,  5058,    9,   7,  SUN,   0,  0,  0),
        new TestCase(2229274.5,  0,  5151,   11,   1,  SUN,   0,  0,  0),
        new TestCase(2245580.5,  0,  5196,    5,   7,  WED,   0,  0,  0),
        new TestCase(2266100.5,  0,  5252,    8,   3,  SAT,   0,  0,  0),
        new TestCase(2288542.5,  0,  5314,    1,   1,  SAT,   0,  0,  0),
        new TestCase(2290901.5,  0,  5320,    6,  27,  SAT,   0,  0,  0),
        new TestCase(2323140.5,  0,  5408,   10,  20,  WED,   0,  0,  0),
        new TestCase(2334551.5,  0,  5440,    1,   1,  THU,   0,  0,  0),
        new TestCase(2334581.5,  0,  5440,    2,   1,  SAT,   0,  0,  0),
        new TestCase(2334610.5,  0,  5440,    3,   1,  SUN,   0,  0,  0),
        new TestCase(2334639.5,  0,  5440,    4,   1,  MON,   0,  0,  0),
        new TestCase(2334668.5,  0,  5440,    5,   1,  TUE,   0,  0,  0),
        new TestCase(2334698.5,  0,  5440,    6,   1,  THU,   0,  0,  0),
        new TestCase(2334728.5,  0,  5440,    7,   1,  SAT,   0,  0,  0),
        new TestCase(2334757.5,  0,  5440,    8,   1,  SUN,   0,  0,  0),
        new TestCase(2334787.5,  0,  5440,    9,   1,  TUE,   0,  0,  0),
        new TestCase(2334816.5,  0,  5440,   10,   1,  WED,   0,  0,  0),
        new TestCase(2334846.5,  0,  5440,   11,   1,  FRI,   0,  0,  0),
        new TestCase(2334848.5,  0,  5440,   11,   3,  SUN,   0,  0,  0),
        new TestCase(2334934.5,  0,  5441,    1,   1,  TUE,   0,  0,  0),
        new TestCase(2348020.5,  0,  5476,   12,   5,  FRI,   0,  0,  0),
        new TestCase(2366978.5,  0,  5528,   11,   4,  SUN,   0,  0,  0),
        new TestCase(2385648.5,  0,  5579,   12,  11,  MON,   0,  0,  0),
        new TestCase(2392825.5,  0,  5599,    8,  12,  WED,   0,  0,  0),
        new TestCase(2416223.5,  0,  5663,    8,  22,  SUN,   0,  0,  0),
        new TestCase(2425848.5,  0,  5689,   12,  19,  SUN,   0,  0,  0),
        new TestCase(2430266.5,  0,  5702,    1,   8,  MON,   0,  0,  0),
        new TestCase(2430833.5,  0,  5703,    8,  14,  MON,   0,  0,  0),
        new TestCase(2431004.5,  0,  5704,    1,   8,  THU,   0,  0,  0),
        new TestCase(2448698.5,  0,  5752,    7,  12,  TUE,   0,  0,  0),
        new TestCase(2450138.5,  0,  5756,    7,   5,  SUN,   0,  0,  0),
        new TestCase(2465737.5,  0,  5799,    2,  12,  WED,   0,  0,  0),
        new TestCase(2486076.5,  0,  5854,   12,   5,  SUN,   0,  0,  0),

        // Additional test cases for bugs found during development
        //           G.YY/MM/DD  Era  Year  Month Day  WkDay Hour Min Sec
        new TestCase(1013, 9, 8, 0,  4774,    1,   1,  TUE,   0,  0,  0),
        new TestCase(1239, 9, 1, 0,  5000,    1,   1,  THU,   0,  0,  0),
        new TestCase(1240, 9,18, 0,  5001,    1,   1,  TUE,   0,  0,  0),

        // Test cases taken from a table of 14 "year types" in the Help file
        // of the application "Hebrew Calendar"
        new TestCase(2456187.5,  0,  5773,    1,   1,  MON,   0,  0,  0),
        new TestCase(2459111.5,  0,  5781,    1,   1,  SAT,   0,  0,  0),
        new TestCase(2453647.5,  0,  5766,    1,   1,  TUE,   0,  0,  0),
        new TestCase(2462035.5,  0,  5789,    1,   1,  THU,   0,  0,  0),
        new TestCase(2458756.5,  0,  5780,    1,   1,  MON,   0,  0,  0),
        new TestCase(2460586.5,  0,  5785,    1,   1,  THU,   0,  0,  0),
        new TestCase(2463864.5,  0,  5794,    1,   1,  SAT,   0,  0,  0),
        new TestCase(2463481.5,  0,  5793,    1,   1,  MON,   0,  0,  0),
        new TestCase(2470421.5,  0,  5812,    1,   1,  THU,   0,  0,  0),
        new TestCase(2460203.5,  0,  5784,    1,   1,  SAT,   0,  0,  0),
        new TestCase(2459464.5,  0,  5782,    1,   1,  TUE,   0,  0,  0),
        new TestCase(2467142.5,  0,  5803,    1,   1,  MON,   0,  0,  0),
        new TestCase(2455448.5,  0,  5771,    1,   1,  THU,   0,  0,  0),
        new TestCase(2487223.5,  0,  5858,    1,   1,  SAT,   0,  0,  0),
    };
    
    
};