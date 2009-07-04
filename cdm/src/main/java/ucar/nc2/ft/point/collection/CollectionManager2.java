/*
 * Copyright (c) 1998 - 2009. University Corporation for Atmospheric Research/Unidata
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
package ucar.nc2.ft.point.collection;

import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateFromString;

import java.util.*;
import java.io.File;

import thredds.inventory.*;
import thredds.inventory.WildcardMatchOnPath;
import thredds.inventory.DateExtractorFromFilename;
import thredds.filesystem.ControllerOS;

/**
 * Manages feature dataset collections.
 * Look; not updating the collection.
 *
 * collection URI syntax:
 *   directory/filter?dateFormatMark
 *
 * @author caron
 * @since May 20, 2009
 */


public class CollectionManager2 implements TimedCollection {
  static private boolean show = true;
  static private MController controller;

  static public void setController(MController _controller) {
    controller = _controller;
  }

  ////////////////////////////////

  private List<TimedCollection.Dataset> c;
  private DateRange dateRange;

  static public CollectionManager2 factory(String collectionDesc, Formatter errlog) {
    if (null == controller) controller = new ControllerOS();  // default 
    return new CollectionManager2(collectionDesc, errlog);
  }

  private CollectionManager2(String collectionDesc, Formatter errlog) {

    // first part is the directory
    int posWildcard = collectionDesc.lastIndexOf('/');
    String dirName = collectionDesc.substring(0, posWildcard);

    File locFile = new File( dirName);
    if (!locFile.exists()) {
      errlog.format(" Directory %s does not exist %n", dirName);
      return;
    }

    // optional dateFormatMark
    String dateFormatMark = null;
    int posFormat = collectionDesc.lastIndexOf('?');
    if (posFormat > 0) {
      dateFormatMark = collectionDesc.substring(posFormat+1); // after the ?
      collectionDesc = collectionDesc.substring(0,posFormat); // before the ?
    }

    // filter
    String filter = null;
    if (posWildcard < collectionDesc.length() - 2)
      filter = collectionDesc.substring(posWildcard + 1);

    if (show) System.out.printf("CollectionManager collectionDesc=%s filter=%s dateFormatMark=%s %n", collectionDesc, filter, dateFormatMark);

    MFileFilter mfilter = (null == filter) ? null : new WildcardMatchOnPath(filter);
    DateExtractor dateExtractor = (dateFormatMark == null) ? null : new DateExtractorFromFilename(dateFormatMark);
    MCollection mc = new thredds.inventory.MCollection(dirName, dirName, mfilter, dateExtractor);

    // get the inventory, sort
    List<MFile> fileList = new ArrayList<MFile>();
    Iterator<MFile> invIter = controller.getInventory( mc);
    while (invIter.hasNext())
      fileList.add(invIter.next());
    Collections.sort(fileList);

    c = new ArrayList<TimedCollection.Dataset>(fileList.size());
    for (MFile f : fileList)
      c.add(new Dataset(f, dateFormatMark));

    if (dateFormatMark != null) {
      for (int i=0; i<c.size()-1; i++) {
        Dataset d1 = (Dataset) c.get(i);
        Dataset d2 = (Dataset) c.get(i+1);
        d1.setDateRange(new DateRange(d1.start, d2.start));
        if (i == c.size()-2)
          d2.setDateRange( new DateRange(d2.start, d1.getDateRange().getDuration()));
      }
      Dataset first = (Dataset) c.get(0);
      Dataset last = (Dataset) c.get(c.size()-1);
      dateRange = new DateRange(first.getDateRange().getStart().getDate(), last.getDateRange().getEnd().getDate());
    }

    if (show) System.out.printf("%s %n", this);
  }

  CollectionManager2(CollectionManager2 from, DateRange want) {
    c = new ArrayList<TimedCollection.Dataset>(from.c.size());
    for (TimedCollection.Dataset d : from.c)
      if (want.intersects(d.getDateRange()))
        c.add(d);
  }

  public TimedCollection.Dataset getPrototype() {
    return c.get(0);
  }

  public Iterator<TimedCollection.Dataset> getIterator() {
    return c.iterator();
  }

  public TimedCollection subset(DateRange range) {
    return new CollectionManager2(this, range);
  }

  public DateRange getDateRange() {
    return dateRange;
  }

  @Override
  public String toString() {
    Formatter f = new Formatter();
    f.format("CollectionManager{%n");
    for (TimedCollection.Dataset d : c)
      f.format(" %s%n", d);
    f.format("}%n");
    return f.toString();
  }

  private class Dataset implements TimedCollection.Dataset {
    String location;
    DateRange dateRange;
    Date start;

    Dataset(MFile f, String dateFormatMark) {
      this.location = f.getPath();
      if (dateFormatMark != null)
        start = DateFromString.getDateUsingDemarkatedCount(f.getName(), dateFormatMark, '#');
    }

    public String getLocation() {
      return location;
    }

    public DateRange getDateRange() {
      return dateRange;
    }

    public void setDateRange(DateRange dateRange) {
      this.dateRange = dateRange;
    }

    @Override
    public String toString() {
      return "Dataset{" +
          "location='" + location + '\'' +
          ", dateRange=" + dateRange +
          '}';
    }
  }

}