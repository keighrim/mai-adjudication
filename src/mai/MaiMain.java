package mai;

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

/*
compiling:
javac -Xlint:unchecked -cp sqlitejdbc-v056.jar *.java
java -cp .:sqlitejdbc-v056.jar MaiGui

java 1.5: javac -target 1.5 -Xlint:unchecked -cp sqlitejdbc-v056.jar *.java

making the .jar file (after compiling):
jar cvfm Mai.jar manifest.txt *.class
java -jar Mai.jar

 */

import java.awt.*;
import java.awt.event.*;
import java.lang.Exception;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.table.TableColumn;
import javax.swing.table.DefaultTableModel;

import java.io.*;
import java.util.*;
import java.util.Timer;

/**
 * MaiGui is the main class for MAI; it manages all the GUI attributes 
 * and manages how the annotation/adjudication information is loaded, interacted with,
 * and displayed.
 * <p>
 * All files loaded into MAI must be UTF-8 encoded, otherwise the character offsets
 * cannot be gauranteed to work.
 * 
 * 
 * @author Amber Stubbs
 * @revised Keigh Rim
 * @version 0.10.0, Dec 2014
 *
 */

public class MaiMain extends JPanel{

    // added by krim: constant strings to be used in GUI title bar
    // krim: made the version number correspond to MAE
    protected final static String VERSION = "0.10.0";
    protected final static String TITLE_PREFIX = "MAI " + VERSION;

    // added by krim: constant string to be used in string representation of mSpans
    protected final static String SPANDELIMITER = "~";
    protected final static String SPANSEPARATOR = ",";
    protected final static String SPANTEXTTRUNC = " ... ";

    private static final long serialVersionUID = -6122390155866896831L;
    private Hashtable<String, Color> mColorTable;

    //Here is where to change the colors that get assigned to tags
    private Color mBlack = Color.black;
    private Color mBlue = Color.blue;
    private Color mRed = Color.red;
    private Color mGreen = Color.green;
    private Color mYellow = Color.yellow;
    private Color mCyan = Color.cyan;
    private Color mPink = Color.pink;

    private Color mMagenta = Color.magenta;
    private Color mDarkOrange = new Color(153,102,0);
    private Color mLightOrange = new Color(255,204,51);
    private Color mLightBlue = new Color(0,172,188);
    private Color mOrange = new Color(234,160,0);
    private Color mPurple = new Color(102,75,153);
    private Color mGray = Color.lightGray;

    private Color[] mColors = {
            mMagenta, mDarkOrange, mLightOrange, mLightBlue, mOrange, mPurple, mGray };

    // added by krim: thses highlight pens would be used in text panel to indicate text selection, etc.
    private TextHighlightPainter mCyanHL = new TextHighlightPainter(mCyan);
    private TextHighlightPainter mGreenHL = new TextHighlightPainter(mGreen);
    private TextHighlightPainter mGrayHL = new TextHighlightPainter(mGray);
    private TextHighlightPainter mDefHL
            = new TextHighlightPainter(SystemColor.textHighlight);

    //private Hashtable<String,Object> highlighterTable;

    //keeps track of what parts of the text
    //have been visited by the user when
    //looking at link tags.
    private HashCollection<String,Integer> mVisited;

    private boolean isFileOpen;
    private boolean isTextSelected;
    private boolean isCtrlPressed;

    // add by krim: additional booleans to keep track of annotation mode
    private final int NORMAL = 0;
    private final int MSPAN = 1;
    private final int MLINK = 2;
    private int mMode;

    private String linkFrom;
    private String linkName;
    private String linkTo;

    private int mPrevSpan; // this will be used when adding link (ctrl+drag UI)
    private ArrayList<int[]> mSpans; // added krim
    // instead of using 2 integers, start & end, now we use a set of tuples

    private int mTableRow;
    private int mTableCol;
    private String mCurTag;

    private JFileChooser mLoadDTDFC;
    private JFileChooser mFileLoadFC;
    private JFileChooser mSaveFC;

    private static JFrame mMainFrame;
    private JPanel mTopPanel;
    private JTextPane mTextPane;
    private JScrollPane mScrollPane;

    private JFrame mLinkPopupFrame;

    private JPanel mLeftPanel;
    private ButtonGroup mTagButtons;

    private JPanel mBottomPanel;
    private TableListener mTableListener;
    private AnnJTable mTagTable;

    private JMenuBar mMenuBar;
    private JMenu mFileMenu;
    private JMenu mDisplayMenu;
    //private JMenu nc_tags;
    // add by krim: this mode menu will be used for toggling special input mode
    // eg> multiple-span extent tag
    private JMenu mModeMenu;
    private JMenu mHelpMenu;

    private JLabel mStatusBar;

    // mod by krim: refactored names to disambiguate
    private JPopupMenu mTextPopup;
    private JPopupMenu mTablePopup;

    private static AdjudicationTask mTask;
    private ArrayList<String> mFiles;
    private String GS_FILE = "goldStandard.xml";

    public MaiMain() {
        super(new BorderLayout());

        mTask = new AdjudicationTask();

        mVisited = new HashCollection<String,Integer>();

        isFileOpen = false;
        isTextSelected =false;
        isCtrlPressed =false;

        mPrevSpan = -1;
        mCurTag = null;
        mMode = NORMAL;
        // mod by krim: init start-end to (-1, -1) pair
        mSpans = new ArrayList<int[]>();
        resetSpans();

        linkFrom="";
        linkName="";
        linkTo="";

        mTableRow = -1;
        mTableCol = -1;

        mFiles = new ArrayList<String>();
        mFiles.add(GS_FILE);
        mColorTable = new Hashtable<String,Color>();

        /* GUI components */
        mLoadDTDFC = new JFileChooser(".");
        mLoadDTDFC.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        mFileLoadFC = new JFileChooser(".");
        mFileLoadFC.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        mSaveFC = new JFileChooser(".");
        mSaveFC.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        mTextPopup = new JPopupMenu();
        mTablePopup = new JPopupMenu();
        mLinkPopupFrame = new JFrame();

        mTagTable = new AnnJTable();
        mTableListener = new TableListener();

        mTextPane = new JTextPane(new DefaultStyledDocument());
        mTextPane.setEditable(false);
        mTextPane.setContentType("text/plain; charset=UTF-8");
        mTextPane.addKeyListener(new ModKeyListener());
        mTextPane.addCaretListener(new AnnCaretListener());
        mTextPane.addMouseListener(new TextMouseListener());

        mScrollPane = new JScrollPane(mTextPane);

        mStatusBar = new JLabel();
        updateStatusBar();

        mTopPanel = new JPanel(new BorderLayout());
        mTopPanel.add(mScrollPane, BorderLayout.CENTER);
        mTopPanel.add(mStatusBar, BorderLayout.SOUTH);

        mLeftPanel = new JPanel(new GridLayout(0,1));
        mTagButtons = new ButtonGroup();
        JLabel lab = new JLabel("DTD tags");
        mLeftPanel.add(lab);

        mBottomPanel = new JPanel(new GridLayout(0,1));

        // krim: display and help menus have constant, context-free items
        // thus we create it only once here in the constructor
        mDisplayMenu = createDisplayMenu();
        mHelpMenu = createHelpMenu();
        mMenuBar = new JMenuBar();
        updateMenus();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, mTopPanel, mBottomPanel);

