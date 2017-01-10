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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RomLoader {
	public static byte[] load(String filename) {
		File file = new File(filename);
		System.out.println(new File(".").getAbsolutePath());
		if(!file.exists()) return null;
		try (FileInputStream fis = new FileInputStream(file)) {
			byte[] raw = new byte[4096];
			int len = fis.read(raw, 0, 4096);
			byte[] rom = new byte[len];
			for(int i = 0; i < len; i++) {
				rom[i] = raw[i];
			}
			return rom;
		}
		catch(IOException ex) {
			System.err.println("Erorr loading ROM: ");
			ex.printStackTrace();
			return null;
		}
	}
}
