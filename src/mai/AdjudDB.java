
/*
 * MAI - Multi-document Adjudication Interface
 * 
 * Copyright Amber Stubbs (astubbs@cs.brandeis.edu)
 * Department of Computer Science, Brandeis University
 * 
 * MAI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package mai;

import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Collections;
import java.util.Enumeration;

/**
 * AdjudDB is the class that handles all the calls to the 
 * SQLite database.  AdjudDB in MAI has the following tables:
 * 1) extents, with columns: location int(5), element_name, id
 * 2) links, with columns: id,fromid,from_name,toid,to_name,element_name
 * 3) one table for each tag in the DTD, where information about each
 * tag in every file that's loaded is stored
 * 
 * @author Amber Stubbs
 * @revised Keigh Rim
 *
 */

class AdjudDB {

    private PreparedStatement mExtInsert;
    private PreparedStatement mLinkInsert;
    private PreparedStatement mOverlapInsert;
    private Connection mConn;    
    private Hashtable<String,PreparedStatement> mSentsToIns;
    private ArrayList<String> mCurLinks; //used to keep track of what links are being displayed
    private HashCollection<String,int[]> mCurHL; //used to keep track of highlights
    private String SEP = "@#@";
    private String GS_FILE = "goldStandard.xml";

    /**
     * Creates all the tables, HashTables, PreparedStatements, and the connection
     * to the database.
     */
    AdjudDB() {
        try {
            mCurLinks = new ArrayList<String>();
            mCurHL = new HashCollection<String,int[]>();
            mSentsToIns = new Hashtable<String,PreparedStatement>();

            Class.forName("org.sqlite.JDBC");
            mConn = DriverManager.getConnection("jdbc:sqlite:adjud.db");
            Statement stat = mConn.createStatement();
            stat.executeUpdate("drop table if exists extents;");
            stat.executeUpdate("create table extents (file_name, location int(5), element_name, id);");
            stat.executeUpdate("drop table if exists links;");
            stat.executeUpdate("create table links (file_name, id,fromid,from_name,toid,to_name,element_name);");
            stat.executeUpdate("drop table if exists extent_overlaps");
            stat.executeUpdate("create table extent_overlaps (gsid, element_name, file_name, fileid);");

            mExtInsert = mConn.prepareStatement("insert into extents values (?, ?, ?, ?);");
            mLinkInsert = mConn.prepareStatement("insert into links values (?, ?, ?, ?, ?, ?, ?);");
            mOverlapInsert = mConn.prepareStatement("insert into extent_overlaps values (?, ?, ?, ?);");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * Adds a table to the DB for every link in the DTD
     * There will be a problem if any of those tags/tables is named
     * "extent", "link", or "extent_overlap"
     * 
     * @param dtd The DTD object that was loaded into MAI
     */
    void addDTD(DTD dtd) {
        ArrayList<Elem> elems = dtd.getElements();
        for (Elem elem : elems) {
            try {
                addTableToDB(elem);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Creates the table and a PreparedStatement for the table;
     * PreparedStatements go in the mSentsToIns hashtable for use later.
     * 
     * @param elem the Elem object being turned into a table
     * @throws Exception
     */
    private void addTableToDB(Elem elem) throws Exception{

        String name = elem.getName();
        Statement stat = mConn.createStatement();
        stat.executeUpdate("drop table if exists " + name + ";");
        ArrayList<Attrib> atts = elem.getAttributes();
        String statement = ("create table "+name+" (file_name, ");
        String prep_insert = ("insert into "+name+" values (?, ");
        for(int i=0;i<atts.size();i++) {
            if(i==atts.size()-1) {
                statement = statement + atts.get(i).getName() +");";
                prep_insert = prep_insert + "?);";
            }
            else{
                statement = statement + atts.get(i).getName() +", ";
                prep_insert = prep_insert + "?, ";
            }
        }
        stat.executeUpdate(statement);
        PreparedStatement st = mConn.prepareStatement(prep_insert);
        mSentsToIns.put(name, st);
    }

    /**
     * Sends the command that all the tag-specific tables in the 
     * database have their PreparedStatements inserted into the table
     * 
     * @param dtd the DTD containing all the tag names used to 
     * create the tables
     * 
     * @throws Exception
     */
    private void batchAll(DTD dtd) throws Exception{
        ArrayList<Elem> elements = dtd.getElements();
        for (Elem element : elements) {
            String name = element.getName();
            PreparedStatement ps = mSentsToIns.get(name);
            mConn.setAutoCommit(false);
            ps.executeBatch();
            mConn.setAutoCommit(true);
        }
    }

    /**
     * Inserts the PreparedStatements for a single table
     * 
     * @param e the Elem object describing the table being
     * updated
     * 
     * @throws Exception
     */
    void batchElement(Elem e) throws Exception{
        PreparedStatement ps = mSentsToIns.get(e.getName());
        mConn.setAutoCommit(false);
        ps.executeBatch();
        mConn.setAutoCommit(true);

    }

    /**
     * When a file is loaded into MAI the tags are turned into a HashCollection 
     * and sent here to be loaded into the database tables.
     * 
     * @param fileName the name of the file being added
     * @param dtd the DTD describing the tags in the file
     * @param newTags the HashCollection containing all the tags being added
     */
    void addTagsFromHash(String fileName, DTD dtd,
            HashCollection<String,Hashtable<String,String>> newTags) {
        //for each tag in the DTD, get the ArrayList of Hashtables associated with it
        ArrayList<Elem> elements = dtd.getElements();
        for (Elem element : elements) {
            String name = element.getName();
            Elem e = dtd.getElem(name);
            ArrayList<Hashtable<String,String>> tagList = newTags.getList(name);
            //for each set of tags in the ArrayList, add the tags to the DB
            //extent tags first
            if(tagList!=null) {
                if (element instanceof ElemExtent) {
                    for (Hashtable<String, String> tag : tagList) {
                        //first, add the extent tags with the PreparedStatement for that table
                        usePreparedExtentStatements(fileName, element, tag);
                    }
                }
            }
        }

        try {
            batchExtents();
            batchAll(dtd);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error adding extent tags");
        }
        for (Elem element : elements) {
            String name = element.getName();
            Elem elem = dtd.getElem(name);
            ArrayList<Hashtable<String, String>> tagList = newTags.getList(name);
            if (elem instanceof ElemLink && tagList != null) {
                for (Hashtable<String, String> tag : tagList) {
                    //next, add the links tags with the PreparedStatement for that table
                    usePreparedLinkStatements(fileName, elem, tag);
                }
            }
        }
        try {
            batchLinks();
            batchAll(dtd);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error adding link tags");
        }
    }

    /**
     * Uses the previously created PreparedStatements
     * to enter extent tag information into the database
     * mod by krim: multi-span support
     * 
     * @param fileName name of the file being added
     * @param elem the Elem object describing the tag being added
     * @param tag the Hashtable containing the tag information
     */
    void usePreparedExtentStatements(String fileName, Elem elem,
            Hashtable<String,String> tag) {
        //get PreparedStatement from Hashtable
        PreparedStatement ps = mSentsToIns.get(elem.getName());
        ArrayList<Attrib> atts = elem.getAttributes();
        try {
            ps.setString(1,fileName);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error adding name");
        }
        //add the tag information to the preparedStatement
        for(int i=0;i<atts.size();i++) {
            try {
                ps.setString(i+2,tag.get(atts.get(i).getName()));
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("error setting String for "+tag.get(atts.get(i).getName()));
            }
        }
        try {
            //add the set strings to the preparedstatement's batch
            ps.addBatch();
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error adding extent batch");
        }
        //now, add the tag information to the extent table
        String spansString = tag.get("spans");
        ArrayList<int[]> spans = MaiMain.parseSpansString(spansString);

        for (int[] span : spans) {
            int start = span[0], end = span[1];
            //if the tag is associated with a span in the text, use this
            if (start > -1) {
                for (int i = start; i < end; i++) {
                    try {
                        addExtent(fileName, i, elem.getName(), tag.get("id"));
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("error adding extent");
                    }
                }
            }
            //otherwise (if it's a non-consuming tag), use this
            else {
                try {
                    addExtent(fileName, -1, elem.getName(), tag.get("id"));
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("error adding -1 extent");
                }
            }
        }
    }

    /**
     * Uses the previously created PreparedStatements
     * to enter extent tag information into the database
     * 
     * @param fileName name of the file being added
     * @param elem the Elem object describing the tag being added
     * @param tag the Hashtable containing the tag information
     */
    void usePreparedLinkStatements(String fileName, Elem elem,
            Hashtable<String,String> tag) {
        //get PreparedStatement from Hashtable
        try {
            PreparedStatement ps = mSentsToIns.get(elem.getName());
            ArrayList<Attrib> atts = elem.getAttributes();
            ps.setString(1,fileName);
            for(int i=0;i<atts.size();i++) {
                String test = tag.get(atts.get(i).getName());
                if (test!=null) {
                    ps.setString(i+2,test);
                }
                else{
                    ps.setString(i+2,"");
                }
            }
            try {
                ps.addBatch();
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("error adding link batch");
            }
            //add the tag information to the link table
            String from_id = tag.get("fromID");
            String to_id = tag.get("toID");
            String from_type = getElementByFileAndID(fileName,from_id);
            String to_type = getElementByFileAndID(fileName,to_id);
            try {
                addLink(fileName, tag.get("id"), elem.getName(),
                        from_id, from_type, to_id, to_type);
            } catch(Exception e) {
                e.printStackTrace();
                System.out.println("error adding link to link table");
            }
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("error adding link: "+
                    "filename = "+fileName+ "\ntag id = "+tag.get("id"));
        }

    }

    //returns all the tag information based on file name and id
    /**
     * Returns a hashtable of tag information based on the id,  
     * tag name, and filename of the tag
     * 
     * @param tagName the name of the tag in the DTD
     * @param id the ID being searched for
     * @param fileName the name of the file the tag is in
     * @param atts the list of attributes whose values will be put in the Hashtable.
     * 
     * @return a Hashtable containing the attribute names and their 
     * values for the tag being searched for.
     * @throws Exception
     */
    Hashtable<String,String>getTagsByFileAndID(
            String tagName, String id, String fileName, ArrayList<Attrib> atts)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select * from " + tagName + " where id = '" +
                id + "' and file_name = '" + fileName + "';");

        ResultSet rs = stat.executeQuery(query);
        Hashtable<String,String> ht = new Hashtable<String,String>();

        int i = 0;
        while(rs.next()) {
            // for each attribute in the list,
            // get the value and put both in the hashtable
            for (Attrib att : atts) {
                // Added by krim: check for each att is required,
                // If not, null in resultset is okay to pass over with empty string
                // Otherwise, return null hash, which will warn a user about mismatch
                // between DTD and XML
                String attName = att.getName(), attVal = rs.getString(attName);
                if (attVal == null) {
                    if (att.getRequired()) {
                        return null;
                    } else {
                        ht.put(attName, "");
                    }
                } else {
                    ht.put(att.getName(), rs.getString(att.getName()));
                }
            }
        }
        rs.close();
        return ht;
    }

    /**
     * Gets the names of files that have a tag of a particular type
     * at a specified location.
     * 
     * @param elemName the name of the tag being searched for
     * @param loc the location being searched for
     * @return ArrayList of file names that have that tag type at that location
     * 
     * @throws Exception
     */
    ArrayList<String>getFilesAtLocbyElement(String elemName, int loc)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select * from extents where location = " +
                loc + " and element_name ='" + elemName +"';");
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> files = new ArrayList<String>();
        while(rs.next()) {
            files.add(rs.getString("file_name"));
        }
        rs.close();
        return files;
    }

    /**
     * Returns an ArrayList of extent tags that exist in a particular file
     * and are of the type specified by the Elem object.
     * ArrayList contains the tags in String form that are used for writing out
     * the XML files.
     * 
     * @param fileName the name of the file the tag is in
     * @param elem Elem object defining the type of tag being searched for
     * @returnArrayList of extent tags that exist in a particular file
     * and are of the type specified by the Elem object.
     * 
     * @throws Exception
     */
    ArrayList<String> getExtentTagsByFileAndType(
            String fileName, Elem elem) throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select * from "+elem.getName()+ " where file_name = '" +
                fileName + "' order by start;");
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> tags = makeTagStringsForOutput(rs, elem);
        rs.close();
        return tags;
    }

    /**
     * Returns an ArrayList of link tags that exist in a particular file
     * and are of the type specified by the Elem object.
     * ArrayList contains the tags in String form that are used for writing out
     * the XML files.
     * 
     * @param fileName the name of the file the tag is in
     * @param elem Elem object defining the type of tag being searched for
     * @returnArrayList of extent tags that exist in a particular file
     * and are of the type specified by the Elem object.
     * 
     * @throws Exception
     */
    ArrayList<String> getLinkTagsByFileAndType(
            String fileName, Elem elem) throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select * from "+elem.getName()+ " where file_name = '" +
                fileName + "' order by id;");
        ResultSet rs = stat.executeQuery(query);
        ArrayList<String> tags = makeTagStringsForOutput(rs, elem);
        rs.close();
        return tags;
    }

    /**
     * Creates strings containing the tag information being searched for
     * 
     * 
     * @param rs the ResultSet of a different method (getLinkTagsByFileAndType or
     * (getExtentTagsByFileAndType)
     * 
     * @param elem the Elem object describing the tags being retrieved
     * 
     * @return an ArrayList of Strings containing the tag information
     */
    private ArrayList<String> makeTagStringsForOutput(ResultSet rs, Elem elem) {
        ArrayList<String> tags = new ArrayList<String>();
        ArrayList<Attrib> atts= elem.getAttributes();
        try {
            while(rs.next()) {
                String tag = ("<"+elem.getName()+ " ");
                for(int i=0;i<atts.size();i++) {
                    String attName = atts.get(i).getName();
                    String attText = rs.getString(attName);
                    //need to get rid of all the information that's
                    //not valid in XML
                    attText=attText.replace("\n"," ");
                    attText=attText.replace("<","&lt;");
                    attText=attText.replace(">","&gt;");
                    attText=attText.replace("&","&amp;");
                    attText=attText.replace("\"","'");
                    tag = tag+attName+"=\""+attText+"\" ";
                }
                tag = tag + "/>\n";
                tags.add(tag);
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return tags;
    }
    
    /**
     * Retrieves a Hashtable of all the locations in a file where tags exist.
     * Used when assigning colors to the text when a link is selected
     * 
     * @param filename the name of the file the tags are coming from
     * @return Hashtable with the locations of tags as keys
     * @throws Exception
     */
    Hashtable<String,String>getAllExtentsByFile(String filename)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select location from extents where file_name = '"+ filename+"';");
        ResultSet rs = stat.executeQuery(query);
        Hashtable<String,String> allLocs = new Hashtable<String,String>();
        while(rs.next()) {
            allLocs.put(rs.getString("location"),"");
        }
        rs.close();
        return allLocs;
    }
    
    /**
     * Returns a HashCollection of locations (as keys) and the file names that have 
     * a particular type of tag at each location.
     * 
     * @param tagName
     * @return
     * @throws Exception
     */
    HashCollection<String,String> getExtentAllLocs(String tagName)
            throws Exception{
        HashCollection<String,String>elems = new HashCollection<String,String>();
        Statement stat = mConn.createStatement();
        String query = ("select location, file_name from extents where " +
                "element_name = '" + tagName + "';");
        ResultSet rs = stat.executeQuery(query);
        while(rs.next()) {
            elems.putEnt(rs.getString("location"),rs.getString("file_name"));
        }
        rs.close();
        return elems;
    }

    /**
     * Gets the type of a tag based on its file and ID.  Assumes
     * that no file will have an ID that is used more than once, 
     * even for different tags
     * 
     * @param fileName
     * @param id
     * @return String containing the element type
     * @throws Exception
     */
    String getElementByFileAndID(String fileName,String id)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select * from extents where id = '" 
                + id + "'" + " and file_name =  '" + fileName + "';");
        ResultSet rs = stat.executeQuery(query);
        String elemName =  rs.getString("element_name");
        rs.close();
        return elemName;
    }

    /**
     * Adds all the inserts that have been batched into the PreparedStatement
     * for the extent table in the extent table to the database
     *
     * @throws Exception
     */
    void batchExtents() throws Exception{
        mConn.setAutoCommit(false);
        mExtInsert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Used to add a single tag's worth of information to the preparedStatement
     * for the extent table.  Does not add to the database.
     *
     * @param fileName name of the file the tag is from
     * @param loc the location of the tag in the text
     * @param elemName the type of the tag being added
     * @param id the ID of the tag being added
     * @throws Exception
     */
    void addExtent(String fileName, int loc, String elemName, String id)
            throws Exception{
        mExtInsert.setString(1, fileName);
        mExtInsert.setInt(2, loc);
        mExtInsert.setString(3, elemName);
        mExtInsert.setString(4, id);
        mExtInsert.addBatch();
    }

    /**
     * Used to add a single tag's worth of information to the preparedStatement
     * for the extent table and to the database.
     * 
     * @param fileName name of the file the tag is from
     * @param loc the location of the tag in the text
     * @param elemName the type of the tag being added
     * @param id the ID of the tag being added
     * @throws Exception
     */
    void insertExtent(String fileName, int loc, String elemName, String id)
            throws Exception{
        mExtInsert.setString(1, fileName);
        mExtInsert.setInt(2, loc);
        mExtInsert.setString(3, elemName);
        mExtInsert.setString(4, id);
        mExtInsert.addBatch();
        mConn.setAutoCommit(false);
        mExtInsert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Adds all the inserts that have been batched into the PreparedStatement
     * for the link table in the extent table to the database
     * 
     * @throws Exception
     */
    void batchLinks() throws Exception{
        mConn.setAutoCommit(false);
        mLinkInsert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Used to add a single tag's worth of information to the preparedStatement
     * for the link table.  Does not add to the database.
     * 
     * @param fileName name of the file the added tag is from
     * @param newID ID of the tag being added
     * @param linkName type of the tag being added
     * @param linkFrom the id of the 'from' anchor for the link
     * @param fromName the type of the 'from' anchor for the link
     * @param linkTo the id of the 'to' anchor for the link
     * @param toName the type of the 'to' anchor for the link
     * @throws Exception
     */
    void addLink(String fileName, String newID, String linkName, String linkFrom,
                 String fromName, String linkTo, String toName) throws Exception{
        mLinkInsert.setString(1, fileName);
        mLinkInsert.setString(2, newID);
        mLinkInsert.setString(3, linkFrom);
        mLinkInsert.setString(4, fromName);
        mLinkInsert.setString(5, linkTo);
        mLinkInsert.setString(6, toName);
        mLinkInsert.setString(7, linkName);
        mLinkInsert.addBatch();
    }

    /**
     * Used to add a single tag's worth of information to the preparedStatement
     * for the link table and to the database.
     * 
     * @param fileName name of the file the added tag is from
     * @param newID ID of the tag being added
     * @param linkName type of the tag being added
     * @param linkFrom the id of the 'from' anchor for the link
     * @param fromName the type of the 'from' anchor for the link
     * @param linkTo the id of the 'to' anchor for the link
     * @param toName the type of the 'to' anchor for the link
     * @throws Exception
     */
    void insertLink(String fileName, String newID,  String linkName,
                    String linkFrom, String fromName,
                    String linkTo, String toName) throws Exception{
        mLinkInsert.setString(1, fileName);
        mLinkInsert.setString(2, newID);
        mLinkInsert.setString(3, linkFrom);
        mLinkInsert.setString(4, fromName);
        mLinkInsert.setString(5, linkTo);
        mLinkInsert.setString(6, toName);
        mLinkInsert.setString(7, linkName);
        mLinkInsert.addBatch();
        mConn.setAutoCommit(false);
        mLinkInsert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * Closes the connection to the DB
     */
    void closeDb() {
        try {
            mConn.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks to see if the given id exists for the filename.
     * 
     * @param id the ID being searched for
     * @param fileName the name of the file being searched i
     * @return true if the id exists, false if not
     * 
     * @throws Exception
     */
    boolean idExists(String id, String fileName)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select count(id) from extents where " +
                "id = '" + id + "' and file_name ='"+fileName+"';");
        ResultSet rs = stat.executeQuery(query);
        int num = rs.getInt(1);
        rs.close();
        if (num>0) {
            return true;
        }
        String query2 = ("select count(id) from links where " +
                "id = '" + id + 
                "' and file_name ='"+fileName+"';");
        ResultSet rs2 = stat.executeQuery(query2);
        int num2 = rs2.getInt(1);
        rs2.close();
        if (num2>0) {
            return true;
        }

        return false;
    }

    /**
     * Checks to see if the file has a tag at the given location
     * 
     * @param fileName the name of the file being searched
     * @param loc the location being searched
     * @return true or false, depending on if there's a tag there
     * 
     * @throws Exception
     */
    boolean tagExistsInFileAtLoc(String fileName, int loc)
            throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select count(id) from extents where " +
                "location = " + loc + " and file_name ='"+fileName+"';");
        ResultSet rs = stat.executeQuery(query);
        int num = rs.getInt(1);
        rs.close();
        if(num>0) {
            return true;
        }
        return false;
    }
    
    /**
     * removes a link tag based on file, element name and id.  Is currently
     * only called for the goldStandard file.
     * 
     * @param fileName name of the file the tag is in
     * @param elemName type of the tag being removed
     * @param id ID of the tag being removed
     * 
     * @throws Exception
     */
    void removeLinkTags(String fileName, String elemName, String id)
            throws Exception{
        printOther(elemName);
        //remove the tag from the links table
        Statement stat = mConn.createStatement();
        String delete = ("delete from links where id = '" 
                +id + "' and element_name = '" 
                + elemName + "' and file_name = '"+fileName+"';");
        stat.executeUpdate(delete);  
        //also need to remove it from the table associated with its element name
        stat = mConn.createStatement();
        delete = ("delete from " + elemName +" where id = '"
                +id + "' and file_name = '"+fileName+"';");
        stat.executeUpdate(delete);
    }

    /**
     * removes an extent tag based on file, element name and id.  Is currently
     * only called for the goldStandard file.
     * 
     * @param fileName name of the file the tag is in
     * @param elemName type of the tag being removed
     * @param id ID of the tag being removed
     * 
     * @throws Exception
     */
    void removeExtentTags(String fileName, String elemName, String id)
            throws Exception{
        //remove the tag from the extents table
        Statement stat = mConn.createStatement();
        String delete = ("delete from extents where id = '" 
                +id + "' and element_name = '" 
                + elemName + "' and file_name = '"+fileName+"';");
        stat.executeUpdate(delete); 
        
        //also need to remove it from the element_name table
        stat = mConn.createStatement();
        delete = ("delete from " + elemName +" where id = '"
                +id + "' and file_name = '"+fileName+"';");
        stat.executeUpdate(delete); 

        //finally, remove it from the overlap_extents
        if(fileName.equals(GS_FILE)) {
            stat = mConn.createStatement();
            delete = ("delete from extent_overlaps where gsid = '" 
                    +id + "' and element_name = '"+elemName+"';");
            stat.executeUpdate(delete);
        }
    }


    /**
     * Checks to see if the provided tag overlaps with tags of the same type
     * from other files.
     * mod by krim: multi span support
     * 
     * @param tag the element describing the tag being added
     * @param tagVals the Hashtable of tag information
     * 
     * @throws Exception
     */
    void addOverlaps(Elem tag, Hashtable<String, String> tagVals)
            throws Exception{
        Statement stat = mConn.createStatement();
        ArrayList<int[]> spans = MaiMain.parseSpansString(tagVals.get("spans"));
        String gsid = tagVals.get("id");

        for (int[] span : spans) {
            int start = span[0], end = span[1];
            String query = ("select distinct(id), file_name from extents" +
                    " where element_name = '" + tag.getName() +"'" +
                    " and location >= " + start +
                    " and location <=" + end +
                    " and file_name !='" + GS_FILE + "';");
            ResultSet rs = stat.executeQuery(query);
            while (rs.next()) {
                String filename = rs.getString("file_name");
                String id = rs.getString("id");
                mOverlapInsert.setString(1, gsid);
                mOverlapInsert.setString(2, tag.getName());
                mOverlapInsert.setString(3, filename);
                mOverlapInsert.setString(4, id);
                mOverlapInsert.addBatch();
            }
            rs.close();
        }
        mConn.setAutoCommit(false);
        mOverlapInsert.executeBatch();
        mConn.setAutoCommit(true);

    }

    /**
     * Finds all the overlaps with the goldStandard and other files.
     * Called when a new goldStandard file is loaded into MAI
     * mod by krim: multi-span support
     * TODO need thorough test
     * 
     * @throws Exception
     */
    void findAllOverlaps()  throws Exception{
        //first, clear out the table
        Statement stat = mConn.createStatement();
        String delete = ("delete from extent_overlaps;");
        stat.executeUpdate(delete);
        //then, find the ids and types of the GS links
        String findGSIDs = ("select distinct(id), element_name from extents" +
                " where file_name = '" + GS_FILE + "';");
        ResultSet rs = stat.executeQuery(findGSIDs);
        while(rs.next()) {

            // for each elem id and name in query result
            String gsId = rs.getString("id");
            String gsElemName = rs.getString("element_name");
            // get element spans then parse
            String spanString = getLocByFileAndID("goldstandard.xml", gsId);
            ArrayList<int[]> spans = MaiMain.parseSpansString(spanString);

            // for each span
            for (int[] span : spans) {
                int start = span[0], end = span[1];

                // query tags in the span
                Statement stat2 = mConn.createStatement();
                // find the tags from other files that overlap with the one in the GS
                String query = (String.format(
                        "select distinct(id), file_name from extents " +
                            "where element_name = '%s' and location >= %d " +
                            "and location <= %d and file_name !='" + GS_FILE + "';"
                        , gsElemName, start, end));
                ResultSet rs2 = stat2.executeQuery(query);
                while (rs2.next()) {
                    String fileName = rs2.getString("file_name");
                    String id = rs2.getString("id");
                    mOverlapInsert.setString(1, gsId);
                    mOverlapInsert.setString(2, gsElemName);
                    mOverlapInsert.setString(3, fileName);
                    mOverlapInsert.setString(4, id);
                    mOverlapInsert.addBatch();
                }
                rs2.close();
            }
        }
        rs.close();
        //add the tags to the table
        mConn.setAutoCommit(false);
        mOverlapInsert.executeBatch();
        mConn.setAutoCommit(true);
    }

    /**
     * gets the start and end locations of a tag by the file and id,
     * concatenated into a string: start,end
     * mod by krim: multi-span support
     *
     * @param fileName the file the tag is in
     * @param id the id of the tag
     * @return a string containing the start and end locations
     * of the tag being searched for.
     * @//add the tags to the table throws Exception
     */
    String getLocByFileAndID(String fileName, String id) throws Exception {
        Statement stat = mConn.createStatement();
        String query = ("select * from extents where id = '" + 
                id + "' and file_name = '" + fileName + "';");
        ResultSet rs = stat.executeQuery(query);
        ArrayList<Integer> locs = new ArrayList<Integer>();
        while (rs.next()) {
            locs.add(Integer.parseInt(rs.getString("location")));
        }
        //sort the ArrayList
        Collections.sort(locs);
        rs.close();

        // added by krim: need to make a string representing multiple spans then return it
        int initLoc, endCandi;
        initLoc = endCandi = locs.get(0);
        String s = Integer.toString(initLoc);

        if (locs.size()>1) {
            for (int loc : locs) {
                if (loc <= endCandi+1) {
                    endCandi = loc;
                }
                else {
                    s += MaiMain.SPANDELIMITER + (endCandi+1) +
                            MaiMain.SPANSEPARATOR + loc;
                    endCandi = loc;
                }
            }
        }
        s += MaiMain.SPANDELIMITER + (locs.get(locs.size()-1)+1);
        return s;
    }

    /**
     * Returns the start or the end of the string, whichever is asked for
     *
     * @param id the id of the tag
     * @param fileName the name of the file the tag is in
     * @param elemName the type of the tag
     * @param attName the attribute being searched for, here only used for "start" or "end"
     *
     * @return an integer of start or end offset
     */
    private int getStartOrEnd(String id, String fileName, String elemName, String attName) {
        try {
            Statement stat = mConn.createStatement();
            String query = ("select * from " + elemName + " where file_name = '" +
                    fileName + "' and id = '" + id + "';");
            ResultSet rs = stat.executeQuery(query);
            String att = rs.getString(attName);
            rs.close();
            return(Integer.parseInt(att));
        } catch(Exception e) {
            e.printStackTrace();
            return -2;
        }
    }

    /**
     * Gets all instances of the requested link tag that exist in the gold standard.
     * This method is called when a link tag is selected from the radio buttons in MAI.
     * This is a complicated procedure, because it has to determine what link tags from the 
     * files being adjudicated have anchors that overlap with extents in the gold standard, and 
     * return a list of the locations in the gold standard that overlap with the link anchors 
     * from the other files.
     * 
     * It also fills in the information in the mCurLinks hash, which keeps track of what links 
     * from other files have overlaps so that when extents are selected the process of filling in the
     * adjudication table is sped up.
     * 
     * TODO: refactor to make more efficient, use multiple database connections
     * 
     * @param tagname the name of the tag being evaluated
     * 
     * @return a hashcollection with all the locations and file names where
     * link anchors overlap with the gold standard
     * @throws Exception
     */
    HashCollection<String,String> getGSLinksByType(String tagname)
            throws Exception{

        //keep track of relevant links, reset each time a 
        //new link tag is selected
        mCurLinks.clear(); 

        HashCollection<String,String>links = new HashCollection<String,String>();

        Statement stat = mConn.createStatement();
        String query = ("select * from links where element_name = '" +
                tagname + "';");
        ResultSet rs = stat.executeQuery(query);

        Hashtable<String,String>inGS = new Hashtable<String,String>();
        Hashtable<String,String>inOther = new Hashtable<String,String>();

        while(rs.next()) {
            //this needs to be re-written
            if(rs.getString("file_name").equals(GS_FILE)) {
                String newId = (rs.getString("file_name") + SEP
                        + rs.getString("fromid") + SEP
                        + rs.getString("from_name"));
                inGS.put(newId,"");
                newId = (rs.getString("file_name") + SEP
                        + rs.getString("toid") + SEP
                        + rs.getString("to_name"));
                inGS.put(newId,"");
                mCurLinks.add(rs.getString("file_name") + SEP
                        + rs.getString("id"));
            } else {
                //if the link isn't in the GS, we only want to highlight
                //these extents if they both have overlaps in the GS
                String newId = (rs.getString("file_name") + SEP
                        + rs.getString("fromid") + SEP
                        + rs.getString("from_name") + SEP
                        + rs.getString("file_name") + SEP
                        + rs.getString("toid") + SEP
                        + rs.getString("to_name") + SEP
                        + rs.getString("id"));
                inOther.put(newId,"");
            }
        }
        rs.close();

        for(Enumeration<String> ids = inOther.keys() ; ids.hasMoreElements() ;) {
            //if the ids being examined don't come from a GS link,
            //we need to get the corresponding GS id (based on overlaps)
            //both IDs must have overlaps for either to be included 
            //in the hashtable
            String id = ids.nextElement();
            boolean hasToOverlap = false;
            boolean hasFromOverlap = false;
            ArrayList<String> overlaps = new ArrayList<String>();
            String filename = id.split(SEP)[0];
            String fileTagId = id.split(SEP)[1];

            query = ("select gsid from extent_overlaps where element_name ='" +
                    id.split(SEP)[2]+"' and file_name ='" + filename +
                    "'and fileid='" + fileTagId +"';");

            rs=stat.executeQuery(query);
            while (rs.next()) {
                String newId = (filename+ SEP +rs.getString("gsid")
                        + SEP +id.split(SEP)[2]);
                overlaps.add(newId);
                hasToOverlap = true;
            }
            rs.close();

            fileTagId = id.split(SEP)[4];
            query = ("select gsid from extent_overlaps where element_name ='" +
                    id.split(SEP)[5]+"' and file_name ='" + filename +
                    "'and fileid='" + fileTagId +"';");

            rs=stat.executeQuery(query);
            while (rs.next()) {
                String newId = (filename+ SEP +rs.getString("gsid")
                        + SEP +id.split(SEP)[5]);
                overlaps.add(newId);
                hasFromOverlap = true;
            }
            rs.close();

            if (hasToOverlap && hasFromOverlap) {
                for (String overlap : overlaps) {
                    inGS.put(overlap, "");
                    mCurLinks.add(filename + SEP + id.split(SEP)[6]);
                }
            }

        }

        //now that we have all the overlapping GS ids, we can 
        for (Enumeration<String> ids = inGS.keys() ; ids.hasMoreElements() ;) {
            String id = ids.nextElement();
            String filename = id.split(SEP)[0];
            query = ("select location from extents where file_name = '" +
                    GS_FILE + "' and element_name = '" + id.split(SEP)[2] +
                    "' and id = '" + id.split(SEP)[1] + "';");
            rs = stat.executeQuery(query);
            while (rs.next()) {
                links.putEnt(rs.getString("location"),filename);
            }
            rs.close();

        }

        return links;

    }

    /**
     * Returns a HashCollection of link IDs from a file where the given extent id is an anchor
     * 
     * @param fileName the name of the file being searched
     * @param elemName the type of element being searched for in the anchor
     * @param id the id of the anchor being searched for
     * @return A HashCollection where the keys are the type of element and the 
     * values are link IDs
     * @throws Exception
     */
    HashCollection<String,String> getLinksByFileAndExtentID(
            String fileName, String elemName, String id) throws Exception{
        HashCollection<String,String>links = new HashCollection<String,String>();
        //first get the links where the extent being searched for is the 
        //'from' anchor
        Statement stat = mConn.createStatement();
        String query = ("select id,element_name from links where fromid = '" +
                id + "' and from_name  ='" + elemName + "' and file_name = '"+
                fileName + "';");
        ResultSet rs = stat.executeQuery(query);
        while(rs.next()) {
            links.putEnt(rs.getString("element_name"),rs.getString("id"));
        }
        rs.close();
        //then get the ones where the extent is the 'to' anchor
        String query2 = ("select id,element_name from links where toid = '" +
                id + "' and to_name  ='" + elemName + "' and file_name = '"+
                fileName + "';");
        ResultSet rs2 = stat.executeQuery(query2);
        while(rs2.next()) {
            links.putEnt(rs2.getString("element_name"),rs2.getString("id"));
        }
        rs2.close();
        return links;
    }

    /**
     * Finds the tags of a particular type that exist in an extent and 
     * returns the filenames as keys and the IDs as values.  Used to fill
     * in the table in MAI when an extent is selected.
     * 
     * @param begin the starting offset of the selected extent
     * @param end the ending offset of the selected extent
     * @param tagName the type of tag being searched for
     * @return HashCollection with file names as keys and IDs as values
     * 
     * @throws Exception
     */
    HashCollection<String,String> getTagsInSpanByType(
            int begin, int end, String tagName) throws Exception{
        Statement stat = mConn.createStatement();
        String query;
        if(begin!=end) {
            query = ("select distinct(id), file_name from extents " +
                    "where element_name = '"+tagName+"' and location >= " 
                    + begin + " and location <=" + end + ";");
        }
        else{
            query = ("select distinct(id), file_name from extents where location = " 
                    + begin + " and element_name = '"+tagName+"';");
        }

        ResultSet rs = stat.executeQuery(query);
        HashCollection<String,String> tags = new HashCollection<String,String>();
        while(rs.next()) {
            tags.putEnt(rs.getString("file_name"),rs.getString("id"));
        }
        rs.close();
        return tags;
    }

    /**
     * This function is called when a user selects an extent in the text window
     * and returns the tag information that will be used to fill in the table.
     * While links are collected from all the files entered into the adjudication,
     * the text and extent IDs that are shown in the links are ones from the GS.
     * This helps ensure that only links with both anchors in (or at least overlapping with)
     * the goldStandard are displayed, but it does make the function much more complicated.
     *  
     * <p>
     * This method also tracks the locations of the extents that should be 
     * highlighted in the text to reflect where the annotations and gold standard 
     * placed the other extents associated with the selected text.  This information 
     * is kept in the mCurHL hash.
     *
     * mod by krim: multi-span support
     * <p>
     * TODO: refactor, use more nested DB queries, remove assumption about
     * link ends only having one tag per document
     * <p>
     * @param begin the beginning offset of the selected extent
     * @param end the ending offset of the selected extent
     * @param tagName the type of tag being searched for
     * @param atts the attributes of the tag being searched for
     * @return a HashCollection of tag information, keyed by filename
     * @throws Exception
     */
    HashCollection<String,Hashtable<String,String>> getLinkTagsInSpanByType(
            int begin, int end,
            String tagName, ArrayList<Attrib> atts) throws Exception{
        //based on the new selection, the highlights in the text window will
        //be changed; keep track of those here
        mCurHL.clear();
        int[] inputSpan = new int[]{begin, end};

        HashCollection<String,Hashtable<String,String>> gsLinkExtents = 
                new HashCollection<String,Hashtable<String,String>>();

        HashCollection<String,Hashtable<String,String>> gsTempLinkExtents = 
                new HashCollection<String,Hashtable<String,String>>();

        Statement stat = mConn.createStatement();

        for (String link : mCurLinks) {
            String fileName = link.split(SEP)[0], linkId = link.split(SEP)[1];

            //first, grab the info for each of the links that are being considered
            String query = ("select * from " + tagName + " where id = '" +
                    linkId + "' and file_name = '" + fileName + "';");
            ResultSet rs = stat.executeQuery(query);
            Hashtable<String, String> linkElems = new Hashtable<String, String>();
            while (rs.next()) {
                for (Attrib att : atts) {
                    linkElems.put(att.getName(), rs.getString(att.getName()));
                }
                //use TempLinkExtents so that only links with overlaps will be
                //passed back
                gsTempLinkExtents.putEnt(fileName, linkElems);
            }
            rs.close();
        }

        //next, go through each link, check to see if either end overlaps with
        //the selected text, then
        //find the GS replacements for the From
        ArrayList<String> fileNames = gsTempLinkExtents.getKeyList();
        for (String fileName : fileNames) {
            ArrayList<Hashtable<String, String>> links
                    = gsTempLinkExtents.get(fileName);
            for (Hashtable<String, String> link : links) {
//                Hashtable<String, String> link = link1;

                boolean fromOverlap = false, toOverlap = false;
                String fromid = link.get("fromID");
                String fromElemName = getElemNameByFileAndId(fileName, fromid);
                ArrayList<int[]> fromSpans = getSpansByFileElemAndId(
                        fromElemName, fileName, fromid);
                int[] fromOLSpan = fromSpans.get(0);
                if (isOverlapped(fromSpans, inputSpan) != null) {
                    fromOLSpan = isOverlapped(fromSpans, inputSpan);
                    fromOverlap = true;
                }

                String toid = link.get("toID");
                String toElemName = getElemNameByFileAndId(fileName, toid);
                ArrayList<int[]> toSpans = getSpansByFileElemAndId(
                        toElemName, fileName, toid);
                int[] toOLSpan = toSpans.get(0);
                if (isOverlapped(toSpans, inputSpan) != null) {
                    toOLSpan = isOverlapped(toSpans, inputSpan);
                    toOverlap = true;
                }

                boolean overlap = fromOverlap || toOverlap;

                //if there's an overlap, proceed with replacing the ids and text
                if (overlap) {
                    //add overlaps to mCurHL
                    if (!fromOverlap) {
                        mCurHL.putList(fileName, fromSpans);
                    }
                    if (!toOverlap) {
                        mCurHL.putList(fileName, toSpans);
                    }

                    //first, swap out fromID and fromText
                    String newFromID = getIdWithinSpanInGSByElem(fromElemName, fromOLSpan);
                    link.put("fromID", newFromID);
                    try {
                        link.put("fromText",
                                getTextByFileElemAndID(GS_FILE, fromElemName, newFromID));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //now, do the same for toID and toText
                    String newToID = getIdWithinSpanInGSByElem(toElemName, toOLSpan);
                    link.put("toID", newToID);
                    try {
                        link.put("toText",
                                getTextByFileElemAndID(GS_FILE, toElemName, newToID));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    //add new link info to HashCollection being sent back to MAI
                    gsLinkExtents.putEnt(fileName, link);
                }
            }
        }
        return gsLinkExtents;
    }

    /**
     * Check if any span in target list of spans overlaps with source span
     * added by krim
     *
     * @param targets list of spans to check
     * @param source a span to compare with
     * @return first overlapping span (or null)
     */
    private int[] isOverlapped(ArrayList<int[]> targets, int[] source) {
        for (int[] target: targets) {
            if ((source[0] <= target[0] && target[0] <= source[1]) ||
                    (source[0] <= target[1] && target[1] <= source[1]) ||
                    (source[1] <= target[1] && target[0] <= source[0])) {
                return target;
            }
        }
        return null;
    }

    /**
     * Get element name using filename and id
     * added by krim
     *
     */
    private String getElemNameByFileAndId(
            String fileName, String id) throws Exception {
        Statement stat = mConn.createStatement();

        // need to get type of extent to ensure compatibility
        String query = ("select * from extents " +
                "where file_name = '" + fileName +
                "' and id = '" + id + "';");
        ResultSet rs = stat.executeQuery(query);
        String elemName = rs.getString("element_name");
        rs.close();

        return elemName;
    }

    /**
     * get element spans using filename and element name and its id
     * added by krim
     *
     */
    private ArrayList<int[]> getSpansByFileElemAndId(
            String fileName, String elemName, String id) throws Exception {
        Statement stat = mConn.createStatement();
        //check to see if ID overlaps with selected text
//                query = ("select start, end from " + fromElemName +
        String query = ("select spans from " + elemName
                + " where id = '" + id
                + "' and file_name = '" + fileName + "';");
        ResultSet rs = stat.executeQuery(query);
        ArrayList<int[]> spans
                = MaiMain.parseSpansString(rs.getString("spans"));
        rs.close();

        return spans;
    }

    /**
     * get element id from given a span of text in the gold standard,
     * using an element name
     * added by krim
     *
     */
    private String getIdWithinSpanInGSByElem(String elemName, int[] targetSpan)
            throws Exception {
        Statement stat = mConn.createStatement();
        int start = targetSpan[0], end = targetSpan[1];
        String query = ("select distinct(id) from extents "
                + "where element_name = '" + elemName + "'"
                + " and file_name = '" + GS_FILE + "'"
                + " and location >=" + start
                + " and location <=" + end + ";");
        ResultSet rs = stat.executeQuery(query);
        //NOTE: assumes there will be a one-to-one overlap
        //may need to be fixed in later versions
        // TODO need to modify for multi link support
        String id = rs.getString("id");
        rs.close();

        return id;
    }

    /**
     * Retrieves the text of an extent tag by using the id, type, and file
     * of the tag
     * 
     * @param fileName name of the file the tag is in
     * @param elemName type of the tag being searched for
     * @param id ID of the tag
     * @return the text of the tag
     * @throws Exception
     */
    String getTextByFileElemAndID(
            String fileName, String elemName, String id) throws Exception{
        Statement stat = mConn.createStatement();
        String query = ("select text from " + elemName +
                " where file_name = '"+ fileName +"' and id = '"
                + id + "';");
        ResultSet rs = stat.executeQuery(query);
        String text = rs.getString("text");
        rs.close();
        return text;

    }

    /**
     * Returns IDs of all the tags located over an extent, as well as 
     * all non-consuming tags from that file.
     * 
     * @param fileName the file being searched
     * @param begin the beginning of the selected extent
     * @param end the end of the selected extent
     * @return a HashCollection of tag information keyed by element_name
     * @throws Exception
     */
    HashCollection<String,String> getFileTagsInSpanAndNC(
            String fileName,int begin, int end) throws Exception{
        Statement stat = mConn.createStatement();
        String query;
        if(begin!=end) {
            query = ("select distinct(id), element_name from extents where location >= " 
                    + begin + " and location <=" + end + " and file_name ='" + fileName + "';");
        }
        else{
            query = ("select distinct(id), element_name from extents where location = " 
                    + begin + " and file_name = '" + fileName+ "';");
        }

        ResultSet rs = stat.executeQuery(query);
        HashCollection<String,String> tags = new HashCollection<String,String>();
        while(rs.next()) {
            tags.putEnt(rs.getString("element_name"),rs.getString("id"));
        }
        rs.close();

        //now get the non-consuming tags
        query = ("select distinct(id), element_name from extents where location = -1;");
        rs = stat.executeQuery(query);
        while(rs.next()) {
            tags.putEnt(rs.getString("element_name"),rs.getString("id"));
        }
        rs.close();

        return tags;
    }

    /**
     * Returns the HashCollection mCurHL for use 
     * in highlighting the appropriate extents in MAI's text area.
     * 
     * @return
     */
    public HashCollection<String,int[]> getCurrentHighlights() {
        return mCurHL;
    }

    //Below are a series of methods for printing the information in the
    //database.  They aren't terribly useful because it's easier and
    //more accureate to check the DB through the command line if necessary,
    //but they're nice to have around.

    /**
     * Prints the extent table
     */
    public void printExtents() {
        System.out.println("Extents in DB:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from extents;");
            int i = 0;
            while (rs.next()) {
                if (i % 10 == 0) {
                    System.out.printf("%20s\t%20s\t%20s%n",
                            "location", "element", "id");
                }
                System.out.printf("%20s\t%20s\t%20s%n",
                        rs.getString("location"),
                        rs.getString("element_name"),
                        rs.getString("id"));
                i++;
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Prints the unique ids in the extent table
     */
    public void printUniqueExtents() {
        System.out.println("Extents in DB:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select distinct(id), file_name, element_name from extents;");
            while (rs.next()) {
                System.out.println("file name = " + rs.getString("file_name"));
                System.out.println("element_name = " + rs.getString("element_name"));
                System.out.println("id = " + rs.getString("id"));
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Prints the links in the DB
     */
    public void printLinks() {
        System.out.println("Links in DB:");
        //links (id,fromid,from_name,toid,to_name,element_name);");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from links;");
            int i = 0;
            while (rs.next()) {
                if (i % 10 == 0) {
                    System.out.printf("%20s\t%20s\t%20s\t%20s\t%20s%%%\t%20s%n",
                            "id", "from", "f_name", "to", "t_name", "element");
                }
                System.out.printf("%20s\t%20s\t%20s%n",
                        rs.getString("id"),
                        rs.getString("fromid"),
                        rs.getString("from_name"),
                        rs.getString("toid"),
                        rs.getString("to_name"),
                        rs.getString("element_name"));
                i++;
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Prints the extent_overlaps table
     */
    public void printOverlaps() {
        System.out.println("\nExtent overlaps:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from extent_overlaps;");
            while (rs.next()) {
                System.out.println("gsid = " + rs.getString("gsid"));
                System.out.println("element_name = " + rs.getString("element_name"));
                System.out.println("file_name = " + rs.getString("file_name"));
                System.out.println("fileid = " + rs.getString("fileid"));
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints basic information about the table with the 
     * provided tag name.
     * 
     * @param extName the name of the tag/table to be printed.
     */
    private void printOther(String extName) {
        System.out.println("\nTag info:");
        try {
            Statement stat = mConn.createStatement();
            ResultSet rs = stat.executeQuery("select * from "+ extName + ";");
            while (rs.next()) {
                System.out.println("id = " + rs.getString("id"));
                System.out.println("file_name = " + rs.getString("file_name"));
            }
            rs.close();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
