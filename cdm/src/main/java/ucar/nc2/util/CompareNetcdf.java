// $Id: TestCompare.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.util;

import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.*;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.ma2.DataType;

import java.util.List;
import java.util.Formatter;
import java.util.ArrayList;
import java.io.IOException;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

/**
 * Compare two NetcdfFile
 *
 * @author john
 */
public class CompareNetcdf {

  static public void compareFiles(NetcdfFile org, NetcdfFile copy) {
    compareFiles(org, copy, false, false, false);
  }

  static public void compareFiles(NetcdfFile org, NetcdfFile copy, boolean _compareData, boolean _showCompare, boolean _showEach) {
    CompareNetcdf tc = new CompareNetcdf(_showCompare, _showEach, _compareData);
    tc.compare(org, copy, new Formatter(System.out));
  }

  /////////

  boolean showCompare = false;
  boolean showEach = false;
  boolean compareData = false;

  public CompareNetcdf(boolean showCompare, boolean showEach, boolean compareData ) {
    this.compareData = compareData;
    this.showCompare = showCompare;
    this.showEach = showEach;
  }

  public boolean compare(NetcdfFile org, NetcdfFile copy, Formatter f) {
    f.format("Original = %s%n", org.getLocation());
    f.format("CompareTo= %s%n", copy.getLocation());

    long start = System.currentTimeMillis();

    boolean ok = compareGroups(org.getRootGroup(), copy.getRootGroup(), f);
    f.format("Files are the same = %s%n", ok);

    long took = System.currentTimeMillis() - start;
    f.format("Time to compare = %d msecs%n", took);

   return ok;
  }

