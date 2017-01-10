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
import java.util.HashMap;

public class Assembler {
	
	public static final String reservedRefs = "v0v1v2v3v4v5v6v7v8v9vavbvcvdvevfdtsti[i]fbk";
	public static final String opcodes = "CLSRETJPCALLSESNELDADDANDXORSHRSUBNSHLDRWSKPSKNPRND";
	
	public static byte[] assemble(ArrayList<String> src) {
		ArrayList<Byte> code = new ArrayList<Byte>();
		HashMap<Integer, String> jumps = new HashMap<Integer, String>();
		HashMap<Integer, String> refs = new HashMap<Integer, String>();
		HashMap<String, ArrayList<Byte>> data = new HashMap<String, ArrayList<Byte>>();
		HashMap<String, Integer> labels = new HashMap<String, Integer>();
		
		// Instruction counter
		int ic = 0;
		
		String currentDataLabel = null;
		ArrayList<Byte> currentData = null;
		boolean readingData = false;
		
		for(int ln = 0; ln < src.size(); ln++) {
			String line = src.get(ln);
			// Clean the line
			if(line.contains(";")) {
				line = line.substring(0, line.indexOf(";"));
			}
			line = line.replace('\t', ' ').trim().toUpperCase();
			if(line.isEmpty()) {
				continue;
			}
			
			String opcode = line.split(" ")[0].trim();
			String[] rawargs = line.contains(" ") ? line.substring(line.indexOf(" ")).split(",") : new String[] { "" };
			String[] args = new String[rawargs.length];
			for(int i = 0; i < rawargs.length; i++) {
				args[i] = rawargs[i].trim().toLowerCase();
			}
			
			try {
				if(opcode.equals("CLS")) {
					code.add((byte) 0x00);
					code.add((byte) 0xE0);
				}
				else if(opcode.equals("RET")) {
					code.add((byte) 0x00);
					code.add((byte) 0xEE);
				}
				else if(opcode.equals("JP")) {
					if(args[0].equals("v0")) {
						short addr = parseShort(args[1]);
						code.add((byte) (0xB0 | (addr >> 8)));
						code.add((byte) (addr & 0x00FF));
					}
					else if(args[0].startsWith("$")) {
						code.add((byte) (0x10 | (parseShort(args[0].substring(1)) >> 8)));
						code.add((byte) (parseShort(args[0].substring(1)) & 0x00FF));
					}
					else {
						code.add((byte) 0x10);
						code.add((byte) 0x00);
						jumps.put(ic, args[0]);
					}
				}
				else if(opcode.equals("CALL")) {
					if(args[0].startsWith("$")) {
						code.add((byte) (0x20 | (parseShort(args[0].substring(1)) >> 8)));
						code.add((byte) (parseShort(args[0].substring(1)) & 0x00FF));
					}
					else {
						code.add((byte) 0x20);
						code.add((byte) 0x00);
						jumps.put(ic, args[0]);
					}
				}
				else if(opcode.equals("SE")) {
					byte nyb = 0x30;
					if(args[1].startsWith("v")) {
						nyb = 0x50;
					}
					code.add((byte) (nyb | parseByte(args[0].substring(1))));
					if(nyb == 0x30) {
						code.add(parseByte(args[1]));
					}
					else {
						code.add((byte) (parseByte(args[1].substring(1)) << 4));
					}
				}
				else if(opcode.equals("SNE")) {
					byte nyb = 0x40;
					if(args[1].startsWith("v")) {
						nyb = (byte) 0x90;
					}
					code.add((byte) (nyb | Byte.parseByte(args[0].substring(1), 16)));
					if(nyb == 0x40) {
						code.add(parseByte(args[1]));
					}
					else {
						code.add((byte) (parseByte(args[1].substring(1)) << 4));
					}
				}
				else if(opcode.equals("LD")) {
					if(args[0].startsWith("v")) {
						if(args[1].startsWith("v")) {
							code.add((byte) (0x80 | parseByte(args[0].substring(1))));
							code.add((byte) (parseByte(args[1].substring(1)) << 4));
						}
						else if(args[1].equals("dt")) {
							code.add((byte) (0xF0 | parseByte(args[0].substring(1))));
							code.add((byte) 0x07);
						}
						else if(args[1].equals("k")) {
							code.add((byte) (0xF0 | parseByte(args[0].substring(1))));
							code.add((byte) 0x0A);
						}
						else if(args[1].equals("[i]")) {
							code.add((byte) (0xF0 | parseByte(args[0].substring(1))));
							code.add((byte) 0x65);
						}
						else {
							code.add((byte) (0x60 | parseByte(args[0].substring(1))));
							code.add(parseByte(args[1]));
						}
					}
					else if(args[0].equals("i")) {
						if(args[1].startsWith("$")) {
							code.add((byte) (0xA0 | (parseShort(args[1].substring(1)) >> 8)));
							code.add((byte) (parseShort(args[1].substring(1)) & 0x00FF));
						}
						else {
							refs.put(ic, args[1]);
							code.add((byte) 0xA0);
							code.add((byte) 0x00);
						}
					}
					else if(args[0].equals("dt")) {
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x15);
					}
					else if(args[0].equals("st")) {
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x18);
					}
					else if(args[0].equals("f")) {
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x29);
					}
					else if(args[0].equals("b")) {
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x33);
					}
					else if(args[0].equals("[i]")) {
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x55);
					}
					else {
						System.out.println("Unrecognized LD syntax at line " + (ln+1));
						System.out.println("> " + opcode + " " + args[0] + ", " + args[1]);
						return null;
					}
				}
				else if(opcode.equals("ADD")) {
					if(args[0].startsWith("v")) {
						if(args[1].startsWith("v")) {
							code.add((byte) (0x80 | parseByte(args[0].substring(1))));
							code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x04));
						}
						else {
							code.add((byte) (0x70 | parseByte(args[0].substring(1))));
							code.add(parseByte(args[1]));
						}
					}
					else if(args[0].equals("i")) {
						if (!args[1].startsWith("v")) {
							System.out.println("Unrecognized ADD syntax at line " + (ln+1));
							System.out.println("> " + opcode + " " + args[0] + ", " + args[1]);
							return null;
						}
						
						code.add((byte) (0xF0 | parseByte(args[1].substring(1))));
						code.add((byte) 0x1E);
					}
				}
				else if(opcode.equals("OR")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x01));
				}
				else if(opcode.equals("AND")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x02));
				}
				else if(opcode.equals("XOR")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x03));
				}
				else if(opcode.equals("SUB")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x05));
				}
				else if(opcode.equals("SHR")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x06));
				}
				else if(opcode.equals("SUBN")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x07));
				}
				else if(opcode.equals("SHL")) {
					code.add((byte) (0x80 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | 0x08));
				}
				else if(opcode.equals("RND")) {
					code.add((byte) (0xC0 | parseByte(args[0].substring(1))));
					code.add(parseByte(args[1]));
				}
				else if(opcode.equals("DRW")) {
					code.add((byte) (0xD0 | parseByte(args[0].substring(1))));
					code.add((byte) ((parseByte(args[1].substring(1)) << 4) | parseByte(args[2])));
				}
				else if(opcode.equals("SKP")) {
					code.add((byte) (0xE0 | parseByte(args[0].substring(1))));
					code.add((byte) 0x9E);
				}
				else if(opcode.equals("SKNP")) {
					code.add((byte) (0xE0 | parseByte(args[0].substring(1))));
					code.add((byte) 0xA1);
				}
				else if(opcode.equals("DB")) {
					if(reservedRefs.contains(args[0])) {
						System.out.println("Invalid reference name at line " + (ln+1));
						return null;
					}
					else if(args.length == 2) {
						ArrayList<Byte> single = new ArrayList<Byte>();
						single.add(parseByte(args[1]));
						data.put(args[0], single);
						if(readingData) {
							readingData = false;
							data.put(currentDataLabel, currentData);
							currentDataLabel = null;
							currentData = null;
						}
					}
					else if(readingData) {
						currentData.add(parseByte(args[0]));
					}
				}
				else if(opcode.equals("DW")) {
					if(reservedRefs.contains(args[0])) {
						System.out.println("Invalid reference name at line " + (ln+1));
						return null;
					}
					else if(args.length == 2) {
						short d = parseShort(args[1]);
						ArrayList<Byte> single = new ArrayList<Byte>();
						single.add((byte) (d >> 8));
						single.add((byte) (d & 0x00FF));
						data.put(args[0], single);
						if(readingData) {
							readingData = false;
							data.put(currentDataLabel, currentData);
							currentDataLabel = null;
							currentData = null;
						}
					}
					else if(readingData) {
						currentData.add((byte) (parseShort(args[0]) >> 8));
						currentData.add((byte) (parseShort(args[0]) & 0x00FF));
					}
				}
				else if(opcode.equals("LABEL")) {
					if(reservedRefs.contains(args[0])) {
						System.out.println("Invalid reference name at line " + (ln+1));
						return null;
					}
					labels.put(args[0], ic);
				}
				// Handle syntax of the form some_label:
				else if(opcode.endsWith(":")) {
					args[0] = opcode.substring(0, opcode.indexOf(":")).toLowerCase();
					if(reservedRefs.contains(args[0])) {
						System.out.println("Invalid reference name at line " + (ln+1));
						return null;
					}
					else if(readingData) {
						readingData = false;
						data.put(currentDataLabel, currentData);
						currentDataLabel = null;
						currentData = null;
					}
					String next = "";
					for(int i = ln+1; i < src.size(); i++) {
						next = src.get(i);
						if(next.contains(";")) {
							next = next.substring(0, next.indexOf(";"));
						}
						next = next.replace('\t', ' ').trim().toUpperCase();
						if(next.isEmpty()) {
							continue;
						}
						else {
							break;
						}
					}
					if(opcodes.contains(next.split(" ")[0].trim())) {
						labels.put(args[0], ic);
						if(readingData) {
							readingData = false;
							data.put(currentDataLabel, currentData);
							currentDataLabel = null;
							currentData = null;
						}
					}
					else {
						currentDataLabel = args[0];
						currentData = new ArrayList<Byte>();
						readingData = true;
					}
				}
				else {
					System.out.println("Unrecognized token at line " + (ln+1));
					System.out.println(line);
					return null;
				}
				
				if(opcodes.contains(opcode)) {
					ic += 2;
					if(readingData) {
						readingData = false;
						data.put(currentDataLabel, currentData);
						currentDataLabel = null;
						currentData = null;
					}
				}

				if(ln+1 >= src.size() && readingData) {
					readingData = false;
					data.put(currentDataLabel, currentData);
					currentDataLabel = null;
					currentData = null;
				}
				
			}
			catch(NumberFormatException e) {
				System.out.println("Number format error at line " + (ln+1));
				return null;
			}
			catch(ArrayIndexOutOfBoundsException e) {
				System.out.println("Parameter error for instruction '" + opcode + " " + args[0] +  "' at line " + (ln+1));
				e.printStackTrace();
				return null;
			}
			catch(Exception e) {
				System.out.println("Assembly failure at line " + (ln+1));
				return null;
			}
		}
		code.add((byte) 0x00);
		code.add((byte) 0x00);
		ic += 2;
		
		// Tie up JP and CALL references
		for(int k : jumps.keySet()) {
			if(!labels.containsKey(jumps.get(k))) {
				System.out.println("Invalid reference '" + jumps.get(k) + "' at line " + (k+1));
				return null;
			}
			short dest = (short)(labels.get(jumps.get(k)) + 0x200);
			byte b1 = code.get(k);
			byte b2 = code.get(k+1);
			b1 |= (dest >> 8);
			b2 = (byte) (dest & 0x00FF);
			code.set(k, b1);
			code.set(k+1, b2);
		}
		
		// Take care of addressing references
		HashMap<String, Short> locations = new HashMap<String, Short>();
		for(String k : data.keySet()) {
			if(k != null) {
				locations.put(k, (short) (ic + 0x200));
				if(data.get(k) == null) {
					System.out.println("Null reference for " + k);
				}
				for(byte b : data.get(k)) {
					code.add(b);
					ic++;
				}
			}
		}
		
		// Use the produced label/memory location map to fill in code that depended on a reference
		for(int k : refs.keySet()) {
			if(!locations.containsKey(refs.get(k))) {
				System.out.println("Invalid reference '" + refs.get(k) + "' at line " + (k+1));
				return null;
			}
			byte b1 = code.get(k);
			byte b2 = code.get(k+1);
			short dest = locations.get(refs.get(k));
			b1 |= (dest >> 8);
			b2 = (byte) (dest & 0x00FF);
			code.set(k, b1);
			code.set(k+1, b2);
		}
		
		byte[] array = new byte[code.size()];
		for(int i = 0; i < code.size(); i++) {
			array[i] = code.get(i);
		}
		return array;
	}
	
	// Wrapper that will parse numerals in a variety of formats
	public static byte parseByte(String num) {
		if(num.startsWith("0x")) {
			return (byte) Integer.parseInt(num, 16);
		}
		else if(num.startsWith("0b")) {
			num = num.substring(2);
			if(!num.startsWith("0")) num = "0" + num;
			return (byte) Integer.parseInt(num, 2);
		}
		else if(num.startsWith("#")) {
			return (byte) Integer.parseInt(num.substring(1));
		}
		else {
			return (byte) Integer.parseInt(num);
		}
	}
	
	// Wrapper that will parse numerals in a variety of formats
	public static short parseShort(String num) {
		if(num.startsWith("0x")) {
			return (short) Integer.parseInt(num.substring(2), 16);
		}
		else if(num.startsWith("0b")) {
			num = num.substring(2);
			if(!num.startsWith("0")) num = "0" + num;
			return (short) Integer.parseInt(num, 2);
		}
		else if(num.startsWith("#")) {
			return (short) Integer.parseInt(num.substring(1));
		}
		else {
			return (short) Integer.parseInt(num);
		}
	}
}
