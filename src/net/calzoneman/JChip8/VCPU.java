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

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Random;

public class VCPU extends Canvas {
	private static final long serialVersionUID = -7577044753877808534L;

	private byte[] VREGISTERS = new byte[16]; // Represents registers V0-VF
	
	private short I = 0; // Memory pointer register
	private short PC = 0x200; // Program counter (program code is addressed at 0x200)
	private short SP = 0; // Stack pointer
	private byte DT = 0; // Delay timer
	private byte ST = 0; // Sound timer (sound not implemented yet)
	
	private short[] STACK = new short[16];
	private byte[] memory = new byte[4096];
	private byte[] videomem = new byte[64 * 32];
	private boolean needsRedraw = true;
	
	private Random random = new Random();
	private InputHandler input = new InputHandler();
	
	private boolean keepRunning = true;
	
	public VCPU(byte[] rom) {
		// Load the ROM
		System.arraycopy(rom, 0, memory, 0x200, rom.length);
		
		// Setup Canvas stuff
		setMinimumSize(new Dimension(256, 128));
		setPreferredSize(new Dimension(256, 128));
		setMaximumSize(new Dimension(256, 128));
		addKeyListener(input);
		setVisible(true);
		
		// Load default font into memory
		byte[] font = { (byte) 0xF0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xF0 , // 0
						(byte) 0x20, (byte) 0x60, (byte) 0x20, (byte) 0x20, (byte) 0x70 , // 1
						(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x80, (byte) 0xF0 , // 2
						(byte) 0xF0, (byte) 0x10, (byte) 0xF0, (byte) 0x10, (byte) 0xF0 , // 3
						(byte) 0x90, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0x10 , // 4
						(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x10, (byte) 0xF0 , // 5
						(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x90, (byte) 0xF0 , // 6
						(byte) 0xF0, (byte) 0x10, (byte) 0x20, (byte) 0x40, (byte) 0x40 , // 7
						(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0xF0 , // 8
						(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x10, (byte) 0xF0 , // 9
						(byte) 0xF0, (byte) 0x90, (byte) 0xF0, (byte) 0x90, (byte) 0x90 , // A
						(byte) 0xE0, (byte) 0x90, (byte) 0xE0, (byte) 0x90, (byte) 0xE0 , // B
						(byte) 0xF0, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0xF0 , // C
						(byte) 0xE0, (byte) 0x90, (byte) 0x90, (byte) 0x90, (byte) 0xE0 , // D
						(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0xF0 , // E
						(byte) 0xF0, (byte) 0x80, (byte) 0xF0, (byte) 0x80, (byte) 0x80 }; // F
		
		System.arraycopy(font, 0, memory, 0, font.length);
	}
	
	@Override
	public void paint(Graphics g) {
		for(int i = 0; i < 64; i++) {
			for(int j = 0; j < 32; j++) {
				if(videomem[j * 64 + i] == 1) {
					g.setColor(Color.white);
				}
				else {
					g.setColor(Color.black);
				}
				g.fillRect(i*4, j*4, 4, 4);
			}
		}
	}
	
	@Override
	public void update(Graphics g) {
		paint(g);
	}
	
	/**
	 * run() should assume the VCPU is completely ready to execute code.  Value resetting should be done in reset()
	 */
	public void run() {
		
		double _60Hz = 1000000000.0/60;
		long lastDec = System.nanoTime();
		
		while(PC+1 < memory.length && keepRunning) {

			if(needsRedraw) {
				paint(getGraphics());
				needsRedraw = false;
			}
			
			if(input.keyPressed(KeyEvent.VK_R)) reset();
			
			// DT and ST decrease at 60Hz
			if(System.nanoTime() > lastDec + _60Hz) {
				if(unsign(DT) > 0) DT--;
				if(unsign(ST) > 0) ST--;
				lastDec = System.nanoTime();
			}
			
			byte[] nyb = getNybbles(bytesToShort(memory[PC], memory[PC+1]));
			PC += 2;
			short addr = nybblesToAddress(nyb[1], nyb[2], nyb[3]); // For opcodes with a 3-nybble address
			byte arg = (byte)((nyb[2] << 4) | nyb[3]); // For opcodes with a 1-byte argument
			
			switch(nyb[0]) {
				case 0x0:
					if(arg == (byte)0xE0) {
						opCls();
					}
					else if(arg == (byte)0xEE) {
						opRet();
					}
					break;
				case 0x1:
					opJp(addr);
					break;
				case 0x2:
					opCall(addr);
					break;
				case 0x3:
					opSEConst(nyb[1], arg);
					break;
				case 0x4:
					opSNEConst(nyb[1], arg);
					break;
				case 0x5:
					opSEReg(nyb[1], nyb[2]);
					break;
				case 0x6:
					opLd(nyb[1], arg);
					break;
				case 0x7:
					opAdd(nyb[1], arg);
					break;
				case 0x8:
					switch(nyb[3]) {
						case 0x0:
							opLdReg(nyb[1], nyb[2]);
							break;
						case 0x1:
							opOr(nyb[1], nyb[2]);
							break;
						case 0x2:
							opAnd(nyb[1], nyb[2]);
							break;
						case 0x3:
							opXor(nyb[1], nyb[2]);
							break;
						case 0x4:
							opAddReg(nyb[1], nyb[2]);
							break;
						case 0x5:
							opSub(nyb[1], nyb[2]);
							break;
						case 0x6:
							opShr(nyb[1]);
							break;
						case 0x7:
							opSubN(nyb[1], nyb[2]);
							break;
						case 0xE:
							opShl(nyb[1]);
							break;
						default: break;
					}
					break;
				case 0x9:
					opSNEReg(nyb[1], nyb[2]);
					break;
				case 0xA:
					opLdI(addr);
					break;
				case 0xB:
					opJpV0(addr);
					break;
				case 0xC:
					opRnd(nyb[1], arg);
					break;
				case 0xD:
					opDraw(nyb[1], nyb[2], nyb[3]);
					break;
				case 0xE:
					if(arg == (byte)0x9E) {
						opSkipKey(nyb[1]);
					}
					else if(arg == (byte)0xA1) {
						opSkipNotKey(nyb[1]);
					}
					break;
				case 0xF:
					switch(arg) {
						case 0x07:
							opLdFromDT(nyb[1]);
							break;
						case 0x0A:
							opWaitKey(nyb[1]);
							break;
						case 0x15:
							opLdDT(nyb[1]);
							break;
						case 0x18:
							opLdST(nyb[1]);
							break;
						case 0x1E:
							opAddI(nyb[1]);
							break;
						case 0x29:
							opLdChar(nyb[1]);
							break;
						case 0x33:
							opLdBcd(nyb[1]);
							break;
						case 0x55:
							opStoAllVx(nyb[1]);
							break;
						case 0x65:
							opLdAllVx(nyb[1]);
							break;
						default:
							break;
					}
					break;
				default:
					System.out.println("Unknown Opcode: " + nyb[0]);
					break;
			}
		}
		
		System.out.println("Execution ended.");
		printState();
	}
	
	public void reset() {
		VREGISTERS = new byte[16];
		videomem = new byte[64*32];
		STACK = new short[16];
		needsRedraw = true;
		I = 0;
		PC = 0x200;
		SP = 0;
		DT = 0;
		ST = 0;
		System.out.println("Virtual Machine Reset");
	}
	
	public void die(String message) {
		keepRunning = false;
		System.out.println(message);
	}
	
	// 0x00E0
	private void opCls() {
		for(int i = 0; i < 64*32; i++) {
			videomem[i] = 0;
		}
	}
	
	// 0x00EE
	private void opRet() {
		PC = STACK[SP];
		STACK[SP] = 0;
		if(SP - 1 < 0) {
			die("[Chip8] Stack underflow at $0x" + Integer.toHexString(PC));
			return;
		}
		SP--;
	}
	
	// 0x1NNN
	private void opJp(short addr) {
		PC = addr;	
	}
	
	// 0x2NNN
	private void opCall(short addr) {
		if(SP + 1 >= STACK.length) {
			die("[Chip8] Stack overflow at $0x" + Integer.toHexString(PC));
			return;
		}
		SP++;
		STACK[SP] = PC;
		PC = addr;
		
	}
	
	// 0x3XKK
	private void opSEConst(byte x, byte arg) {
		if(VREGISTERS[x] == arg) {
			PC += 2;
		}
	}
	
	// 0x4XKK
	private void opSNEConst(byte x, byte arg) {
		if(VREGISTERS[x] != arg) PC += 2;		
	}
	
	// 0x5XY0
	private void opSEReg(byte x, byte y) {
		if(VREGISTERS[x] == VREGISTERS[y]) PC += 2;		
	}
	
	// 0x6XKK
	private void opLd(byte x, byte arg) {
		VREGISTERS[x] = arg;		
	}
	
	// 0x7XKK
	private void opAdd(byte x, byte arg) {
		VREGISTERS[x] += arg;
	}
	
	// 0x8XY0
	private void opLdReg(byte x, byte y) {
		VREGISTERS[x] = VREGISTERS[y];
	}
	
	// 0x8XY1
	private void opOr(byte x, byte y) {
		VREGISTERS[x] |= VREGISTERS[y];
	}
	
	// 0x8XY2
	private void opAnd(byte x, byte y) {
		VREGISTERS[x] &= VREGISTERS[y];
	}
	
	// 0x8XY3
	private void opXor(byte x, byte y) {
		VREGISTERS[x] ^= VREGISTERS[y];
	}
	
	// 0x8XY4
	private void opAddReg(byte x, byte y) {
		if(unsign(VREGISTERS[x]) + unsign(VREGISTERS[y]) > 255) VREGISTERS[0xF] = 0x1;
		else VREGISTERS[0xF] = 0;
		VREGISTERS[x] += VREGISTERS[y];
	}
	
	// 0x8XY5
	private void opSub(byte x, byte y) {
		if(unsign(VREGISTERS[x]) < unsign(VREGISTERS[y])) VREGISTERS[0xF] = 0x1;
		else VREGISTERS[0xF] = 0;
		VREGISTERS[x] -= VREGISTERS[y];
	}
	
	// 0x8XY6
	private void opShr(byte x) {
		VREGISTERS[0xF] = (byte) (VREGISTERS[x] & 0xFE);
		VREGISTERS[x] >>= 1;
	}
	
	// 0x8XY7
	private void opSubN(byte x, byte y) {
		if(unsign(VREGISTERS[x]) > unsign(VREGISTERS[y])) VREGISTERS[0xF] = 1;
		else VREGISTERS[0xF] = 0;
		VREGISTERS[x] = (byte) (VREGISTERS[y] - VREGISTERS[x]);
	}
	
	// 0x8XYE
	private void opShl(byte x) {
		VREGISTERS[0xF] = (byte)((VREGISTERS[x] & 0x7F) >> 7);
		VREGISTERS[x] <<= 1;
	}
	
	// 0x9XY0
	private void opSNEReg(byte x, byte y) {
		if(VREGISTERS[x] != VREGISTERS[y]) PC += 2;		
	}
	
	// 0xANNN
	private void opLdI(short addr) {
		I = addr;
	}
	
	// 0xBNNN
	private void opJpV0(short addr) {
		opJp((short) (addr + VREGISTERS[0]));
	}
	
	// 0xCXKK
	private void opRnd(byte x, byte arg) {
		byte rand = (byte)(random.nextInt(255) & arg);
		VREGISTERS[x] = rand;
	}
	
	// 0xDXYN
	private void opDraw(byte x, byte y, byte height) {
		int startX = unsign(VREGISTERS[x]);
		int startY = unsign(VREGISTERS[y]);
				
		int pX = startX;
		int pY = startY;
		
		for(int j = 0; j < height; j++) {
			for(int i = 0; i < 8; i++) {
				if(pX >= 64) pX = 0;
				if(pY >= 32) pY = 0;
				if(I + j >= memory.length || (pY * 64 + pX >= videomem.length)) {
					die("[Chip8] Segmentation Fault at $0x" + Integer.toHexString(PC-2));
					return;
				}
				int newPx = getBit(memory[I + j], i);
				if(newPx == 1) {
					if(videomem[pY * 64 + pX] == 1) VREGISTERS[0xF] = 1;
					else VREGISTERS[0xF] = 0;
					videomem[pY * 64 + pX] ^= 1;
				}
				pX++;
			}
			pX = startX;
			pY++;
		}
		needsRedraw = true;
	}
	
	// 0xEX9E
	private void opSkipKey(byte x) {
		if(input.c8KeyPressed(x)) {
			PC += 2;
		}
	}
	
	// 0xEXA1
	private void opSkipNotKey(byte x) {
		if(!input.c8KeyPressed(x)) {
			PC += 2;
		}
	}
	
	// 0xFX07
	private void opLdFromDT(byte x) {
		VREGISTERS[x] = DT;
	}
	
	// 0xFX0A
	private void opWaitKey(byte x) {
		VREGISTERS[x] = (byte)input.waitForC8Key();
	}
	
	// 0xFX15
	private void opLdDT(byte x) {
		DT = VREGISTERS[x];
	}
	
	// 0xFX18
	private void opLdST(byte x) {
		ST = VREGISTERS[x];
	}
	
	// 0xFX1E
	private void opAddI(byte x) {
		I += VREGISTERS[x];
	}
	
	// 0xFX29
	private void opLdChar(byte x) {
		I = (short) (VREGISTERS[x] * 5);
	}
	
	// 0xFX33
	private void opLdBcd(byte x) {
		byte hundred = (byte)(VREGISTERS[x] / 100);
		byte ten = (byte)((VREGISTERS[x] % 100) / 10);
		byte one = (byte)(VREGISTERS[x] % 10);
		if(I >= memory.length || I+1 >= memory.length || I+2 > memory.length) {
			die("[Chip8] Segmentation Fault at $0x" + Integer.toHexString(PC-2));
			return;
		}
		memory[I] = hundred;
		memory[I+1] = ten;
		memory[I+2] = one;
	}
	
	// 0xFX55
	private void opStoAllVx(byte x) {
		for(int i = 0; i <= x; i++) {
			if(I + i >= memory.length) {
				die("[Chip8] Segmentation Fault at $0x" + Integer.toHexString(PC-2));
				return;
			}
			memory[I + i] = VREGISTERS[x];
		}
	}
	
	// 0xFX65
	private void opLdAllVx(byte x) {
		for(int i = 0; i <= x; i++) {
			if(I + i >= memory.length) {
				die("[Chip8] Segmentation Fault at $0x" + Integer.toHexString(PC-2));
				return;
			}
			VREGISTERS[i] = memory[I + i];
		}
	}

	public static byte[] getNybbles(short input) {
		byte n1 = (byte)((input & 0xF000) >> 12);
		byte n2 = (byte)((input & 0x0F00) >> 8);
		byte n3 = (byte)((input & 0x00F0) >> 4);
		byte n4 = (byte)(input & 0x000F);
		return new byte[] {n1, n2, n3, n4};
	}
	
	public static short nybblesToAddress(byte n1, byte n2, byte n3) {
		return (short)((n1 << 8) | ((n2 << 4) | n3)); // For opcodes of the form Onnn
	}
	
	public static int unsign(byte b) {
		if(b < 0) return 256+b;
		else return b;
	}
	
	public static short bytesToShort(byte b1, byte b2) {
		return (short)((b1 << 8) | (b2 & 0xFF));
	}
	
	public static byte getBit(byte input, int index) {
		return (byte)((input >> (7-index)) & 0x01);
	}
	
	public void printState() {
		System.out.println("---[ VCPU State ]---------");
		System.out.println("PC: " + PC);
		System.out.println("SP: " + SP);
		System.out.println("I : " + I);
		System.out.println("DT: " + DT);
		System.out.println("ST: " + ST);
		for(int i = 0; i <= 0xF; i++) {
			System.out.println("V" + Integer.toHexString(i) + ": " + VREGISTERS[i]);
		}
		System.out.println("--------------------------");
		System.out.println();
	}
}
