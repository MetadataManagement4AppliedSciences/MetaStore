/*
 * Copyright 2017 Karlsruhe Institute of Technology.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.kit.masi.metastore;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import edu.kit.masi.metastore.control.MetaStoreController;
import edu.kit.masi.metastore.exception.MetaStoreException;
import edu.kit.masi.metastore.model.ReturnType;

/**
 * A sequence of commands testing the functionality of the metastore.
 * @author hartmann-v
 */
public class TestServices {
  /**
   * Test of the functions.
   * Parameter>
   * - XSD file for METS
   * - METS file (xml)
   * - Object ID for METS file
   * - XSD file for MASi
   * - Second METS file
   * - Object ID for second METS file
   * - XSD file for CHEM
   *
   * @param args Arguments for the test.
   * @throws MetaStoreException
   * @throws IOException 
   */
	public static void main(String[] args) throws MetaStoreException, IOException {
		MetaStoreController msc = new MetaStoreController();
		// register xsd
		System.out.println("Register XSD: " + args[0]);
		String readFileToString = FileUtils.readFileToString(new File(args[0]));
		msc.registerXsdDocument(readFileToString, "mets");
		System.out.println("Success");

		String nestedXSD = FileUtils.readFileToString(new File(args[3]));
		msc.registerXsdDocument(nestedXSD, "masi");
		System.out.println("Success");

		String nestedXSD2 = FileUtils.readFileToString(new File(args[6]));
		msc.registerXsdDocument(nestedXSD2, "chem");
		System.out.println("Success");

		// Storing Mets
		System.out.println("Storing METS file " + args[1]);
		String metsFile = FileUtils.readFileToString(new File(args[1]));
		msc.storeMetsDocument(metsFile, args[2]);
		System.out.println("Success");

		// Loading Mets as XML
		// System.out.println("Reading METS file with id: " + args[2]);
		// System.out.println(msc.getMetsDocument(args[2], ReturnType.XML));
		// System.out.println("Reading METS file with id: " + args[2]);
		// System.out.println(msc.getMetsDocument(args[2], ReturnType.JSON));

		// Storing Mets2
		System.out.println("Storing METS file " + args[4]);
		String metsFile2 = FileUtils.readFileToString(new File(args[4]));
		msc.storeMetsDocument(metsFile2, args[5]);
		System.out.println("Success");

//		 Searching terms in metastore
		 System.out.println("Searching For term Vaibhav");
		 System.out.println(msc.searchForMetsDocuments("VOLKER", 2,
		 ReturnType.XML));
		 Scanner scan = new Scanner(System.in);
		 scan.nextLine();
		// search for Section matching nameSpace
		 System.out.println("Searching For Sections");
		 System.out.println(msc.getPartialMetsDocument("http://datamanager.kit.edu/masi/chem/1.0",
		 args[5], ReturnType.XML));
		 scan.nextLine();

		// search for Section matching nameSpace
		 try {
		 System.out.println("Searching For Sections should fail");
		 System.out.println(msc.getPartialMetsDocument("http://datamanager.kit.edu/masi/chem/1.0",
		 args[5], ReturnType.JSON));
		 } catch (MetaStoreException mse) {
		 mse.printStackTrace();
		 }
		 scan.nextLine();

		System.out.println("Updating partial mets sections");
		String pamets = "<ns:metadata xmlns:ns=\"http://datamanager.kit.edu/masi/chem/1.0\">"
				+ "<ns:creator>string2</ns:creator>" + "<ns:device>string2</ns:device>"
				+ "<ns:experiment>string2</ns:experiment>" + "<ns:ligand>string2</ns:ligand>"
				+ "<ns:miscellaneous>string2</ns:miscellaneous>" + "<ns:salt>string2</ns:salt>"
				+ "<ns:solvent>string2</ns:solvent>" + "<ns:temperature>string2</ns:temperature>" + "</ns:metadata>";
		msc.updatePartialMetsDocument(pamets, args[2], "DM2");
		System.out.println("Searching For term VOLKER again");
		 System.out.println(msc.searchForMetsDocuments("VOLKER", 2,
		 ReturnType.XML));
	}

}
