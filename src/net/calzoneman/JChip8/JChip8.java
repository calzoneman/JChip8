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

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.swing.JFrame;

public class JChip8 {
	
	public static final String version = "1.0";

	public static void main(String[] args) {
		String output = null;
		if(args.length < 2) {
			help();
			return;
		}
		else if(args.length == 4 && (args[2].equals("-o") || args[2].equals("--output"))) {
			output = args[3];
		}
		
		if(args[0].equals("hexdump") || args[0].equals("h")) {
			memdump(RomLoader.load(args[1]));
		}
		else if(args[0].equals("run") || args[0].equals("r")) {
			run(args[1]);
		}
		else if(args[0].equals("assemble") || args[0].equals("a")) {
			if(output != null) assemble(args[1], output);
			else assemble(args[1]);
		}
		else if(args[0].equals("disassemble") || args[0].equals("d")) {
			if(output != null) disassemble(args[1], output);
			else disassemble(args[1]);
		}
		else {
			help();
		}
	}
	
	public static void assemble(String srcname) {
		String destname = srcname;
		if(srcname.contains(".")) {
			destname = srcname.substring(0, srcname.lastIndexOf("."));
		}
		assemble(srcname, destname + ".ch8");
	}
	
	public static void assemble(String srcname, String destname) {
		ArrayList<String> src = new ArrayList<String>();
		try (BufferedReader br = new BufferedReader(new FileReader(srcname))) {
			String line = "";
			while(line != null) {
				src.add(line);
				line = br.readLine();
			}
		}
		catch(Exception e) {
			System.out.println("Unable to load " + srcname);
			return;
		}
		
		byte[] assembled = Assembler.assemble(src);
		if(assembled != null) {
			try (FileOutputStream fis = new FileOutputStream(destname)) {
				fis.write(assembled);
				System.out.println("Assembled code saved to " + destname);
			}
			catch(Exception e) {
				System.out.println("Unable to save " + destname);
				return;
			}
		}
		else {
			System.out.println("Asembly failed");
		}
	}
	
	public static void disassemble(String romname) {
		String destname = romname;
		if(romname.contains(".")) {
			destname = romname.substring(0, romname.lastIndexOf("."));
		}
		disassemble(romname, destname);
	}
	
	public static void disassemble(String romname, String destname) {
		byte[] rom = RomLoader.load(romname);
		if(rom == null) {
			System.out.println("Unable to load " + romname + "; are you sure you typed it correctly?");
			return;
		}
		ArrayList<String> code = Disassembler.disassemble(rom);
		try {
			PrintWriter pw = new PrintWriter(destname);
			for(String line : code) {
				pw.println(line);
			}
			pw.close();
			System.out.println("Disassembly saved to " + destname);
		}
		catch(IOException ex) {
			System.out.println("Unable to save " + destname);
			return;
		}
	}
	
	public static void run(String romname) {
		byte[] rom = RomLoader.load(romname);
		if(rom == null) {
			System.out.println("Unable to load " + romname + "; are you sure you typed it correctly?");
			return;
		}
		VCPU vcpu = new VCPU(rom);
		JFrame appFrame = new JFrame();
		appFrame.setTitle("JChip8");
		appFrame.add(vcpu);
		appFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		appFrame.setVisible(true);
		appFrame.getContentPane().setMinimumSize(new Dimension(256, 128));
		appFrame.getContentPane().setPreferredSize(new Dimension(256, 128));
		appFrame.getContentPane().setMaximumSize(new Dimension(256, 128));
		appFrame.pack();
		vcpu.run();
		appFrame.setVisible(false);
		System.exit(0);
	}
	
	public static void memdump(byte[] mem) {
		if(mem == null) return;
		for(int i = 0; i < mem.length; i++) {
			char upper = (char)((mem[i] & 0xF0) >> 4);
			if(upper > 9) upper += 55;
			else upper += 48;
			char lower = (char)(mem[i] & 0x0F);
			if(lower > 9) lower += 55;
			else lower += 48;

			System.out.print(String.format("%c%c  ", upper, lower));
			if(i % 8 == 7) System.out.println();
		}
		System.out.println();
	}
	
	public static void help() {
		System.out.println("--- JChip8 v" + version + " by Calvin Montgomery ---");
		System.out.println();
		System.out.println("Usage: java -jar JChip8.jar <run(r)|assemble(a)|disassemble(d)|hexdump(h)> <file> [--output(-o) <dest>]");
		System.out.println();
		System.out.println("run <file> loads Chip8 bytecode from <file> and executes it");
		System.out.println("assemble <file> assembles Chip8 assembly into bytecode.  If the -o flag is specified, it is saved to <dest>, otherwise <file>.ch8");
		System.out.println("disassemble <file> disassembles Chip8 bytecode from <file>. If the -o flag is specified, it is saved to <dest>, otherwise <file>.asm");
		System.out.println("hexdump <file> prints the contents of <file> in hexadecimal");
	}
}
