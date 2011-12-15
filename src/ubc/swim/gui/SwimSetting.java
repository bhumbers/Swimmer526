/*******************************************************************************
 * Copyright (c) 2011, Daniel Murphy
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 	* Redistributions of source code must retain the above copyright notice,
 * 	  this list of conditions and the following disclaimer.
 * 	* Redistributions in binary form must reproduce the above copyright notice,
 * 	  this list of conditions and the following disclaimer in the documentation
 * 	  and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package ubc.swim.gui;

/**
 * Defines a single setting used by swimmer GUI Modeled on
 * org.jbox2d.testbed.framework.TestbedSetting by Daniel Murphy
 */
public class SwimSetting {

	/**
	 * Whether the setting effects the engine's behavior or modifies drawing.
	 * 
	 */
	public static enum SettingType {
		DRAWING, ENGINE
	}

	/**
	 * The type of value this setting pertains to
	 */
	public static enum ConstraintType {
		BOOLEAN, RANGE
	}

	public final String name;
	public final SettingType settingsType;
	public final ConstraintType constraintType;
	public boolean enabled;
	public double value;
	public final double min;
	public final double max;
	
	public final int sliderStops; // used to set # of notches on UI slider for floating point settings

	public SwimSetting(String name, SettingType type, boolean enabled) {
		this.name = name;
		this.settingsType = type;
		this.enabled = enabled;
		constraintType = ConstraintType.BOOLEAN;
		min = max = value = 0;
		sliderStops = 2;
	}
	
	/**
	 * Creates a numeric range setting that will use default number of slider stops
	 * @param name
	 * @param type
	 * @param val
	 * @param min
	 * @param max
	 */
	public SwimSetting(String name, SettingType type, double val, double min, double max) {
		this(name, type, val, min, max, (int)(max - min));
	}

	/**
	 * Creates a numeric range setting that will use given number of slider stops
	 * @param name
	 * @param type
	 * @param val
	 * @param min
	 * @param max
	 * @param sliderStops
	 */
	public SwimSetting(String name, SettingType type, double val, double min, double max, int sliderStops) {
		this.name = name;
		this.settingsType = type;
		this.value = val;
		this.min = min;
		this.max = max;
		this.sliderStops = sliderStops;
		constraintType = ConstraintType.RANGE;
		enabled = false;
	}

	/**
	 * Returns value in integer precision
	 * 
	 * @return
	 */
	public int getIntValue() { return (int) value;
	}
}
