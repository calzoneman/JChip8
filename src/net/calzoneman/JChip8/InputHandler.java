package net.calzoneman.JChip8;

/******************************************************************************
 * JChip8 - A free assembler, disassembler, and emulator for the Chip8        *
 * by Calvin "calzoneman" Montgomery                                          *
 ******************************************************************************
 *                                                                            *
 * This work is licensed under the Creative Commons Attribution 3.0 Unported  *
 * License. To view a copy of this license, visit                             *
 * http://creativecommons.org/licenses/by/3.0/ or send a letter to            *
 * Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, *
 *  94041, USA.                                                               *
 *                                                                            *
 ******************************************************************************/

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

public class InputHandler implements KeyListener {
	
	public static final int NUM_KEYS = 256;
	
	private boolean[] keyStates;
	private boolean[] c8KeyStates;
	private HashMap<Integer, Integer> keymap;
	
	private boolean waiting = false;
	private int c8WaitKey = 0;
	
	public InputHandler() {
		this.keyStates = new boolean[NUM_KEYS];
		this.c8KeyStates = new boolean[16];
		this.keymap = new HashMap<Integer, Integer>();
		
		keymap.put(KeyEvent.VK_6, 0x1);
		keymap.put(KeyEvent.VK_7, 0x2);
		keymap.put(KeyEvent.VK_8, 0x3);
		keymap.put(KeyEvent.VK_9, 0xC);
		
		keymap.put(KeyEvent.VK_Y, 0x4);
		keymap.put(KeyEvent.VK_U, 0x5);
		keymap.put(KeyEvent.VK_I, 0x6);
		keymap.put(KeyEvent.VK_O, 0xD);
		
		keymap.put(KeyEvent.VK_H, 0x7);
		keymap.put(KeyEvent.VK_J, 0x8);
		keymap.put(KeyEvent.VK_K, 0x9);
		keymap.put(KeyEvent.VK_L, 0xE);
		
		keymap.put(KeyEvent.VK_N, 0xA);
		keymap.put(KeyEvent.VK_M, 0x0);
		keymap.put(KeyEvent.VK_COMMA, 0xB);
		keymap.put(KeyEvent.VK_PERIOD, 0xF);
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		keyStates[e.getKeyCode()] = true;
		if(keymap.containsKey(e.getKeyCode())) {
			c8KeyStates[keymap.get(e.getKeyCode())] = true;
			if(waiting) {
				c8WaitKey = keymap.get(e.getKeyCode());
				waiting = false;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		keyStates[e.getKeyCode()] = false;
		if(keymap.containsKey(e.getKeyCode())) {
			c8KeyStates[keymap.get(e.getKeyCode())] = false;
		}
	}
	
	public boolean keyPressed(int key) {
		if(key >= 0 && key < NUM_KEYS) {
			return keyStates[key];
		}
		return false;
	}
	
	public boolean c8KeyPressed(int key) {
		if(key >= 0 && key < 16) {
			return c8KeyStates[key];
		}
		return false;
	}
	
	public int waitForC8Key() {
		waiting = true;
		while(waiting);
		return c8WaitKey;
	}

}
