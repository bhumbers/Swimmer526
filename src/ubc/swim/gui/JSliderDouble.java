package ubc.swim.gui;

import javax.swing.JSlider;

/**
 * A JSlider that allows non-integer double values
 * 
 * Source: http://stackoverflow.com/questions/1548606/java-link-jslider-and-jtextfield-for-float-value
 * 
 * @author Ben Humberston
 *
 */
class JSliderDouble extends JSlider {
	private static final long serialVersionUID = 1L;
	
	protected double rangeScale;

    public JSliderDouble(double min, double max, double value, int numStops) {
    	//Convert to integer ranges
        super((int) (min * numStops / (max - min)), (int) (max * numStops / (max - min)), (int) (value * numStops / (max - min)));
        
        rangeScale = numStops / (max - min);
        
//        //Update labels to show double values
//        Enumeration e = this.getLabelTable().keys();
//
//        while (e.hasMoreElements()) {
//            Integer i = (Integer) e.nextElement();
//            JLabel label = (JLabel) this.getLabelTable().get(i);
//            label.setText(String.valueOf(i / rangeScale));          
//        }
    }

    public double getScaledValue() {
        return ((double)super.getValue()) / this.rangeScale;
    }
}