  private boolean compareGroups(Group org, Group copy, Formatter f) {
    if (showCompare) f.format(" compareGroup %s to %s %n", org.getName(), copy.getName());
    boolean ok = true;

    if (!org.getName().equals(copy.getName())) {
      f.format(" names are different %s != %s %n", org.getName(), copy.getName());
      ok = false;
    }


    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null, f);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null, f);

    // variables
    // cant use object equality, just match on short name
    for (Variable orgV : org.getVariables()) {
      Variable copyVar = copy.findVariable(orgV.getShortName());
      if (copyVar == null) {
        f.format(" cant find variable %s in 2nd file%n", orgV.getName());
        ok = false;
      } else {
        ok &= compareVariables(orgV, copyVar, compareData, f);
      }
    }

    for (Variable copyV : copy.getVariables()) {
      Variable orgV = org.findVariable(copyV.getShortName());
      if (orgV == null) {
        f.format(" cant find variable %s in 1st file%n", copyV.getName());
        ok = false;
      }
    }

    // nested groups
    List groups = new ArrayList();
    ok &= checkAll(org.getGroups(), copy.getGroups(), groups, f);
    for (int i = 0; i < groups.size(); i += 2) {
      Group orgGroup = (Group) groups.get(i);
      Group ncmlGroup = (Group) groups.get(i + 1);
      ok &= compareGroups(orgGroup, ncmlGroup, f);
    }

    return ok;
  }


  private boolean compareVariables(Variable org, Variable copy, boolean compareData, Formatter f) {
    boolean ok = true;

    if (showCompare) f.format("  compareVariables %s to %s %n", org.getName(), copy.getName());
    if (!org.getName().equals(copy.getName())) {
      f.format("  names are different %s != %s %n", org.getName(), copy.getName());
      ok = false;
    }

    // dimensions
    ok &= checkAll(org.getDimensions(), copy.getDimensions(), null, f);

    // attributes
    ok &= checkAll(org.getAttributes(), copy.getAttributes(), null, f);

    // coord sys
    if ((org instanceof VariableEnhanced) && (copy instanceof VariableEnhanced)) {
      VariableEnhanced orge = (VariableEnhanced) org;
      VariableEnhanced copye = (VariableEnhanced) copy;
      ok &= checkAll(orge.getCoordinateSystems(), copye.getCoordinateSystems(), null, f);
    }

    // data !!
    if (compareData) {
      try {
        compareVariableData(org, copy, showCompare, f);

      } catch (IOException e) {
         ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
         e.printStackTrace(new PrintStream(bos));
         f.format("%s", bos.toString());
      }
    }

    // nested variables
    if (org instanceof Structure) {
      assert (copy instanceof Structure);
      Structure orgS = (Structure) org;
      Structure ncmlS = (Structure) copy;

      List vars = new ArrayList();
      ok &= checkAll(orgS.getVariables(), ncmlS.getVariables(), vars, f);
      for (int i = 0; i < vars.size(); i += 2) {
        Variable orgV = (Variable) vars.get(i);
        Variable ncmlV = (Variable) vars.get(i + 1);
        ok &= compareVariables(orgV, ncmlV, false, f);
      }
    }

    return ok;
  }

  // make sure each object in each list are in the other list, using equals().
  // return an arrayList of paired objects.
  private boolean checkAll(List list1, List list2, List result, Formatter f) {
    boolean ok = true;

    for (Object aList1 : list1) {
      ok &= checkEach(aList1, list1, list2, result, f);
    }

    for (Object aList2 : list2) {
      ok &= checkEach(aList2, list2, list1, result, f);
    }

    return ok;
  }

  // check that want is in both list1 and list2, using object.equals()
  private boolean checkEach(Object want1, List list1, List list2, List result, Formatter f) {
    boolean ok = true;
    try {
      int index2 = list2.indexOf(want1);
      if (index2 < 0) {
        f.format("   %s %s not in list 2", want1.getClass().getName(), want1);
        ok = false;
      }

      Object want2 = list2.get(index2);
      int index1 = list1.indexOf(want2);
      if (index1 < 0) {
        f.format("   %s %s not in list 1", want2.getClass().getName(), want2);
        ok = false;
      }

      Object want = list1.get(index1);
      if (want != want1) {
        f.format("   %s not ==", want1);
        ok = false;
      }

      if (showEach) f.format("  OK <%s>.equals<%s>%n", want1, want2);
     if (result != null) {
        result.add(want1);
        result.add(want2);
      }

    } catch (Throwable t) {
      f.format(" Error= %s %n", t.getMessage());
    }

    return ok;
  }

  static public void compareVariableData(Variable var1, Variable var2, boolean showCompare, Formatter f) throws IOException {
    Array data1 = var1.read();
    Array data2 = var2.read();

    if (showCompare) f.format("   compareArrays %s %s size=%d%n", var1.getNameAndDimensions(), var1.isUnlimited(), data1.getSize());
    compareData(data1, data2);
    if (showCompare) f.format("   ok%n");
  }

  static public void compareData(Array data1, Array data2) {
    assert data1.getSize() == data2.getSize();
    assert data1.getElementType() == data2.getElementType() : data1.getElementType() + "!=" + data2.getElementType();
    DataType dt = DataType.getType(data1.getElementType());

    IndexIterator iter1 = data1.getIndexIterator();
    IndexIterator iter2 = data2.getIndexIterator();

    if (dt == DataType.DOUBLE) {
      while (iter1.hasNext()) {
        double v1 = iter1.getDoubleNext();
        double v2 = iter2.getDoubleNext();
        if (!Double.isNaN(v1) || !Double.isNaN(v2))
          assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt == DataType.FLOAT) {
      while (iter1.hasNext()) {
        float v1 = iter1.getFloatNext();
        float v2 = iter2.getFloatNext();
        if (!Float.isNaN(v1) || !Float.isNaN(v2))
          assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt == DataType.INT) {
      while (iter1.hasNext()) {
        int v1 = iter1.getIntNext();
        int v2 = iter2.getIntNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt == DataType.SHORT) {
      while (iter1.hasNext()) {
        short v1 = iter1.getShortNext();
        short v2 = iter2.getShortNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    } else if (dt == DataType.BYTE) {
      while (iter1.hasNext()) {
        byte v1 = iter1.getByteNext();
        byte v2 = iter2.getByteNext();
        assert v1 == v2 : v1 + " != " + v2 + " count=" + iter1;
      }
    }
  }


  public static void main(String arg[]) throws IOException {
    NetcdfFile ncfile1 = NetcdfDataset.openFile("dods://thredds.cise-nsf.gov:8080/thredds/dodsC/satellite/SFC-T/SUPER-NATIONAL_1km/20090516/SUPER-NATIONAL_1km_SFC-T_20090516_2200.gini", null);
    NetcdfFile ncfile2 = NetcdfDataset.openFile("dods://motherlode.ucar.edu:8080/thredds/dodsC/satellite/SFC-T/SUPER-NATIONAL_1km/20090516/SUPER-NATIONAL_1km_SFC-T_20090516_2200.gini", null);
    compareFiles(ncfile1, ncfile2, false, true, false);
  }
}