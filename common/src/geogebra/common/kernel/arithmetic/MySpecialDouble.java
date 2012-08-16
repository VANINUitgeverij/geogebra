/* 
GeoGebra - Dynamic Mathematics for Everyone
http://www.geogebra.org

This file is part of GeoGebra.

This program is free software; you can redistribute it and/or modify it 
under the terms of the GNU General Public License as published by 
the Free Software Foundation.

 */

package geogebra.common.kernel.arithmetic;

import geogebra.common.kernel.Kernel;
import geogebra.common.kernel.StringTemplate;
import geogebra.common.kernel.arithmetic.ExpressionNodeConstants.StringType;
import geogebra.common.main.App;
import geogebra.common.util.StringUtil;
import geogebra.common.util.Unicode;

import java.math.BigDecimal;

/**
 * MyDouble that returns a certain string in toString(). This is used for
 * example for the degree sign in geogebra.parser.Parser.jj: new
 * MySpecialDouble(kernel, Math.PI / 180.0d, "\u00b0" );
 * 
 * @author Markus Hohenwarter
 */
public class MySpecialDouble extends MyDouble {

	private String strToString;
	private final String originalString;
	private boolean keepOriginalString;
	private boolean isLetterConstant; // for Pi, Euler, or Degree constant
	private boolean scientificNotation = false;

	private static MySpecialDouble eulerConstant;

	/**
	 * @param kernel kernel
	 * @param val value
	 * @param str string representation 
	 */
	public MySpecialDouble(Kernel kernel, double val, String str) {
		super(kernel, val);
		originalString = str;
		strToString = str;
		if(strToString == null)
			strToString = "0";
		// check if this is a letter constant, e.g. Pi or Euler number
		char firstChar = strToString.charAt(0);
		isLetterConstant = StringUtil.isLetter(firstChar)
				|| firstChar == Unicode.degreeChar;
		scientificNotation = strToString.indexOf("E") > 0;
		keepOriginalString = !isLetterConstant
				&& ( scientificNotation
						|| Double.isInfinite(val));

		if (keepOriginalString) {
			BigDecimal bd = new BigDecimal(strToString);
			// avoid E notation for small values
			if (val >= 10E-3 && val < 10E7) {
				// from GeoGebraCAS we get a String using 15 significant figures
				// like 3.14160000000000
				// let's remove trailing zeros
				bd = bd.stripTrailingZeros();
				// no E notation
				strToString = bd.toPlainString();
			} else {
				// use E notation if necessary
				strToString = bd.toString();
				scientificNotation = strToString.indexOf("E") > 0;
			}
		}
	}

	/**
	 * Copy constructor.
	 * @param sd special double to copy
	 */
	public MySpecialDouble(MySpecialDouble sd) {
		super(sd);
		originalString = sd.strToString;
		strToString = sd.strToString;
		keepOriginalString = sd.keepOriginalString;
		isLetterConstant = sd.isLetterConstant; // for Pi, Euler, or Degree
												// constant
		scientificNotation = sd.scientificNotation;
	}

	@Override
	public ExpressionValue deepCopy(Kernel kernel1) {
		if (isEulerConstant())
			return getEulerConstant(kernel1);

		MySpecialDouble ret = new MySpecialDouble(this);
		ret.kernel = kernel1;
		return ret;
	}

	/**
	 * Force this number to keep original input 
	 */
	public void setKeepOriginalString() {
		keepOriginalString = true;
	}

	/**
	 * @param kernel kernel
	 * @return E as MySpecialDouble
	 */
	public static MySpecialDouble getEulerConstant(Kernel kernel) {
		if (eulerConstant == null) {
			eulerConstant = new MySpecialDouble(kernel, Math.E,
					Unicode.EULER_STRING);
		}
		return eulerConstant;
	}

	/**
	 * @return true if this equals E (no tolerance)
	 */
	public boolean isEulerConstant() {
		return getDouble() == Math.E;
	}

	@Override
	public String toString(StringTemplate tpl) {
		App.debug(originalString + tpl.allowMoreDigits());
		if (!isLetterConstant) {
			//serializing to CAS -- simply print input
			if(tpl.hasType(StringType.MPREDUCE))
				return originalString;
			//if we are printing result of numeric and user didn't force us to use significant digits
			//print the original string
			if (keepOriginalString || (!tpl.useScientific(kernel.useSignificantFigures) && !strToString.contains("."))
					|| tpl.allowMoreDigits()) {
				if (scientificNotation) {
					// change 5.1E-20 to 5.1*10^(-20) or 5.1 \cdot 10^{-20}
					return kernel.convertScientificNotation(strToString,tpl);
				}
				// keep original string
				return strToString;
			}
			// format double value using kernel settings
			return super.toString(tpl);
		}

		// letter constants for pi, e, or degree character
		StringType printForm = tpl.getStringType();
		switch (printForm) {
		// case JASYMCA:
		case MATH_PIPER:
			char ch = strToString.charAt(0);
			switch (ch) {
			case Unicode.piChar:
				return "Pi";
			case Unicode.degreeChar:
				return "Pi/180";
			}
			break;

		case MAXIMA:
			ch = strToString.charAt(0);
			switch (ch) {
			case Unicode.piChar:
				return "%pi";
			case Unicode.degreeChar:
				return "%pi/180";
			case Unicode.eulerChar:
				return "%e";
			}
			break;

		case MPREDUCE:
			ch = strToString.charAt(0);
			switch (ch) {
			case Unicode.piChar:
				return "pi";
			case Unicode.degreeChar:
				return "pi/180";
			case Unicode.eulerChar:
				return "e";
			}
			break;

		case LATEX:
			ch = strToString.charAt(0);
			switch (ch) {
			case Unicode.piChar:
				return "\\pi";
			case Unicode.degreeChar:
				return "^{\\circ}";
			case Unicode.eulerChar:
				return "\\textit{e}";
				//return Unicode.EULER_STRING; 
			}
			break;
		}

		return strToString;
	}

}
