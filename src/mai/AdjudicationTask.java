
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

import java.util.*;

/** 
 * AdjudicationTask serves as a go-between for MaiGui and the 
 * SQLite interface adjudDB, and also manages the ID 
 * assignments for the gold standard file.
 * <p>
 * The majority of methods in this file just provide error 
 * catching for the methods in AdjudDB.
 * 
 * @author Amber Stubbs 
 * @revised Keigh Rim
 */


class AdjudicationTask {

    private Hashtable<String,Elem> mElements;
    private Hashtable<String,AttID> mIdTracker;

    private AdjudDB mDB;
    private DTD mDtd;
    private boolean hasDTD;

    /**
     * Creates a new AdjudicationTask object and accompanying database
     */
    AdjudicationTask() {
        mDB = new AdjudDB();
        hasDTD = false;
    }

    /**
     * resets the database
     */
    void reset_db() {
        mDB.closeDb();
        mDB = new AdjudDB();
    }

    /**
     * Clears the mIdTracker hashtable
     */
    void reset_IDTracker() {
        mIdTracker = createIDTracker();
    }

    /**
     * Calls the DB to create all the necessary tables
     */
    void addDTDtoDB() {
        mDB.addDTD(mDtd);
    }

    void addTagsFromHash(String fullName,
            HashCollection<String,Hashtable<String,String>> newTags) {
        mDB.addTagsFromHash(fullName, mDtd, newTags);
    }

    /**
     * called when a goldStandard file is added to the task
     */
    void findAllOverlaps() {
        try{
            mDB.findAllOverlaps();
        }
        catch(Exception e) {
            e.printStackTrace();
            System.out.println("help, error finding extent overlaps!");
        }
    }

    /**
     * Adds a tag (with information about attribute values in a Hashtable)
     * to the database
     * 
     * @param fileName name of the file the tag is from
     * @param tag the type of Elem (tag) being added
     * @param tagVals Hashtable with information about the tag
     */
    void addTagFromHash(String fileName,Elem tag, Hashtable<String,String> tagVals) {
        if (tag instanceof ElemExtent) {
            mDB.usePreparedExtentStatements(fileName, tag, tagVals);
            try{
                mDB.batchExtents();
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("help, error in batch extents!");
            }
            try{
                mDB.batchElement(tag);
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("help, error in batch element extent!");
            }
            //also, check for overlaps and add them to the extent_overlaps table
            if(fileName.equals("goldStandard.xml")) {
                try{
                    mDB.addOverlaps(tag, tagVals);
                } catch(Exception ex) {
                    ex.printStackTrace();
                    System.out.println("help, error in finding extent overlaps!");
                }
            }
        }
        else if (tag instanceof ElemLink) {
            mDB.usePreparedLinkStatements(fileName, tag, tagVals);
            try{
                mDB.batchLinks();
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("help, error in batchLinks link!");
            }
            try{
                mDB.batchElement(tag);
            } catch(Exception ex) {
                ex.printStackTrace();
                System.out.println("help, error in batchElement link!");
            }
        }
        else{
            System.out.println("error!  element type not found");
        }
    }


    /**
     * Creates the hastable of DTD Elements
     * @return
     */
    private Hashtable<String,Elem> createHash() {
        Hashtable<String,Elem> es=new Hashtable<String,Elem>();
        ArrayList<Elem>elems = mDtd.getElements();
        for(int i=0;i<elems.size();i++) {
            es.put(elems.get(i).getName(),elems.get(i));
        } 
        return(es);
    }