        addKeyListener(new ModKeyListener());
        add(mMenuBar,BorderLayout.NORTH);
        add(mLeftPanel,BorderLayout.WEST);
        add(splitPane,BorderLayout.CENTER);
        splitPane.setDividerLocation(400);

    }

    // *******************************************************
    // This section of MAI code contains the classes used in MAI

    /**
     * Creates a highlighter object that can be added to the text display
     *
     */
    private class TextHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {
        private TextHighlightPainter(Color color) {
            super(color);
        }
    }

    /**
     * Timer Task for timed messages in the status bar
     */
    private class TimedUpdateStatusBar extends TimerTask {
        @Override
        public void run() {
            updateStatusBar();
        }
    }

    /**
     * Handles the actions in the File menu; loading DTDs, starting new
     * adjudication tasks, adding files/gold standards to the adjudication task.
     *
     */
    private class getFile implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Load DTD")) {
                int returnVal = mLoadDTDFC.showOpenDialog(MaiMain.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = mLoadDTDFC.getSelectedFile();
                    try{
                        mTextPane.setStyledDocument(new DefaultStyledDocument());
                        DTDLoader dtdl = new DTDLoader(file);
                        mTask.reset_db();
                        mTask.setDTD(dtdl.getDTD());
                        makeRadioTags();
                        //reset visited locations
                        mVisited = new HashCollection<String,Integer>();

                        // add by krim: need to refresh interfaces
                        updateMenus();
                        returnToNormalMode();
                        mStatusBar.setText("DTD load succeed! Click anywhere to continue.");

                        isFileOpen =false;
                        mTask.addDTDtoDB();
                        resetBottomTable();
                    } catch(Exception o) {
                        o.printStackTrace();
                        // print out the error message on the status bar
                        mStatusBar.setText("Error loading DTD");
                    }
                }
            } else if (e.getActionCommand().equals("start adjud")) {
                int returnVal = mFileLoadFC.showOpenDialog(MaiMain.this);
                boolean succeed = true;
                String status = "";
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = mFileLoadFC.getSelectedFile();
                    String fileName = file.getName();

                    try{
                        // mod by krim: to show tool version on title bar
                        mMainFrame.setTitle(TITLE_PREFIX + " - " + fileName);

                        mFiles = new ArrayList<String>();
                        mFiles.add(fileName);
                        mFiles.add(GS_FILE);

                        isFileOpen = true;
                        mTask.reset_db();
                        mTask.addDTDtoDB();
                        mTask.reset_IDTracker();
                        mColorTable.clear();
                        mColorTable.put(GS_FILE, mYellow);
                        mColorTable.put("allOtherFiles", mCyan);
                        mColorTable.put("someOtherFiles",mPink);

                        // krim: refresh UI
                        updateMenus();
                        resetBottomTable();
                        returnToNormalMode();
                        mVisited = new HashCollection<String,Integer>();

                        mTextPane.setStyledDocument(new DefaultStyledDocument());
                        mTextPane.setContentType("text/plain; charset=UTF-8");
                        mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        if (FileOperations.areTags(file)) {
                            XMLFileLoader xfl = new XMLFileLoader(file);
                            StyledDocument d = mTextPane.getStyledDocument();
                            Style def = StyleContext.getDefaultStyleContext()
                                    .getStyle( StyleContext.DEFAULT_STYLE );
                            Style regular = d.addStyle( "regular", def );
                            d.insertString(0, xfl.getTextChars(), regular);
                            HashCollection<String,Hashtable<String,String>> newTags = xfl.getTagHash();
                            if (newTags.size()>0) {
                                mTask.addTagsFromHash(fileName, newTags);
                            }
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        succeed = false;
                        isFileOpen =false;
                        status = "Error loading file";
                    }
                    if (mTagButtons.getSelection()!=null) {
                        mCurTag = mTagButtons.getSelection().getActionCommand();
                        refreshTextColors();
                    }
                }
                mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                mTextPane.setCaretPosition(0);
                // add by krim: status bar refreshing come last
                // since any caret update event (like the aboves) updates status bar
                // thus we need to set text after caret events
                if (succeed) {
                    status = "File load succeed! Click anywhere to continue.";
                }
                mStatusBar.setText(status);

            } else if (e.getActionCommand().equals("add file")) {
                int returnVal = mFileLoadFC.showOpenDialog(MaiMain.this);
                boolean succeed = false;
                String status = "";
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = mFileLoadFC.getSelectedFile();

                    try {
                        String fileName = file.getName();

                        mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        //check to make sure the text is the same as the first file
                        int textLen = mTextPane.getStyledDocument().getLength();
                        String text= mTextPane.getStyledDocument().getText(0,textLen);
                        XMLFileLoader xfl = new XMLFileLoader(file);

                        String text2 = xfl.getTextChars();
                        int textLen2 = text2.length();

                        if (textLen != textLen2) {
                            status = "File length mismatch!";
                        } else if (!text.equals(text2)) {
                            status = "Text content mismatch!";
                        } else {
                            succeed = true;
                        }

                        if (succeed) {
                        //add the new file to the DB
                            if (FileOperations.areTags(file)) {
                                HashCollection<String,Hashtable<String,String>> newTags = xfl.getTagHash();
                                if (newTags.size()>0) {
                                    mTask.addTagsFromHash(fileName, newTags);
                                }
                            }


                            //check to make sure name isn't already there
                            while (mFiles.contains(fileName)) {
                                fileName = "x" + fileName;
                            }
                            mFiles.add(0, fileName);
                            assignColors(fileName);

                            mMainFrame.setTitle(mMainFrame.getTitle() + ", " + fileName);
                            //update the display
                            updateMenus();
                            resetBottomTable();
                            if (mTagButtons.getSelection() != null) {
                                mCurTag = mTagButtons.getSelection().getActionCommand();
                                refreshTextColors();
                            }
                            status = "File load succeed! Click anywhere to continue.";
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        status = "Error loading file";
                    }
                }
                mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                mTextPane.setCaretPosition(0);
                // add by krim: status bar refreshing come last
                // since any caret update event (like the aboves) updates status bar
                // thus we need to set text after caret events
                mStatusBar.setText(status);

            } else if (e.getActionCommand().equals("add GS")) {
                int returnVal = mFileLoadFC.showOpenDialog(MaiMain.this);
                boolean succeed = false;
                String status = "";
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = mFileLoadFC.getSelectedFile();

                    try{
                        String fileName = file.getName();

                        mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        //check to make sure the text is the same as the first file
                        int textLen = mTextPane.getStyledDocument().getLength();
                        String text= mTextPane.getStyledDocument().getText(0,textLen);
                        XMLFileLoader xfl = new XMLFileLoader(file);

                        String text2 = xfl.getTextChars();
                        int textLen2 = text2.length();

                        if (textLen != textLen2) {
                            status = "File length mismatch!";
                        } else if (!text.equals(text2)) {
                            status = "Text content mismatch!";
                        } else {
                            succeed = true;
                        }

                        if (succeed) {
                            // add new file as GS into DB
                            String tempHack = GS_FILE;
                            if (FileOperations.areTags(file)) {
                                HashCollection<String, Hashtable<String, String>> newTags = xfl.getTagHash();
                                if (newTags.size() > 0) {
                                    mTask.addTagsFromHash(tempHack, newTags);
                                }
                            }

                            mMainFrame.setTitle(mMainFrame.getTitle() + ", " + fileName);
                            //update the display
                            updateMenus();
                            resetBottomTable();
                            mTask.findAllOverlaps();
                            if (mTagButtons.getSelection() != null) {
                                mCurTag = mTagButtons.getSelection().getActionCommand();
                                refreshTextColors();
                            }
                            status = "File load succeed! Click anywhere to continue.";
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                        status = "Error loading file";
                    }
                }
                mMainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                mTextPane.setCaretPosition(0);
                // add by krim: status bar refreshing come last
                // since any caret update event (like the aboves) updates status bar
                // thus we need to set text after caret events
                mStatusBar.setText(status);
            }//end addGS


            else if(e.getActionCommand().equals("Save XML")) {
                mSaveFC.setSelectedFile(new File(GS_FILE));
                int returnVal = mSaveFC.showSaveDialog(MaiMain.this);
                if(returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = mSaveFC.getSelectedFile();
                    String fullName = file.getName();
                    try{
                        FileOperations.saveAdjudXML(file, mTextPane,
                                mTask);
                        mMainFrame.setTitle(fullName);
                        mStatusBar.setText("Save Complete!");
                    } catch(Exception e2) {
                        e2.printStackTrace();
                        mStatusBar.setText("Error saving XML file");
                    }
                }
            }
            // reset status bar after 3 secs
            new Timer().schedule(new TimedUpdateStatusBar(), 3000);
        }
    }
    
    /**
     * Listens for the request from the Help Menu
     */
    private class AboutListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            showAboutDialog();
        }
    }

    /**
     * Listens for the command to increase/decrease the size of the font
     */
    private class DisplayListener implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String action = e.getActionCommand();
            if (action.equals("Font++")) {
                Font font = mTextPane.getFont();
                Font font2 = new Font(font.getName(),font.getStyle(),font.getSize()+1);
                mTextPane.setFont(font2);
            }
            if (action.equals("Font--")) {
                Font font = mTextPane.getFont();
                Font font2 = new Font(font.getName(),font.getStyle(),font.getSize()-1);
                mTextPane.setFont(font2);
            }
        }
    }

    /**
     * Listens to the table to determine if a button has been clicked
     *
     */
    private class TableListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int curCol = mTagTable.getSelectedColumn();
            int curRow = mTagTable.getSelectedRow();

            //first, determine if we actually want to trigger an event
            if (!(mTableRow == curRow && mTableCol ==curCol)) {
                //if we do, figure out what event we're triggering
                if (mTagTable.getSelectedColumn() == mTagTable.getColumnCount()-1) {
                    ButtonEditor b =
                            (ButtonEditor) mTagTable.getCellEditor(curRow, curCol);
                    if (b.getLabel().startsWith("add")) {
                        checkForAddition(curCol, curRow);
                    } else if (b.getLabel().startsWith("copy")) {
                        //get data for new row and add directly to the GS
                        String[]newdata = makeRow(curCol, curRow);
                        DefaultTableModel tableModel
                                = (DefaultTableModel) mTagTable.getModel();
                        int i = tableModel.getRowCount();
                        tableModel.addRow(newdata);
                        checkForAddition(curCol,i);
                    }
                }
            }
            mTableCol = curCol;
            mTableRow = curRow;
        }
    }
    
    /**
     * This is the class that's called whenever a  
     * radio button is pressed.
     */
    private class RadioButtonListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            resetBottomTable();
            mCurTag = e.getActionCommand();
            refreshTextColors();
            Highlighter high = mTextPane.getHighlighter();
            high.removeAllHighlights();
            
        }
    }

    /**
     * If the user decides to remove the highlighted table rows, 
     * this method is called.  It removes the tags from the table 
     * as well as the database, and for extent tags it also 
     * removes any link tags the extent is participating in.
     *
     */
    private class RemoveSelectedTableRows implements ActionListener{
        public void actionPerformed(ActionEvent actionEvent) {

            boolean check = showDeleteWarning();
            if (check) {
                // TODO check for redundancy
//                mCurTag = mTagButtons.getSelection().getActionCommand();
                Elem elem = mTask.getElem(mCurTag);
                int[] selectedViewRows = mTagTable.getSelectedRows();
                
                //convert the rows of the table view into the rows of the 
                //table model so that the correct rows are deleted
                int[] selectedRows = new int[selectedViewRows.length];
                for (int i=0;i<selectedRows.length;i++) {
                    selectedRows[i]= mTagTable.convertRowIndexToModel(selectedViewRows[i]);
                }

                DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
                //find the id column
                int cols = tableModel.getColumnCount();
                int idCol = -1;
                int sourceCol = -1;
                for(int i=0;i<cols;i++) {
                    String colname = tableModel.getColumnName(i);
                    if(colname.equalsIgnoreCase("id")) {
                        idCol = i;
                    }
                    if(colname.equalsIgnoreCase("source")) {
                        sourceCol = i;
                    }
                }
                //get the id for each selected row and remove id
                String id;
                String source;
                for (int i=selectedRows.length-1;i>=0;i--) {
                    int row = selectedRows[i];
                    id = (String) tableModel.getValueAt(row,idCol);
                    source = (String) tableModel.getValueAt(row,sourceCol);
                    // don't want to delete tags that come from the files
                    // being adjudicated
                    if (source.equalsIgnoreCase(GS_FILE)) {
                        if(elem instanceof ElemExtent) {
                            mTask.removeExtentByFileAndID(source, mCurTag, id);

                            // mod by krim: instead of take 2 integers,
                            // take a string of possibly multiple mSpans
                            String spanString = (String)tableModel.getValueAt(row,2);
                            ArrayList<int[]> spans = parseSpansString(spanString);

                            assignTextColor(mCurTag, spans);
                            HashCollection<String,String> links
                                    =  mTask.getLinksByFileAndExtentID(source,mCurTag,id);
                            //remove links that use the tag being removed
                            ArrayList<String> linkTags = links.getKeyList();
                            for (String tag : linkTags) {
                                ArrayList<String> ids = links.get(tag);
                                for (String idl : ids) {
                                    mTask.removeLinkByFileAndID(source, tag, idl);
                                }
                                //also, remove those locations from the visited locations
                                //HashCollection
                                ArrayList<Integer> visited = mVisited.get(tag);
                                // mod by krim: support mSpans
                                for (int[] span : spans) {
                                    for (int j = span[0]; j <= span[1]; j++) {
                                        visited.remove(new Integer(j));
                                    }
                                }
                            }
                        }
                        else {
                            mTask.removeLinkByFileAndID(source, mCurTag, id);
                        }
                        tableModel.removeRow(selectedRows[i]);
                    }
                }
                if (elem instanceof ElemExtent) {
                    refreshTextColors();
                }
            }
        }
    }

    /**
     * This is the class that's called when an extent tag is
     * selected from the popup menu
     *
     */
    private class MakeTagListener implements ActionListener{
        public void actionPerformed(ActionEvent actionEvent) {
            clearTableSelections();
            String action = actionEvent.getActionCommand();
            //if the tag being added is non-consuming, make sure
            //start and end are set to -1
            if(action.contains("NC-")) {
                resetSpans();
                action = action.split("-")[1];
            }
            DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
            //clear out the rest of the table
            tableModel.getDataVector().removeAllElements();
            //create array for data for row*/
            String[] newdata = new String[tableModel.getColumnCount()];
            for(int i=0;i<tableModel.getColumnCount();i++) {
                newdata[i]="";
            }
            //get the Elem that the table was based on, and go through
            //     the attributes.  Put in the start and end bits
            Hashtable<String,Elem> elements = mTask.getElemHash();
            Elem elem = elements.get(action);
            //get ID number. This also isn't as hard-coded as it looks:
            //     the columns for the table are created from the Attributes array list
            String newID;
            ArrayList<Attrib> attributes = elem.getAttributes();
            for(int i=0;i<attributes.size();i++) {
                if(attributes.get(i) instanceof AttID) {
                    newID= mTask.getNextID(elem.getName(),GS_FILE);
                    newdata[i+1]=newID;
                }
                if(attributes.get(i).hasDefaultValue()) {
                    newdata[i+1]=attributes.get(i).getDefaultValue();
                }
            }

            //put in start and end values
            if (elem instanceof ElemExtent) {
//                attributes = elem.getAttributes();
                for(int k=0;k<tableModel.getColumnCount();k++) {
                    String colName = tableModel.getColumnName(k);
                    //this isn't as hard-coded as it looks, because
                    //all extent elements have these attributes

                    // mod by krim:
                    // try to put in spanString, not take out.

                    if (colName.equals("spans")) {
                        newdata[k] = spansToString(mSpans);
                    } else if (colName.equals("text") && !isSpansEmpty()) {
                        newdata[k] = getTextIn(mSpans);
                    } else if (colName.equals("source")) {
                        newdata[k] = GS_FILE;
                    }
                }
                newdata[tableModel.getColumnCount()-1] = "add/modify";

                tableModel.addRow(newdata);
                mTagTable.clearSelection();
                mTagTable.setRowSelectionInterval(
                        tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
                Rectangle rect =  mTagTable.getCellRect(
                        tableModel.getRowCount()-1, 0, true);
                mTagTable.scrollRectToVisible(rect);
                addRowToGoldStandard(0,tableModel.getRowCount()-1,elem);
                if(!isSpansEmpty()) {
                    assignTextColor(action, mSpans);
                }
                resetSpans();
            }
        }
    }

    /**
     * Keeps track of whether the CTRL key (or the equivalent Mac key)
     * is being pressed in order to determine if the link creation window 
     * should be displayed.
     */
    private class ModKeyListener implements KeyListener{
        public void keyPressed(KeyEvent e) {
            int keyCode = e.getKeyCode();

            String p = System.getProperty("os.name");
            if(p.toLowerCase().contains("mac")) {
                if (keyCode == 18 || keyCode == 157) {
                    isCtrlPressed = true;
                }
            } else {
                if ( keyCode == 17) {
                    isCtrlPressed = true;
                }
            }
        }

        public void keyReleased(KeyEvent e) {
            String p = System.getProperty("os.name");
            int keyCode = e.getKeyCode();
            if(p.toLowerCase().contains("mac")) {
                if (keyCode == 18 || keyCode == 157) {
                    isCtrlPressed = false;
                }
            } else {
                if ( keyCode == 17) {
                    isCtrlPressed = false;
                }
            }
        }

        public void keyTyped(KeyEvent e) {
            //do nothing
        }
    }

    /**
     * Keeps track of what extents in the text are highlighted by the user 
     * in order to refresh the tag tables, and assigns highlights based 
     * on the tag and extent selected.
     *
     * mod by krim: overall re-written for multi-span mode
     *
     */
    private class AnnCaretListener implements CaretListener{
        public void caretUpdate(CaretEvent e) {
            Highlighter hl = mTextPane.getHighlighter();
            //when the caret is moved, remove the any link highlights
            hl.removeAllHighlights();

            mTagTable.getColumnModel().getSelectionModel().removeListSelectionListener(mTableListener);
            mTagTable.getSelectionModel().removeListSelectionListener(mTableListener);
            mTableRow = -1;
            mTableCol = -1;
            resetBottomTable();

            int dot = e.getDot();
            int mark = e.getMark();

            if (isCtrlPressed) {
                if  (mPrevSpan == -1) {
                    mPrevSpan = dot;
                } else {
                    showLinkPopup(mPrevSpan, dot);
                    isCtrlPressed = false;
                    mPrevSpan = -1;
                }
            }

            // mod by krim.
            // Not just set start and end field to caret selection,
            // but clear the mSpans set first, then fill it with caret selection span.
            // Consequently the array get one int[] in it.

            // krim: if in normal mode, need to clear first, to remove default (-1, -1) pair
            if (isSpansEmpty()) {
                mSpans.clear();
            }

            int start, end;

            if (dot!=mark) {
                isTextSelected =true;
                if(dot<mark) {
                    start=dot;
                    end=mark;
                }
                else {
                    start=mark;
                    end=dot;
                }

                // krim: check if a span is already in mSpans list
                boolean dupSpan = false;
                int[] newSpan = new int[]{start, end};
                for (int[] span : mSpans) {
                    if (span[0] == newSpan[0] && span[1] == newSpan[1]) {
                        dupSpan = true;
                        break;
                    }
                }
                // add a new span to list only if it is really NEW one
                if (!dupSpan) {
                    mSpans.add(new int[]{start, end});
                }
            } else {

                // krim: in normal mode, reset mSpans to (-1, -1)
                if (mMode != MSPAN) {
                    isTextSelected = false;
                    resetSpans();
                }
            }

            // highlight corresponding row of table, in normal mode
            if (mMode != MSPAN) {
                findRelatedTags();
            }

            // add by krim: need to update current selection and status bar
            if (!isSpansEmpty()) {
                highlightTextSpans(hl, mSpans, mDefHL);
            }
            updateStatusBar();
        }
    }//end AnnCaretListener

    /**
     * This is the class that's associated with the make link button
     * in the popup window created in showLinkPopup()
     *
     */
    private class LinkListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            clearTableSelections();
            //check to make sure that linkFrom, linkName, and linkTo
            //are all valid ids/link names
            linkFrom = linkFrom.split(" \\(")[0];
            String from_id = linkFrom.split(" - ")[1];
            String from_type = linkFrom.split(" - ")[0];

            linkTo = linkTo.split(" \\(")[0];
            String to_id = linkTo.split(" - ")[1];
            String to_type = linkTo.split(" - ")[0];

            String from_text = mTask.getTextByFileElemAndID(GS_FILE,from_type,from_id);
            String to_text = mTask.getTextByFileElemAndID(GS_FILE,to_type,to_id);

            //add link to appropriate table
            DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();

            String[] newdata = new String[tableModel.getColumnCount()];
            for(int i=0;i<tableModel.getColumnCount();i++) {
                newdata[i]="";
            }
            //get the Elem that the table was based on, and go through
            //the attributes.  Put in the start and end bits
            Hashtable<String,Elem> elements = mTask.getElemHash();
            Elem elem = elements.get(linkName);

            //get ID number for link
            String newID = "";

            for(int k=0;k<tableModel.getColumnCount();k++) {
                String colName = tableModel.getColumnName(k);
                if(colName.equals("id")) {
                    newID= mTask.getNextID(elem.getName(),GS_FILE);
                    newdata[k]=newID;
                }
                else if(colName.equals("fromID")) {
                    newdata[k]=from_id;
                }
                else if(colName.equals("toID")) {
                    newdata[k]=to_id;
                }
                else if(colName.equals("fromText")) {
                    newdata[k]=from_text;
                }
                else if(colName.equals("toText")) {
                    newdata[k]=to_text;
                }
                else if (colName.equals("source")) {
                    newdata[k] = GS_FILE;
                }
            }
            newdata[tableModel.getColumnCount()-1] = "add/modify";
            tableModel.addRow(newdata);
            mTagTable.clearSelection();
            mTagTable.setRowSelectionInterval(tableModel.getRowCount() - 1, tableModel.getRowCount() - 1);
            Rectangle rect =  mTagTable.getCellRect(tableModel.getRowCount()-1, 0, true);
            mTagTable.scrollRectToVisible(rect);

            //add the new tag to the database
            addRowToGoldStandard(0,tableModel.getRowCount()-1,elem);

            //reset variables
            linkFrom="";
            linkName="";
            linkTo="";
            mLinkPopupFrame.setVisible(false);
        }

    }

    /**
     * A quick and dirty way to change global variables based
     * on what's going on in the link creation window so that
     * a new link can be created more easily.
     *
     */
    private class jboxListener implements ActionListener{
        public void actionPerformed(ActionEvent e) {
            JComboBox box = (JComboBox)e.getSource();
            String select = (String)box.getSelectedItem();
            if (e.getActionCommand() == "fromID") {
                linkFrom = select;
            }
            else if (e.getActionCommand() == "link") {
                linkName = select;
            }
            else if (e.getActionCommand() == "toID") {
                linkTo = select;
            }
        }

    }

    /**
     * Listens to the table to determine when a tag is double-clicked, and
     * calls the function to highlight the related extents.  Also determines
     * if the user right-clicked and should be given the option to remove
     * a tag from the database.
     *
     */
    private class TableMouseAdapter extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowRemovePopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowRemovePopup(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if(e.getClickCount()==2) {
                Highlighter hl = mTextPane.getHighlighter();
                hl.removeAllHighlights();
                mCurTag = mTagButtons.getSelection().getActionCommand();
                Elem el = mTask.getElem(mCurTag);

                if(el instanceof ElemExtent) {
                    int selectedRow = mTagTable.getSelectedRow();

                    // mod by krim: wrote a method to remove redundancy
                    ArrayList<int[]> spansSelect
                            = parseSpansString((String) mTagTable.getValueAt(selectedRow, 1));
                    highlightTextSpans(hl, spansSelect, mCyanHL);
                } //end if ElemExtent

                // krim: below block is used to highlight linked extents
                // "from" extent get yellow color,
                // "to" extent get green color,
                if(el instanceof ElemLink) {
                    //if a link is selected, the locations of the from and to anchors
                    //need to be found and highlighted
                    int selectedRow = mTagTable.getSelectedRow();
                    String fromSelect = (String) mTagTable.getValueAt(selectedRow,2);
                    String toSelect = (String) mTagTable.getValueAt(selectedRow,4);

                    ArrayList<int[]> fromSpans
                            = parseSpansString(mTask.getLocByFileAndID(GS_FILE, fromSelect));
                    ArrayList<int[]> toSpans
                            = parseSpansString(mTask.getLocByFileAndID(GS_FILE, toSelect));

                    if (toSpans != null) {
                        highlightTextSpans(hl, toSpans, mGreenHL);

                    }

                    // mod by krim: since highlightTextSpans() moves cursor,
                    // fromSpans need to be highlighted later
                    if (fromSpans != null) {
                        highlightTextSpans(hl, fromSpans, mCyanHL);
                    }
                }//end if ElemLink
            }
        }

        //if the user right-clicks on a link
        private void maybeShowRemovePopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                mTablePopup = tableContextMenu();
                mTablePopup.show(e.getComponent(),
                        e.getX(), e.getY());
            }
        }
    }

    /**
     * Listens for mouse events in the text area; if the
     * mouse situation meets popup requirements,
     * give the option to create a new tag.
     *
     */
    private class TextMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger() && isTextSelected) {
                mTextPopup = textContextMenu();
                mTextPopup.show(e.getComponent(),
                        e.getX(), e.getY());
            }
        }
    }

    /**
     * Change text selection mode to multiple span mode
     * add by krim
     */
    private class MultiSpanListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            mMode = MSPAN;
            mSpans.clear();
            updateMenus();
            mStatusBar.setText("Entering Multi-span mode! Click anywhere to continue.");
        }
    }

    /**
     * Change mode to normal
     * add by krim
     */
    private class ExitModeListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            returnToNormalMode();
        }
    }

    /**
     * Remove last selected text span from mSpans list
     * Used only in multi-span mode
     * add by krim
     */
    private class UndoSelectListener implements ActionListener {
        public void actionPerformed(ActionEvent actionEvent) {
            if (mSpans.size()>0) {
                int[] lastSpan = mSpans.remove(mSpans.size()-1);
                ArrayList<int[]> tmp = new ArrayList<int[]>();
                tmp.add(lastSpan);

                Highlighter high = mTextPane.getHighlighter();
                high.removeAllHighlights();
                highlightTextSpans(high, tmp, mGrayHL);
                highlightTextSpans(high, mSpans, mDefHL);

                mStatusBar.setText("Removed '" +
                        getTextBetween(lastSpan[0], lastSpan[1]) + "'!" +
                        " Click anywhere to continue.");
            } else {
                mStatusBar.setText("No text selected! Click anywhere to continue.");
            }

        }
    }

    //end of classes section
    //********************************************************

    /**
     * fetches the string from the text panel based on character offsets
     * mod by krim: take a string representing span(s), not 2 integers
     *
     * @param spans text mSpans
     * @return the text of the tag mSpans
     */
    private String getTextIn(ArrayList<int[]> spans) {
        String text = "";
        for (int[] span : spans) {
            text += getTextBetween(span[0], span[1]) + SPANTEXTTRUNC;
        }
        // fence posting
        return text.substring(0,text.length()-SPANTEXTTRUNC.length());
    }

    /**
     * fetches the string from the text panel based on character offsets
     * 
     * @param start the start of the span
     * @param end the end of the span
     * @return the text at the specified location
     */
    private String getTextBetween(int start, int end) {
        DefaultStyledDocument styleDoc = (DefaultStyledDocument) mTextPane.getStyledDocument();
        String text;
        try{
            text = styleDoc.getText(start,end-start);
        } catch(Exception e) {
            e.printStackTrace();
            text = "Error getting text from a selected span";
        }
        return text;
    }

    /**
     * Updates the colors in the text area with current tag chosen from the radio buttons
     * 
     * When a tag is selected, the colors are based on which/how many 
     * files include it in the annotation (if it's in the Gold Standard, the 
     * text is green, if it's in all the files but the Gold Standard the text 
     * is blue, and if it's some but not all the files the text is red).    
     * 
     */
    private void refreshTextColors() {
        //reset all text to black, then set new mColors
        setColorAtLocation(mBlack,0, mTextPane.getStyledDocument().getLength(),false);

        //check to see if the tagname is a non-consuming tag
        if (mCurTag.startsWith("NC-")) {
            // no colors will be set if an non-consuming tag is chosen; instead
            // skip straight to filling in the table
            String command = mCurTag.substring(3);
            HashCollection<String,String> idHash = mTask.getTagsSpanByType(
                    -1, -1,command);
            fillInTable(idHash,command);
        } else {
            Elem e = mTask.getElem(mCurTag);
            HashCollection<String,String> elems;
            if (e instanceof ElemExtent) {
                // if selected tag is a extent tag, ,
                // retrieve all extent locations from the DB
                elems = mTask.getExtentAllLocs(mCurTag);
            } else {
                // if selected tag is a link
                // first, get all the places where there are extent tags in the GS
                Hashtable<String, String> allLocs = mTask.getAllExtentsByFile(GS_FILE);
                for (Enumeration<String> locs = allLocs.keys(); locs.hasMoreElements(); ) {
                    int loc = Integer.parseInt(locs.nextElement());
                    // mod by krim: for consistency with colorVisitedLocs(),
                    // color text with purple (originally light gray)
                    setColorAtLocation(mPurple, loc, 1, false);
                }
                // then, figure out what extents are already in links and
                // highlight them appropriately
                elems = mTask.findGoldStandardLinksByType(mCurTag);
            }
            ArrayList<String> locations = elems.getKeyList();
            for (String location : locations) {
                int loc = Integer.parseInt(location);
                assignTextColor(mCurTag, loc, loc + 1);

            }
            // factored out coloring statements into colorVisited()
            // TODO need thorough test
            if (!(e instanceof ElemExtent)) {
                //for link tag selected, go over everything that's already been looked at
                colorVisitedLocs(mCurTag);
            }
        }
    }

    /**
     * Goes over the text when a link is selected and colors the locations
     * that the adjudicator has already examined.
     * 
     * @param tagName that tag that's been selected
     */
    private void colorVisitedLocs(String tagName) {
        ArrayList<Integer> visitLocs = mVisited.get(tagName);
        if (visitLocs !=null) {
            for (Integer visitLoc : visitLocs) {
                setColorAtLocation(mPurple, visitLoc, 1, false);
            }
        }
    }

    /**
     *  This method is for coloring/underlining text
     *  in the text window.  It detects overlaps, and
     *  should be called every time a tag is added
     *  or removed.
     *
     *  mod by krim to support multiple mSpans
     *
     * @param tagName the selected tagname
     * @param spans a sorted set of all mSpans
     */
    private void assignTextColor(String tagName, ArrayList<int[]> spans) {
        // go through each part of the word being changed and
        // find what tags are there, and what color it should be.
        Elem e = mTask.getElem(tagName);
        if (e instanceof ElemExtent) {
            for (int[] span : spans) {
                int start = span[0], end = span[1];
                assignTextColor(tagName, start, end);
            }
        }
    }

    /**
     *  This method is for coloring/underlining text
     *  in the text window.  It detects overlaps, and
     *  should be called every time a tag is added
     *  or removed.
     *
     *  mod by krim: re-written to remove redundancy
     *
     *
     * @param tagName the selected tagname
     * @param start the beginning of the text span being assigned a color
     * @param end the end of the text span being assigned a color
     */
    private void assignTextColor(String tagName, int start, int end) {
        // go through each part of the word being changed and 
        // find what tags are there, and what color it should be.
        for(int i=0;i<end-start;i++) {
            int cur = start + i;
            ArrayList<String>files =
                    mTask.getFilesAtLocbyElement(tagName, cur);
            //if there is a gold standard tag in that location, the text will be gree
            if (files.contains(GS_FILE)) {
                setColorAtLocation(mGreen, cur, 1, false);
            } else if (files.size()>0) {
                boolean allfiles = true;
                // check to see if there's a tag at that location from each
                // of the files being adjudicated
                for (String file : mFiles) {
                    if (!files.contains(file) && !file.equals(GS_FILE)) {
                        allfiles = false;
                    }
                }
                if (allfiles) {
                    //if a tag exists in all files, the text is blue
                    setColorAtLocation(mBlue, cur, 1, false);
                } else {
                    //otherwise it's red
                    setColorAtLocation(mRed, cur, 1, false);
                }
            } else {
                setColorAtLocation(mBlack, cur, 1, false);
            }
        }
    }

    /**
     * Used to set the text in a span to a determined color
     * krim: when should texts be underlined?
     *
     * @param color the color being assigned
     * @param pos the start of the span
     * @param len the end of the span
     * @param underLined whether or not the text will be underlined
     */
    private void setColorAtLocation(Color color, int pos, int len, boolean underLined) {
        DefaultStyledDocument styleDoc
                = (DefaultStyledDocument) mTextPane.getStyledDocument();
        SimpleAttributeSet aset = new SimpleAttributeSet();
        StyleConstants.setForeground(aset, color);
        StyleConstants.setUnderline(aset, underLined);
        styleDoc.setCharacterAttributes(pos,len,aset,false);
    }

    /**
     * Creates the popup menu that allows users to create new 
     * tags from the text window
     * @return popup menu
     */
    private JPopupMenu textContextMenu() {
        JPopupMenu jp = new JPopupMenu();
        //create a menu item for the selected RadioButton
        try {
            mCurTag = mTagButtons.getSelection().getActionCommand();
            JMenuItem menuItem = new JMenuItem(mCurTag);
            menuItem.addActionListener(new MakeTagListener());
            jp.add(menuItem);
        } catch (NullPointerException e) {
            String name = "No tag selected";
            JMenuItem menuItem = new JMenuItem(name);
            menuItem.setEnabled(false);
            jp.add(menuItem);
        }

        switch (mMode) {
            case MSPAN:
                jp.addSeparator();
                JMenuItem undo = new JMenuItem("Delete the last selected");
                undo.setActionCommand("Delete");
                undo.addActionListener(new UndoSelectListener());
                JMenuItem exit = new JMenuItem("Exit Multi-span Mode");
                exit.setActionCommand("Exit");
                exit.addActionListener(new ExitModeListener());

                jp.add(undo);
                jp.add(exit);
                break;
            case MLINK:
                // TODO if in link mode, retrieve existing tags and populate with (from, to etc) items
                break;

            case NORMAL:
                // Unlike MAE,
                // MAI don't support removing extents using a context menu at text panel
                // maybe later
                break;
        }
        return jp;
    }

    /**
     * The pop-up that appears when a tag table is right-clicked to give the
     * user the option to delete tags from the gold standard
     *
     * @return popup menu
     *
     * TODO add menu item to add link or so
     *
     */
    private JPopupMenu tableContextMenu() {
        JPopupMenu jp = new JPopupMenu();
        mCurTag = mTagButtons.getSelection().getActionCommand();
        if (mCurTag.startsWith("NC-")) {
            mCurTag = mCurTag.substring(3);
        }
        String action = "Remove selected " + mCurTag + " rows";
        JMenuItem menuItem = new JMenuItem(action);
        menuItem.setActionCommand(mCurTag);
        menuItem.addActionListener(new RemoveSelectedTableRows());
        jp.add(menuItem);
        return jp;
    }

    /**
     * Shows the warning when deleting a tag.  This will be 
     * displayed even if the extent does not participate in any links.
     * @return true or false
     */
    private boolean showDeleteWarning() {
        String text = ("Deleting extent tag(s) will also delete \n" +
                "any links that use these extents.  Would you like to continue?");

        int message = JOptionPane.showConfirmDialog(mMainFrame,
                text, "Warning!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return message == 0;
    }

    /**
     * Fills in the table based on the extent selected in the text window
     */
    private void findRelatedTags() {
        //first, get files and ids of elements in selected extents by type\
        if (mTagButtons.getSelection()!=null) {
            mCurTag = mTagButtons.getSelection().getActionCommand();
            Elem e = mTask.getElem(mCurTag);
            if (e instanceof ElemExtent) {
                HashCollection<String,String> idHash
                        = mTask.getTagsWithinSpansByType(mSpans, mCurTag);
                fillInTable(idHash,mCurTag);
            } else if (e instanceof ElemLink) {
                // for location between start and end, if there is an extent tag
                // in the gold standard, change color to magenta and add
                // to visited locations
                // mod by krim: support for mSpans
                for (int[] span : mSpans) {
                    int start = span[0], end = span[1];
                    for(int i = start;i<=end;i++) {
                        if (mTask.tagExistsInFileAtLoc(GS_FILE,i)) {
                            mVisited.putEnt(mCurTag, i);
                            colorVisitedLocs(e.getName());
                        }
                    }
                }
                HashCollection<String,Hashtable<String,String>> idHash =
                        mTask.getLinkTagsSpanByType(mSpans,mCurTag);
                fillInLinkTable(idHash,mCurTag);
            } else {
                //do nothing, it's a non-consuming tag
                System.out.println("NC tag is selected");
            }
        } else {
            // no tag is selected from the left panel
            System.out.println("No tag selected");
        }
    }

    /**
     * Removes the bottom table and creates a new one based on the 
     * selected tag.
     */
    private void resetBottomTable() {
        mBottomPanel.removeAll();
        JComponent tableComp = makeTable();
        mBottomPanel.add(tableComp);
        mBottomPanel.updateUI();
    }

    /**
     * Builds and displays the link creation window by getting the tags 
     * at the two locations the user clicked while holding the CTRL key
     * 
     * @param loc the first location clicked
     * @param loc2 the second location clicked
     */
    private void showLinkPopup(int loc, int loc2) {
        JPanel linkPane = new JPanel(new BorderLayout());
        JPanel boxPane = new JPanel(new GridLayout(3,2));
        mLinkPopupFrame = new JFrame();

        //first, get all the tags in the gold standard at the first location
        //non-consuming tags are included, because those can also be linked to/from
        JComboBox fromList = new JComboBox();
        fromList.addActionListener(new jboxListener());
        fromList.setActionCommand("fromID"); 

        HashCollection<String,String> idHash =  
                mTask.getFileTagsSpanAndNC(GS_FILE,loc,loc+1);
        ArrayList<String> elements = idHash.getKeyList();
        if (elements.size()>0) {
            if (elements.size()>1) {
                fromList.addItem("");
            }
            for(int i=0; i<elements.size();i++) {
                ArrayList<String> tags = idHash.get(elements.get(i));
                for(int j=0;j<tags.size();j++) {
                    //create the string for the table list
                    String puttag = (elements.get(i) + 
                            " - " + tags.get(j));
                    //get the text for the words by id and element
                    String text = mTask.getTextByFileElemAndID(GS_FILE,elements.get(i),tags.get(j));
                    puttag = puttag + " ("+text+")";
                    //add string to JComboBox
                    fromList.addItem(puttag);
                }
            }
        }

        //the, create the combobox that contains the link types 
        //(in MAI, this is the tag that's been selected, not all the link tags)
        JComboBox linkList = new JComboBox();
        linkList.setActionCommand("link"); 
        linkList.addActionListener(new jboxListener());
        linkList.addItem(mTagButtons.getSelection().getActionCommand());

        //then, fill in the tag and NC information for the second location
        JComboBox toList = new JComboBox();
        toList.setActionCommand("toID"); 
        toList.addActionListener(new jboxListener());

        idHash =  mTask.getFileTagsSpanAndNC(GS_FILE,loc2,loc2+1);
        elements = idHash.getKeyList();
        if (elements.size()>0) {
            if (elements.size()>1) {
                toList.addItem("");
            }
            for(int i=0; i<elements.size();i++) {
                ArrayList<String> tags = idHash.get(elements.get(i));
                for(int j=0;j<tags.size();j++) {
                    String puttag = (elements.get(i) + 
                            " - " + tags.get(j));
                    //get the text for the words by id and element
                    String text = mTask.getTextByFileElemAndID(GS_FILE,elements.get(i),tags.get(j));
                    puttag = puttag + " ("+text+")";
                    toList.addItem(puttag);
                }
            }
        }
        //pack everything into the window and make it visible
        JButton makeLink = new JButton("Create Link");
        makeLink.addActionListener(new LinkListener());
        boxPane.add(new JLabel("Link from:"));
        boxPane.add(fromList);
        boxPane.add(new JLabel("Link type:"));
        boxPane.add(linkList);
        boxPane.add(new JLabel("Link to:"));
        boxPane.add(toList);
        linkPane.add(boxPane,BorderLayout.CENTER);
        linkPane.add(makeLink,BorderLayout.SOUTH);
        mLinkPopupFrame.setBounds(90, 70, 400, 300);
        mLinkPopupFrame.add(linkPane);
        mLinkPopupFrame.setVisible(true);

    }

    /**
     * Creates the table and table model for the data that will be displayed 
     * based on the selected RadioButton.
     */
    private JComponent makeTable() {
        AnnTableModel model = new AnnTableModel();
        model.setGoldStandardName(GS_FILE);
        mTagTable = new AnnJTable(model);
        JScrollPane scroll = new JScrollPane(mTagTable);
        mTagTable.addMouseListener(new TableMouseAdapter());
        //check to make sure that a tag has been selected
        if (mCurTag != null) {
            if(mCurTag.startsWith("NC-")) {
                mCurTag = mCurTag.substring(3);
            }
            Elem e = mTask.getElem(mCurTag);
            ArrayList<Attrib> attributes = e.getAttributes();
            //for some reason, it's necessary to add the columns first,
            model.addColumn("source");
            for (Attrib attribute : attributes) {
                model.addColumn(attribute.getName());
            }
            model.addColumn("action");

            //then go back and add the cell renderers.
            for (int i=0;i<attributes.size();i++) {
                Attrib a = attributes.get(i);
                TableColumn c = mTagTable.getColumnModel().getColumn(i+1);
                if (a instanceof AttList) {
                    AttList att = (AttList)a;
                    JComboBox options = makeComboBox(att);
                    c.setCellEditor(new DefaultCellEditor(options));
                }
            }

            //add buttons to end of rows
            TableColumn c
                    = mTagTable.getColumnModel().getColumn(mTagTable.getColumnCount()-1);
            c.setCellRenderer(new ButtonRenderer());
            c.setCellEditor(new ButtonEditor(new JCheckBox()));
        }
        //need to add the same listener to both, 
        //otherwise the table events won't trigger correctly
        mTagTable.getSelectionModel().addListSelectionListener(mTableListener);
        mTagTable.getColumnModel().getSelectionModel().
                addListSelectionListener(mTableListener);

        return(scroll);
    }

    /**
     * Link tables are more complicated to fill in because they require that the 
     * links from the files being adjudicated contain the IDs and text of the overlapping 
     * tags from the goldStandard
     * mod by krim: multi span support
     *
     * @param idHash the hashtable containing IDs
     * @param tagname the name of the tag whose information is being filled in
     */
    private void fillInLinkTable(
            HashCollection<String,Hashtable<String,String>> idHash, String tagname) {
        //first, clear out existing table and listener, otherwise the changes to the table
        //trigger conflicting events
        mTagTable.getColumnModel().getSelectionModel().removeListSelectionListener(mTableListener);
        mTagTable.getSelectionModel().removeListSelectionListener(mTableListener);
        mTableRow = -1;
        mTableCol = -1;
        DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
        tableModel.getDataVector().removeAllElements();

        // idHash is a HashCollection containing
        // the filenames as keys and HashTables with attribute info as data
        String[] newdata = new String[tableModel.getColumnCount()];
        ArrayList<String> keys = idHash.getKeyList();
        //for each file source, add all tags in the idHash
        for (String source : keys) {
            ArrayList<Hashtable<String, String>> links = idHash.getList(source);
            if (links != null) {
                for (Hashtable<String, String> ht : links) {
                    if (ht == null) {
                        mStatusBar.setText(
                                "Failed to get tags from DB. " +
                                "Maybe using a wrong DTD definition?");
                        return;
//                        break;
                    }
                    newdata[0] = source;
                    for (int k = 1; k < tableModel.getColumnCount(); k++) {
                        String colName = tableModel.getColumnName(k);
                        String value = ht.get(colName);
                        if (value != null) {
                            newdata[k] = value;
                        } else {
                            newdata[k] = "";
                        }
                    }
                    //create the appropriate buttons
                    if (source.equals(GS_FILE)) {
                        newdata[tableModel.getColumnCount() - 1] = "add/modify";
                    } else {
                        newdata[tableModel.getColumnCount() - 1] = "copy to GS";
                    }
                    tableModel.addRow(newdata);
                }
            }
        }
        //also, highlight the appropriate related extents
        HashCollection<String,int[]> curHL = mTask.getCurrentHighlights();

        //keep the gold standard highlights separate for dealing with afterwards
        ArrayList<int[]> gsHLSpans = curHL.remove(GS_FILE);
        Highlighter hl = mTextPane.getHighlighter();
        hl.removeAllHighlights();
        if (gsHLSpans!=null) {
            highlightTextSpans(hl, gsHLSpans,
                    new TextHighlightPainter(mColorTable.get(GS_FILE)));
        }
        ArrayList<String> files = curHL.getKeyList();
        for (String file : files) {
            ArrayList<int[]> fileHLSpans = curHL.get(file);
            highlightTextSpans(hl, fileHLSpans,
                    new TextHighlightPainter(mColorTable.get(file)));
        }
        //add the listeners back to the table
        mTagTable.getSelectionModel().addListSelectionListener(mTableListener);
        mTagTable.getColumnModel().getSelectionModel().addListSelectionListener(mTableListener);
    }

    /**
     * Fills in the table when an extent tag is selected from the 
     * RadioButtons and a new span is highlighted in the text area.
     * 
     * @param idHash a HashCollection containing relevent IDs
     * @param tagName the type of the tag information being filled in
     */
    private void fillInTable(HashCollection<String,String> idHash, String tagName) {
        // first, clear out existing table and listener,
        // otherwise the changes to the table trigger conflicting events
        mTagTable.getColumnModel().getSelectionModel().removeListSelectionListener(mTableListener);
        mTagTable.getSelectionModel().removeListSelectionListener(mTableListener);
        mTableRow = -1;
        mTableCol = -1;
        DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
        tableModel.getDataVector().removeAllElements();

        // idHash is a HashCollection containing
        // the filenames as keys and tag ids as data
        String[] newdata = new String[tableModel.getColumnCount()];
        ArrayList<String> keys = idHash.getKeyList();
        //for each file source, add all tags in the idHash
        for (String source : keys) {
            ArrayList<String> ids = idHash.getList(source);
            if (ids != null) {
                for (String id : ids) {
                    Hashtable<String, String> ht
                            = mTask.getTagsByFileAndID(tagName, id, source);
                    if (ht == null) {
                        mStatusBar.setText(
                                "Failed to get tags from DB. " +
                                "Maybe using a wrong DTD definition?");
                        break;
                    }
                    newdata[0] = source;
                    for (int k = 1; k < tableModel.getColumnCount(); k++) {
                        String colName = tableModel.getColumnName(k);
                        String value = ht.get(colName);
                        if (value != null) {
                            newdata[k] = value;
                        } else {
                            newdata[k] = "";
                        }
                    }
                    if (source.equals(GS_FILE)) {
                        newdata[tableModel.getColumnCount() - 1] = "add/modify";
                    } else {
                        newdata[tableModel.getColumnCount() - 1] = "copy to GS";
                    }
                    tableModel.addRow(newdata);
                }
            }
        }
        mTagTable.getSelectionModel().addListSelectionListener(mTableListener);
        mTagTable.getColumnModel().getSelectionModel().addListSelectionListener(mTableListener);
    }

    /**
     * If a new file is being added to the gold standard, this method
     * checks to make sure all the necessary information is there.
     * 
     * @param col the column where the "add" button was pressed (will be 0 
     * if this is being called from somewhere outside the tag display table)
     * @param buttonRow the row being checked for inclusion to the gold standard
     */
    private void checkForAddition(int col, int buttonRow) {
        //get array of tag attributes
        mCurTag = mTagButtons.getSelection().getActionCommand();
        //if we're dealing with an NC tag, the checks are the same as a regular tag
        //but the triggering command is different and needs to be fixed.
        if(mCurTag.startsWith("NC-")) {
            mCurTag = mCurTag.substring(3);
        }
        Elem e = mTask.getElem(mCurTag);
        if(e instanceof ElemExtent) {
            //check for start
//            boolean hasStart = false;
//            boolean hasEnd = false;
            boolean hasSpans = false;
            // mod by krim: now we have mSpans, so only check for mSpans
            for (int i=0;i<mTagTable.getColumnCount();i++) {
                String header = mTagTable.getColumnName(i);
                if (header.equals("spans")) {
                    String val = (String) mTagTable.getValueAt(buttonRow, i);
                    if (!val.equals("")) {
                        hasSpans = true;
                    }
                }
            }

//            if (hasStart && hasEnd) {
            if (hasSpans) {
                addRowToGoldStandard(col,buttonRow,e);
            }
            else {
                JOptionPane.showMessageDialog(mTagTable,"No span associated");
            }
        }
        else {//if it's a link
            boolean hasFromID = false;
            boolean hasToID = false;

            for (int i=0;i< mTagTable.getColumnCount();i++) {
                String header = mTagTable.getColumnName(i);
                if (header.equals("fromID")) {
                    String val = (String) mTagTable.getValueAt(buttonRow,i);
                    if(val!="") {
                        hasFromID=true;
                    }
                }
                else if (header.equals("toID")) {
                    String val = (String) mTagTable.getValueAt(buttonRow,i);
                    if(val!="") {
                        hasToID=true;
                    }
                }
            }
            if(hasFromID && hasToID) {
                addRowToGoldStandard(col,buttonRow,e);
            }else {
                JOptionPane.showMessageDialog(mTagTable,"parameters missing");
            }
        }
    }
    
    /**
     * Adds a new row to the gold standard, or resubmits an existing row 
     * if attributes have been changed.
     * 
     * @param col the column of the button that triggered this method being called
     * (0 if the table was not used)
     * @param buttonRow the row being added/modified
     * @param e the type of tag being added
     */
    private void addRowToGoldStandard(int col, int buttonRow, Elem e) {
        boolean hasID = false;
        String id = "";
        int idLoc = -1;

        for (int i=0;i< mTagTable.getColumnCount();i++) {
            String header = mTagTable.getColumnName(i);
            if (header.equals("id")) {
                idLoc = i;
                id = (String) mTagTable.getValueAt(buttonRow,i);
                if (!id.equals("")) {
                    hasID = true;
                }
            }
        }
        if (hasID) {
            //remove previous tag from DB
            //this might not be necessary if the start and end aren't being
            //changed
            if (e instanceof ElemExtent) {
                mTask.removeExtentByFileAndID(GS_FILE, e.getName(), id);
            }
            else {
                mTask.removeLinkByFileAndID(GS_FILE, e.getName(), id);
            }
        } else {
            //if no id exists in the row, get the next one
            id = mTask.getNextID(e.getName(),GS_FILE);
            mTagTable.setValueAt(id, buttonRow, idLoc);
        }
        //create hashtable of element attributes
        Hashtable<String,String> tag = new Hashtable<String,String>();

        if (e instanceof ElemExtent) {

            for (int i=0;i< mTagTable.getColumnCount();i++) {
                String header = mTagTable.getColumnName(i);
                tag.put(header,(String) mTagTable.getValueAt(buttonRow,i));

                // mod by krim: support for mSpans
                if (header.equals("spans")) {
                    mSpans = parseSpansString((String) mTagTable.getValueAt(buttonRow, i));
                }
            }
            //add the column to the DB
            mTask.addTagFromHash(GS_FILE, e, tag);
            //color the new location appropriately
            assignTextColor(e.getName(), mSpans);
            resetSpans();
        }
        else {//if it's a link
            for (int i=0;i< mTagTable.getColumnCount();i++) {
                String header = mTagTable.getColumnName(i);
                tag.put(header,(String) mTagTable.getValueAt(buttonRow,i));
            }
            //add the column to the DB
            mTask.addTagFromHash(GS_FILE, e, tag);
            mCurTag = mTagButtons.getSelection().getActionCommand();
            refreshTextColors();
        }
    }

    /**
     * remove all highlights from rows
     */
    private void clearTableSelections() {
        DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
        int rows = tableModel.getRowCount();
        if(rows>0)
            mTagTable.removeRowSelectionInterval(0,rows-1);
    }

    /**
     * Creates a new row for the Gold Standard, usually from a button click from the table.
     * 
     * @param col the column where the clicked button exists
     * @param row the row being copied to the Gold Standard
     * @return an array of the correct size with the some of the information filled in
     */
    private String[] makeRow(int col, int row) {
        DefaultTableModel tableModel = (DefaultTableModel) mTagTable.getModel();
        String[] newdata = new String[tableModel.getColumnCount()];
        for (int i=0;i<tableModel.getColumnCount()-1;i++) {
            if(i==0) {
                newdata[i]=GS_FILE;
            }
            else if (mTagTable.getColumnName(i).equals("id")) {
                newdata[i]="";
            }
            else {
                newdata[i] = (String) mTagTable.getValueAt(row,i);
            }
        }
        newdata[tableModel.getColumnCount()-1]="add/modify";
        return newdata;
    }
    
    /**
     * Provides information about MAI
     * TODO add credit
     */
    private void showAboutDialog() {
        JOptionPane about = new JOptionPane();
        about.setLocation(100,100);
        about.setAlignmentX(Component.CENTER_ALIGNMENT);
        about.setAlignmentY(Component.CENTER_ALIGNMENT);
        about.setMessage("MAI \nMulti-document Adjudication Interface \n" +
                "Version " + VERSION + "\n\n" +
                "Copyright Amber Stubbs\nastubbs@cs.brandeis.edu \n Lab for " +
                "Linguistics and Computation, Brandeis University 2010-2012." + 
                "\n\nThis distribution of MAI (the software and the source code) \n" +
                " is covered under the GNU General Public License version 3.\n" +
                "http://www.gnu.org/licenses/");
        JDialog dialog = about.createDialog(mMainFrame, "About MAI");
        dialog.setVisible(true);
    }

    /**
     * Makes a comboBox from List-type attribute
     * @param att The Attlist being turned into a combobox
     * @return jcombobox
     */
    private JComboBox makeComboBox(AttList att) {
        JComboBox options = new JComboBox();
        options.addItem("");
        for(int j=0;j<att.getList().size();j++) {
            options.addItem(att.getList().get(j));
        }
        return options;
    }

    /**
     * Creates the radiobutton options on the left side of the display.
     */
    private void makeRadioTags() {
        mLeftPanel.removeAll();
        ArrayList<Elem> elements = mTask.getElements();
        mTagButtons = new ButtonGroup();
        //first, add the regular tags
        for (int i = 0;i<elements.size();i++) {
            Elem e = elements.get(i);
            JRadioButton button = new JRadioButton(e.getName());
            button.setActionCommand(e.getName());
            button.addActionListener(new RadioButtonListener());
            mTagButtons.add(button);
            mLeftPanel.add(button);
        }
        //then, add the NC elements
        ArrayList<Elem> ncElements = mTask.getNCElements();
        for (int i = 0;i<ncElements.size();i++) {
            Elem e = ncElements.get(i);
            JRadioButton button = new JRadioButton("NC-"+e.getName());
            button.setActionCommand("NC-"+e.getName());
            button.addActionListener(new RadioButtonListener());
            mTagButtons.add(button);
            mLeftPanel.add(button);
        }
    }

    /**
     * Assigns colors to each file being adjudicated.
     *
     * @param fileName - name of a new file added for adjudication
     */
    private void assignColors(String fileName) {
        //assigns a color from mColor array as a file is added for adjudication
        int col = mColorTable.size();
        int cols = mColors.length;
        if (col>=cols) {
            col = col%cols;
        }
        mColorTable.put(fileName, mColors[col]);
    }

    /**
     * Create the file menu with associated listeners and 
     * commands.
     */
    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        JMenuItem loadDTD = new JMenuItem("Load DTD");
        loadDTD.setActionCommand("Load DTD");
        loadDTD.addActionListener(new getFile());
        menu.add(loadDTD);

        JMenuItem startAdjud = new JMenuItem("Start new adjudication");
        startAdjud.setActionCommand("start adjud");
        startAdjud.addActionListener(new getFile());
        if(mTask.hasDTD()==false) {
            startAdjud.setEnabled(false);
        }
        else {
            startAdjud.setEnabled(true);
        }
        menu.add(startAdjud);

        JMenuItem addFile = new JMenuItem("Add file to adjudication");
        addFile.setActionCommand("add file");
        addFile.addActionListener(new getFile());
        if(isFileOpen ==false) {
            addFile.setEnabled(false);
        }
        else {
            addFile.setEnabled(true);
        }

        menu.add(addFile);

        JMenuItem addGS = new JMenuItem("Add gold standard file");
        addGS.setActionCommand("add GS");
        addGS.addActionListener(new getFile());
        if(isFileOpen ==false) {
            addGS.setEnabled(false);
        }
        else {
            addGS.setEnabled(true);
        }
        menu.add(addGS);

        menu.addSeparator();
        JMenuItem saveFileXML = new JMenuItem("Save Gold Standard As XML");
        saveFileXML.setActionCommand("Save XML");
        saveFileXML.addActionListener(new getFile());
        if(isFileOpen ==false) {
            saveFileXML.setEnabled(false);
        }
        else {
            saveFileXML.setEnabled(true);
        }

        menu.add(saveFileXML);
        return menu;
    }

    /**
     * Creates the menu for changing the font size
     */
    private JMenu createDisplayMenu() {
        JMenu menu = new JMenu("Display");

        JMenuItem increaseFont = new JMenuItem("Font Size ++");
        increaseFont.setActionCommand("Font++");
        increaseFont.addActionListener(new DisplayListener());
        menu.add(increaseFont);

        JMenuItem decreaseFont = new JMenuItem("Font Size --");
        decreaseFont.setActionCommand("Font--");
        decreaseFont.addActionListener(new DisplayListener());
        menu.add(decreaseFont);

        return menu;

    }

    /**
     * Creates the menu describing MAI
     */
    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Help");
        JMenuItem about = new JMenuItem("About MAI");
        about.addActionListener(new AboutListener());
        menu.add(about);
        return menu;
    }

    /**
     * Creates the menu for special input modes
     * add by krim
     *
     * @return JMenu Mode for the menu bar
     */
    private JMenu createModeMenu() {
        JMenu menu = new JMenu("Mode");
        JMenuItem multiSpan = new JMenuItem("Multi-span Mode");
        if(mMode != NORMAL) {
            multiSpan.setEnabled(false);
        } else {
            multiSpan.setEnabled(true);
        }
        multiSpan.addActionListener(new MultiSpanListener());

        JMenuItem multiLink = new JMenuItem("Multi-link Mode");
        multiLink.setEnabled(false);
        JMenuItem mlInfo = new JMenuItem("Multi-link menu currently not working.");
        mlInfo.setEnabled(false);

        JMenuItem exitMode = new JMenuItem("Exit to Normal Mode");
        if(mMode != NORMAL) {
            exitMode.setEnabled(true);
        } else {
            exitMode.setEnabled(false);
        }
        exitMode.addActionListener(new ExitModeListener());


        menu.add(multiSpan);
        menu.addSeparator();
        menu.add(multiLink);
        menu.add(mlInfo);
        menu.addSeparator();
        menu.add(exitMode);

        return menu;
    }

    /**
     * Updates the menus when new files are added
     */
    private void updateMenus() {
        mMenuBar.removeAll();

        mFileMenu = createFileMenu();
        mMenuBar.add(mFileMenu);

        // krim: note that we don't have to re-create display and help menus
        if (isFileOpen) {
            mMenuBar.add(mDisplayMenu);

            // add by krim: special mode menu
            mModeMenu = createModeMenu();
            mMenuBar.add(mModeMenu);
        }

        mMenuBar.add(mHelpMenu);

        mMenuBar.updateUI();

    }

    /**
     * Takes a string representing possibly multiple mSpans of an extent tag
     * Return array of integer pairs
     *
     * @param spanString - string of mSpans
     * @return a ArrayList of int[]
     */
    protected static ArrayList<int[]> parseSpansString(String spanString) {

        // this list will be returned
        ArrayList<int[]> spans = new ArrayList<int[]>();

        // check if the tag being processed is non-consuming
        if (spanString.equals("-1~-1")) {
            spans.add(new int[]{-1, -1});
            return spans;
        }

        // split each span
        String[] pairs = spanString.split(SPANSEPARATOR);
        for (String pair : pairs) {
            int[] span = new int[2];

            // parse start and end points
            span[0] = Integer.parseInt(pair.split(SPANDELIMITER)[0]);
            span[1] = Integer.parseInt(pair.split(SPANDELIMITER)[1]);

            spans.add(span);
        }

        return spans;

    }

    /**
     * Takes an array of integer pairs, then merge it into a string.
     * Each span separated by a comma, start and end point of each span joined with a hyphen.
     *
     * @param spans - an sorted set of integer pairs
     * @return a string representing mSpans of a tag
     */
    protected static String spansToString(ArrayList<int[]> spans) {
        String spanString = "";
        Iterator<int[]> iter = spans.iterator();
        while(iter.hasNext()) {
            int[] span = iter.next();
            if (iter.hasNext()) {
                spanString += span[0] + SPANDELIMITER + span[1] + SPANSEPARATOR;
            } else {
                spanString += span[0] + SPANDELIMITER + span[1];
            }

        }

        return spanString;
    }

    /**
     * Highlight given mSpans with given highlighter and painter(color)
     *
     * @param high
     * @param spans
     * @param painter
     */
    private void highlightTextSpans(Highlighter high,
                                    ArrayList<int[]> spans,
                                    TextHighlightPainter painter) {
        for (int[] span : spans) {
            int start = span[0], end = span[1];
            try {
                high.addHighlight(start, end, painter);
                mTextPane.scrollRectToVisible(mTextPane.modelToView(start));
            } catch (BadLocationException b) {
                b.printStackTrace();
            }
        }
    }

    /**
     * Updates the status bar display
     * add by krim
     * TODO add MLINK mode
     *
     */
    private void updateStatusBar() {
        if (!mTask.hasDTD()) {
            mStatusBar.setText("No DTD loaded.");
        } else if (!isFileOpen) {
            mStatusBar.setText("No Adjudication in progress.");
        } else {
            switch (mMode) {
                case NORMAL:
                    if (isSpansEmpty()) {
                        mStatusBar.setText("No text selected.");
                    } else {
                        mStatusBar.setText("Selected: " + spansToString(mSpans));
                    }
                    break;
                case MSPAN:
                    if (isSpansEmpty()) {
                        mStatusBar.setText("[Multi-span mode] No text selected.");
                    } else {
                        mStatusBar.setText("[Multi-span mode] Selected: " + spansToString(mSpans));
                    }
                    break;
                case MLINK:
                    mStatusBar.setText("Multiple link mode: ");
                    break;
            }
        }
    }

    /**
     * Sets MAE mode to Normal
     * added by krim
     */
    private void returnToNormalMode() {

        mMode = NORMAL;
        resetSpans();
        updateMenus();

        mStatusBar.setText("Exit to normal mode! Click anywhere to continue.");
    }

    private Boolean isSpansEmpty() {
        return this.mSpans.size() == 0 || this.mSpans.get(0)[0] == -1;
    }

    /**
     * Resets the selected mSpans to default non-selection (-1~-1)
     */
    private void resetSpans() {
        this.mSpans.clear();
        if (mMode != MSPAN) {
            this.mSpans.add(new int[]{-1, -1});
        }
    }

    /**
     * Create the GUI
     */
    private static void createAndShowGUI() {
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        mMainFrame = new JFrame("MAI");
        mMainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Create and set up the content pane.
        JComponent newContentPane = new MaiMain();
        newContentPane.setOpaque(true); //content panes must be opaque
        mMainFrame.setContentPane(newContentPane);

        //Display the window.
        mMainFrame.pack();
        mMainFrame.setSize(900,700);
        mMainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        createAndShowGUI();
                    }
                });
    }

}