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

import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;

import org.jbox2d.common.Vec2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ubc.swim.tests.SwimTest;

/**
 * Controls GUI update loop for Swimmer App. Modeled on
 * org.jbox2d.testbed.framework.TestbedController by Daniel Murphy
 * 
 * @author Ben Humberston
 * 
 */
public class SwimController implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(SwimController.class);

	public static final int DEFAULT_FPS = 60;

	private SwimTest currTest = null;
	private SwimTest nextTest = null;

	private long startTime;
	private long frameCount;
	private int targetFrameRate;
	private float frameRate = 0;
	private boolean animating = false;
	private Thread animator;

	private final SwimModel model;
	private final SwimPanel panel;

	public SwimController(SwimModel argModel, SwimWorldPanel argPanel) {
		model = argModel;
		setFrameRate(DEFAULT_FPS);
		panel = argPanel;
		animator = new Thread(this, "SwimController");
		addListeners();
	}

	private void addListeners() {
		// time for our controlling
		model.addTestChangeListener(new SwimModel.TestChangedListener() {
			@Override
			public void testChanged(SwimTest argTest, int argIndex) {
				nextTest = argTest;
				panel.grabFocus();
			}
		});
		panel.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyReleased(KeyEvent e) {
				char key = e.getKeyChar();
				int code = e.getKeyCode();
				if (key != KeyEvent.CHAR_UNDEFINED) {
					model.getKeys()[key] = false;
				}
				model.getCodedKeys()[code] = false;
				if (model.getCurrTest() != null) {
					model.getCurrTest().queueKeyReleased(key, code);
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				char key = e.getKeyChar();
				int code = e.getKeyCode();
				if (key != KeyEvent.CHAR_UNDEFINED) {
					model.getKeys()[key] = true;
				}
				model.getCodedKeys()[code] = true;

				if (key == ' ' && model.getCurrTest() != null) {
					model.getCurrTest().lanchBomb();
				} else if (key == '[') {
					lastTest();
				} else if (key == ']') {
					nextTest();
				} else if (key == 'r') {
					resetTest();
				} else if (model.getCurrTest() != null) {
					model.getCurrTest().queueKeyPressed(key, code);
				}
			}
		});

		panel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (model.getCurrTest() != null) {
					Vec2 pos = new Vec2(e.getX(), e.getY());
					model.getDebugDraw().getScreenToWorldToOut(pos, pos);
					model.getCurrTest().queueMouseUp(pos);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				panel.grabFocus();
				if (model.getCurrTest() != null) {
					Vec2 pos = new Vec2(e.getX(), e.getY());
					if (e.getButton() == MouseEvent.BUTTON1) {
						model.getDebugDraw().getScreenToWorldToOut(pos, pos);
						if (model.getCodedKeys()[KeyEvent.VK_SHIFT]) {
							model.getCurrTest().queueShiftMouseDown(pos);
						}
						else if (model.getCodedKeys()[KeyEvent.VK_CONTROL]) {
							model.getCurrTest().queueCtrlMouseDown(pos);
						}
						else {
							model.getCurrTest().queueMouseDown(pos);
						}
					}
				}
			}
		});

		panel.addMouseMotionListener(new MouseMotionListener() {
			final Vec2 posDif = new Vec2();
			final Vec2 pos = new Vec2();
			final Vec2 pos2 = new Vec2();

			public void mouseDragged(MouseEvent e) {
				pos.set(e.getX(), e.getY());

				if (e.getButton() == MouseEvent.BUTTON3) {
					posDif.set(model.getMouse());
					model.setMouse(pos);
					posDif.subLocal(pos);
					model.getDebugDraw().getViewportTranform().getScreenVectorToWorld(posDif, posDif);
					model.getDebugDraw().getViewportTranform().getCenter().addLocal(posDif);
					if (model.getCurrTest() != null) {
						model.getCurrTest().setCachedCameraPos(model.getDebugDraw().getViewportTranform().getCenter());
					}
				}
				if (model.getCurrTest() != null) {
					model.setMouse(pos);
					model.getDebugDraw().getScreenToWorldToOut(pos, pos);
					model.getCurrTest().queueMouseMove(pos);
				}
			}

			@Override
			public void mouseMoved(MouseEvent e) {
				pos2.set(e.getX(), e.getY());
				model.setMouse(pos2);
				if (model.getCurrTest() != null) {
					model.getDebugDraw().getScreenToWorldToOut(pos2, pos2);
					model.getCurrTest().queueMouseMove(pos2);
				}
			}
		});
	}

	protected void loopInit() {
		panel.grabFocus();

		if (currTest != null) {
			currTest.init(model);
		}
	}

	protected void update() {
		if (currTest != null) {
			currTest.update();
		}
	}

	public void nextTest() {
		int index = model.getCurrTestIndex() + 1;
		index %= model.getTestsSize();

		while (!model.isTestAt(index) && index < model.getTestsSize() - 1) {
			index++;
		}
		if (model.isTestAt(index)) {
			model.setCurrTestIndex(index);
		}
	}

	public void resetTest() {
		model.getCurrTest().reset();
	}

	public void lastTest() {
		int index = model.getCurrTestIndex() - 1;
		index = (index < 0) ? index + model.getTestsSize() : index;

		while (!model.isTestAt(index) && index > 0) {
			index--;
		}

		if (model.isTestAt(index)) {
			model.setCurrTestIndex(index);
		}
	}

	public void playTest(int argIndex) {
		if (argIndex == -1) {
			return;
		}
		while (!model.isTestAt(argIndex)) {
			if (argIndex + 1 < model.getTestsSize()) {
				argIndex++;
			} else {
				return;
			}
		}
		model.setCurrTestIndex(argIndex);
	}

	public void setFrameRate(int fps) {
		if (fps <= 0) {
			throw new IllegalArgumentException("Fps cannot be less than or equal to zero");
		}
		targetFrameRate = fps;
		frameRate = fps;
	}

	public int getFrameRate() {
		return targetFrameRate;
	}

	public float getCalculatedFrameRate() {
		return frameRate;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getFrameCount() {
		return frameCount;
	}

	public boolean isAnimating() {
		return animating;
	}

	public synchronized void start() {
		if (animating != true) {
			frameCount = 0;
			animator.start();
		} else {
			log.warn("Animation is already animating.");
		}
	}

	public synchronized void stop() {
		animating = false;
	}

	public void run() {
		long beforeTime, afterTime, updateTime, timeDiff, sleepTime, timeSpent;
		float timeInSecs;
		beforeTime = startTime = updateTime = System.nanoTime();
		sleepTime = 0;

		animating = true;
		loopInit();
		while (animating) {

			if (nextTest != null) {
				if (currTest != null) {
					currTest.exit();
				}
				currTest = nextTest;
				currTest.init(model);
				nextTest = null;
			}

			timeSpent = beforeTime - updateTime;
			if (timeSpent > 0) {
				timeInSecs = timeSpent * 1.0f / 1000000000.0f;
				updateTime = System.nanoTime();
				frameRate = (frameRate * 0.9f) + (1.0f / timeInSecs) * 0.1f;
				model.setCalculatedFps(frameRate);
			} else {
				updateTime = System.nanoTime();
			}

			panel.render();
			update();
			panel.paintScreen();
			
			//Write current world image to disk if recording
			//Probably not the best place for this, but ....
			if (model.getSettings().recording && currTest != null) {
				Image img = panel.getBufferedImage();
				
				//Create filename
				String testID = currTest.getTestID();
				DecimalFormat leadingZeroesFormat = new java.text.DecimalFormat("0000");
				long testFrameCount = currTest.getFrameCount();
				String frameNumSuffix = leadingZeroesFormat.format(new Long(testFrameCount));
				String fileName = testID + frameNumSuffix;
				String fullFileName = "./images/" + testID + "/" + fileName + ".png";
				
				if (img != null) {
					try {
						BufferedImage bi = getBufferedImage(img);
						
						File imgFile = new File(fullFileName);
						
						Iterator<ImageWriter> imageWriters = ImageIO.getImageWritersByFormatName("png");
						if ( imageWriters.hasNext() ) {
						    ImageWriter writer = (ImageWriter)imageWriters.next();
						    ImageOutputStream stream = ImageIO.createImageOutputStream(imgFile);
						    writer.setOutput(stream);
						    ImageWriteParam param = writer.getDefaultWriteParam();
						    writer.write(null, new IIOImage(bi, null, null), param);
						    
						    stream.flush();
						    stream.close();
						}
					} catch (Exception exception) {
				      exception.printStackTrace();
				    }
				}
			}
			
			frameCount++;

			afterTime = System.nanoTime();

			timeDiff = afterTime - beforeTime;
			sleepTime = (1000000000 / targetFrameRate - timeDiff) / 1000000;
			if (sleepTime > 0) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException ex) {
				}
			}

			beforeTime = System.nanoTime();
		} // end of run loop
	}
	
	//////////////////////////////////////// OUTPUT RENDERING STUFF BELOW ////////////////////////////////////////
	
	/**
	 * Returns BufferedImage with contents of given image
	 * Source: http://www.exampledepot.com/egs/java.awt.image/image2buf.html
	 * @param image
	 * @return
	 */
	private static BufferedImage getBufferedImage(Image image) {
	    if (image instanceof BufferedImage) {
	        return (BufferedImage)image;
	    }

	    // This code ensures that all the pixels in the image are loaded
	    image = new ImageIcon(image).getImage();

	    // Determine if the image has transparent pixels; for this method's
	    // implementation, see Determining If an Image Has Transparent Pixels
	    boolean hasAlpha = hasAlpha(image);

	    // Create a buffered image with a format that's compatible with the screen
	    BufferedImage bimage = null;
	    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    try {
	        // Determine the type of transparency of the new buffered image
	        int transparency = Transparency.OPAQUE;
	        if (hasAlpha) {
	            transparency = Transparency.BITMASK;
	        }

	        // Create the buffered image
	        GraphicsDevice gs = ge.getDefaultScreenDevice();
	        GraphicsConfiguration gc = gs.getDefaultConfiguration();
	        bimage = gc.createCompatibleImage(
	            image.getWidth(null), image.getHeight(null), transparency);
	    } catch (HeadlessException e) {
	        // The system does not have a screen
	    }

	    if (bimage == null) {
	        // Create a buffered image using the default color model
	        int type = BufferedImage.TYPE_INT_RGB;
	        if (hasAlpha) {
	            type = BufferedImage.TYPE_INT_ARGB;
	        }
	        bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
	    }

	    // Copy image to buffered image
	    Graphics g = bimage.createGraphics();

	    // Paint the image onto the buffered image
	    g.drawImage(image, 0, 0, null);
	    g.dispose();

	    return bimage;
	}
	
	/**
	 * Returns true if the specified image has transparent pixels
	 * Source: http://www.exampledepot.com/egs/java.awt.image/HasAlpha.html
	 * @param image
	 * @return
	 */
	private static boolean hasAlpha(Image image) {
	    // If buffered image, the color model is readily available
	    if (image instanceof BufferedImage) {
	        BufferedImage bimage = (BufferedImage)image;
	        return bimage.getColorModel().hasAlpha();
	    }

	    // Use a pixel grabber to retrieve the image's color model;
	    // grabbing a single pixel is usually sufficient
	     PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
	    try {
	        pg.grabPixels();
	    } catch (InterruptedException e) {
	    }

	    // Get the image's color model
	    ColorModel cm = pg.getColorModel();
	    return cm.hasAlpha();
	}
}