    /**
     * The IDTracker hashtable keeps one ID for each element that
     * has an ID, and increments the number so that no two 
     * tags of the same type will have the same ID.  In MAI this 
     * is used only for the Gold Standard file (other files can't be edited and 
     * are assumed to already have only one tag per ID)
     * 
     * @return
     */
    private Hashtable<String,AttID> createIDTracker() {
        Hashtable<String,AttID> ids = new Hashtable<String,AttID>();
        ArrayList<Elem>elems = mDtd.getElements();
        for (Elem elem : elems) {
            ArrayList<Attrib> attribs = elem.getAttributes();
            for (Attrib attrib : attribs) {
                if (attrib instanceof AttID) {
                    AttID oldid = (AttID) attrib;
                    AttID id = new AttID(oldid.getName(),
                            oldid.getPrefix(), true);
                    id.setNumber(0);
                    ids.put(elem.getName(), id);
                }
            }
        }        
        return ids;
    }

    /**
     * Finds the next available ID for an element and returns it.
     * 
     * @param element tag type
     * @param fileName name of the file the ID is for
     * @return the next ID for that element
     */
    String getNextID(String element,String fileName) {
        AttID id = mIdTracker.get(element);
        String nextid = id.getID();
        id.incrementNumber();
        //check to see if nextid is already in db
        //this will catch cases where two tags have
        //the same prefix
        try{
            while(mDB.idExists(nextid,fileName)) {
                nextid = id.getID();
                id.incrementNumber();
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return nextid;
    }

    HashCollection<String,String> getExtentAllLocs(String tagname) {
        try{
            return(mDB.getExtentAllLocs(tagname));
        } catch(Exception e) {
            e.printStackTrace();
            return (new HashCollection<String,String>());
        }
    }

    ArrayList<String> getFilesAtLocbyElement(String elem, int loc) {
        try{
            return mDB.getFilesAtLocbyElement(elem,loc);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    ArrayList<String> getExtentTagsByFileAndType(String file, Elem elem) {
        try{
            return mDB.getExtentTagsByFileAndType(file,elem);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    ArrayList<String> getLinkTagsByFileAndType(String file, Elem elem) {
        try{
            return mDB.getLinkTagsByFileAndType(file,elem);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String getTextByFileElemAndID(String file, String elem, String id) {
        try{
            return mDB.getTextByFileElemAndID(file,elem,id);
        } catch(Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    Hashtable<String,String> getAllExtentsByFile(String file) {
        try{
            return mDB.getAllExtentsByFile(file);
        } catch(Exception e) {
            e.printStackTrace();
            return new Hashtable<String,String>();
        }
    }

    boolean tagExistsInFileAtLoc(String file, int loc) {
        try{
            return mDB.tagExistsInFileAtLoc(file,loc);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("help!");
            return false;
        }
    }


    Hashtable<String,String> getTagsByFileAndID(
            String tagName, String id, String fileName) {
        try{
            ArrayList<Attrib> atts = mDtd.getElem(tagName).getAttributes();
            return mDB.getTagsByFileAndID(tagName, id, fileName, atts);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    String getLocByFileAndID(String file, String id) {  
        try{
            return mDB.getLocByFileAndID(file,id);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    HashCollection<String,String>findGoldStandardLinksByType(String tagname) {
        try{
            return mDB.getGSLinksByType(tagname);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return new HashCollection<String,String>();

    }

    void removeExtentByFileAndID(String fullName,String e_name,String id) {
        try{
            mDB.removeExtentTags(fullName, e_name, id);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    void removeLinkByFileAndID(String fullName,String e_name,String id) {
        try{
            mDB.removeLinkTags(fullName, e_name, id);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    HashCollection<String,String> getLinksByFileAndExtentID(String file,String e_name,String id) {
        try{
            return(mDB.getLinksByFileAndExtentID(file,e_name,id));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return (new HashCollection<String,String>());
    }

    /**
     * Sets the DTD for the Adjudication Task so 
     * information about the files being adjudicated 
     * are easily available.
     * 
     * @param d the object describing the task's DTD
     */
    public void setDTD(DTD d) {
        mDtd =d;
        mElements = createHash();
        mIdTracker = createIDTracker();
        hasDTD=true;
    }

    /**
     * Returns all the Elem objects in the DTD
     * 
     * @return
     */
    public ArrayList<Elem> getElements() {
        return mDtd.getElements();
    }


    /**
     * Get all tags of a specific type from given spans of text
     * Added by krim: support multi-span mode
     *
     * @param spans - a list of text spans (start-end pairs)
     * @param tagName - a name of tag type
     * @return
     */
    HashCollection<String,String> getTagsWithinSpansByType(ArrayList<int[]> spans,
                                                           String tagName) {
        HashCollection<String, String> tagsAndAtts = new HashCollection<String, String>();
        for (int[] span : spans) {
            tagsAndAtts.putAll(getTagsSpanByType(span[0], span[1], tagName));
        }
        return tagsAndAtts;
    }

    /**
     * Get all tags of a specific type which is non-consuming or from given spans of text
     * Added by krim: support multi-span mode
     *
     * @param file - an annotation file name
     * @param spans - a list of text spans (start-end pairs)
     * @return
     */
    HashCollection<String,String> getFileTagsSpanAndNC(String file, ArrayList<int[]> spans) {
        HashCollection<String, String> tagsAndAttsWithNC = new HashCollection<String, String>();
        for (int[] span : spans) {
            tagsAndAttsWithNC.putAll(getFileTagsSpanAndNC(file, span[0], span[1]));
        }
        return tagsAndAttsWithNC;
    }

    /**
     * Get all link tags of a specific type from given spans of text
     * Added by krim: support multi-span mode
     *
     * @param spans - a list of text spans (start-end pairs)
     * @param tagName - a name of tag type
     * @return
     */
    HashCollection<String,Hashtable<String,String>> getLinkTagsSpanByType
       (ArrayList<int[]> spans, String tagName) {
        HashCollection<String, Hashtable<String, String>> linkTagsAndAtts = new HashCollection<String, Hashtable<String, String>>();
        for (int[] span : spans) {
            linkTagsAndAtts.putAll(getLinkTagsSpanByType(span[0], span[1], tagName));
        }
        return linkTagsAndAtts;
    }

    HashCollection<String,String> getTagsSpanByType(int begin, int end, 
            String tag) {
        try{
            return (mDB.getTagsInSpanByType(begin,end,tag));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    HashCollection<String,Hashtable<String,String>> getLinkTagsSpanByType
       (int begin, int end, String tagname) {
        try{
            ArrayList<Attrib> atts = mDtd.getElem(tagname).getAttributes();
            return (mDB.getLinkTagsInSpanByType(begin,end,tagname,atts));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    HashCollection<String,String> getFileTagsSpanAndNC(String file, int begin, int end) {
        try{
            return (mDB.getFileTagsInSpanAndNC(file, begin,end));
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    public HashCollection<String,int[]>getCurrentHighlights() {
        return mDB.getCurrentHighlights();
    }

    public ArrayList<String> getExtentElements() {
        ArrayList<String> extents = new ArrayList<String>();
        ArrayList<Elem> elems = mDtd.getElements();
        for(int i=0;i<elems.size();i++) {
            Elem e = elems.get(i);
            if(e instanceof ElemExtent) {
                extents.add(e.getName());
            }
        }
        return extents;
    }
    
    /**
     * Returns only non-consuming elements in the DTD
     * 
     * @return
     */
    public ArrayList<Elem> getNCElements() {
        return mDtd.getNCElements();
    }

    public ArrayList<String> getLinkElements() {
        ArrayList<String> links = new ArrayList<String>();
        ArrayList<Elem> elems = mDtd.getElements();
        for (Elem e : elems) {
            if (e instanceof ElemLink) {
                links.add(e.getName());
            }
        }
        return links;
    }

    public Hashtable<String,Elem> getElemHash() {
        return mElements;
    }

    Elem getElem(String name) {
        return mElements.get(name);
    }

    boolean hasDTD() {
        return hasDTD;
    }

    public String getDTDName() {
        return mDtd.getName();
    }

}