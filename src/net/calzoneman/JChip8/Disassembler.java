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

import java.util.ArrayList;

public class Disassembler {
	public static ArrayList<String> disassemble(byte[] rom) {
		ArrayList<String> code = new ArrayList<String>();
		int pc = 0x200;
		boolean keepParsingCode = true;
		
		while(pc - 0x200 + 1 < rom.length) {
			
			if(!keepParsingCode) {
				code.add(format(pc, "DW 0x" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
				pc += 2;
				continue;
			}
			
			byte[] nyb = VCPU.getNybbles(VCPU.bytesToShort(rom[pc-0x200], rom[pc-0x200+1]));
			short addr = VCPU.nybblesToAddress(nyb[1], nyb[2], nyb[3]); // For opcodes with a 3-nybble address
			byte arg = (byte)((nyb[2] << 4) | nyb[3]); // For opcodes with a 1-byte argument
			
			switch(nyb[0]) {
				case 0x0:
					if(arg == (byte)0xE0) {
						code.add(format(pc, "CLS"));
					}
					else if(arg == (byte)0xEE) {
						code.add(format(pc, "RET"));
					}
					else {
						code.add(format(pc, "DW 0x" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
						keepParsingCode = false;
					}
					break;
				case 0x1:
					code.add(format(pc, "JP $0x" + hex(addr)));
					break;
				case 0x2:
					code.add(format(pc, "CALL $0x" + hex(addr)));
					break;
				case 0x3:
					code.add(format(pc, "SE " + regName(nyb[1]) + ", " + arg));
					break;
				case 0x4:
					code.add(format(pc, "SNE " + regName(nyb[1]) + ", " + arg));
					break;
				case 0x5:
					code.add(format(pc, "SE " + regName(nyb[1]) + ", " + regName(nyb[2])));
					break;
				case 0x6:
					code.add(format(pc, "LD " + regName(nyb[1]) + ", " + arg));
					break;
				case 0x7:
					code.add(format(pc, "ADD " + regName(nyb[1]) + ", " + arg));
					break;
				case 0x8:
					switch(nyb[3]) {
						case 0x0:
							code.add(format(pc, "LD " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x1:
							code.add(format(pc, "OR " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x2:
							code.add(format(pc, "AND " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x3:
							code.add(format(pc, "XOR " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x4:
							code.add(format(pc, "ADD " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x5:
							code.add(format(pc, "SUB " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0x6:
							code.add(format(pc, "SHR " + regName(nyb[1])));
							break;
						case 0x7:
							code.add(format(pc, "SUBN " + regName(nyb[1]) + ", " + regName(nyb[2])));
							break;
						case 0xE:
							code.add(format(pc, "SHL " + regName(nyb[1])));
							break;
						default: 
							code.add(format(pc, "DW 0x" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
							keepParsingCode = false;
							break;
					}
					break;
				case 0x9:
					code.add(format(pc, "SNE " + regName(nyb[1]) + ", " + regName(nyb[2])));
					break;
				case 0xA:
					code.add(format(pc, "LD I, $0x" + hex(addr)));
					break;
				case 0xB:
					code.add(format(pc, "JP V0, $0x" + hex(addr)));
					break;
				case 0xC:
					code.add(format(pc, "RND " + regName(nyb[1]) + ", " + arg));
					break;
				case 0xD:
					code.add(format(pc, "DRW " + regName(nyb[1]) + ", " + regName(nyb[2]) + ", " + nyb[3]));
					break;
				case 0xE:
					if(arg == (byte)0x9E) {
						code.add(format(pc, "SKP " + regName(nyb[1])));
					}
					else if(arg == (byte)0xA1) {
						code.add(format(pc, "SKNP " + regName(nyb[1])));
					}
					else {
						code.add(format(pc, "DW $" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
						keepParsingCode = false;
					}
					break;
				case 0xF:
					switch(arg) {
						case 0x07:
							code.add(format(pc, "LD " + regName(nyb[1]) + ", DT"));
							break;
						case 0x0A:
							code.add(format(pc, "LD " + regName(nyb[1]) + ", K"));
							break;
						case 0x15:
							code.add(format(pc, "LD DT, " + regName(nyb[1])));
							break;
						case 0x18:
							code.add(format(pc, "LD ST, " + regName(nyb[1])));
							break;
						case 0x1E:
							code.add(format(pc, "ADD I, " + regName(nyb[1])));
							break;
						case 0x29:
							code.add(format(pc, "LD F, " + regName(nyb[1])));
							break;
						case 0x33:
							code.add(format(pc, "LD B, " + regName(nyb[1])));
							break;
						case 0x55:
							code.add(format(pc, "LD [I], " + regName(nyb[1])));
							break;
						case 0x65:
							code.add(format(pc, "LD " + regName(nyb[1]) + ", [I]"));
							break;
						default:
							code.add(format(pc, "DW $" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
							keepParsingCode = false;
							break;
					}
					break;
				default:
					keepParsingCode = false;
					code.add(format(pc, "DW $" + hex(rom[pc-0x200]) + hex(rom[pc-0x200+1])));
					break;
			}
			pc += 2;
		}
		return code;
	}
	
	public static String hex(int i) {
		return Integer.toHexString(i).toUpperCase();
	}
	
	public static String hex(byte i) {
		char upper = (char) ((i & 0xF0) >> 4);
		if(upper > 9) upper += 55;
		else upper += 48;
		char lower = (char)(i & 0x0F);
		if(lower > 9) lower += 55;
		else lower += 48;

		return String.format("%c%c", upper, lower);
	}
	
	public static String regName(byte i) {
		return "V" + Integer.toHexString(i).toUpperCase();
	}
	
	public static String format(int addr, String value) {
		return "[0x" + hex(addr) + "] " + value;
	}
}
