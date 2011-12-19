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
/**
 * Created at 3:13:48 AM Jul 17, 2010
 */
package ubc.swim.gui;

import java.awt.Image;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import org.jbox2d.callbacks.DebugDraw;

/**
 * A GUI panel in the swimmer app. Adapted from org.jbox2d.testbed.framework.TestbedPanel by Daniel Murphy
 * 
 * @author Ben Humberston
 */
public interface SwimPanel {

  /**
   * Adds a key listener
   * 
   * @param argListener
   */
  public void addKeyListener(KeyListener argListener);

  /**
   * Adds a mouse listener
   * 
   * @param argListener
   */
  public void addMouseListener(MouseListener argListener);

  /**
   * Adds a mouse motion listener
   * 
   * @param argListener
   */
  public void addMouseMotionListener(MouseMotionListener argListener);

  public void grabFocus();

  /**
   * Gets the display-specific debug draw
   * @return
   */
  public DebugDraw getDebugDraw();
  
  /**
   * Returns current buffered image (may be null if recently resized window)
   * @return
   */
  public Image getBufferedImage();

  /**
   * Renders the world
   */
  public void render();

  /**
   * Paints the rendered world to the screen
   */
  public void paintScreen();
}
