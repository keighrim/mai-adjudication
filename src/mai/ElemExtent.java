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


/**
 * Extents Elem to provid information about tags that are
 * used to label extents in a text (as well as non-conumsing
 * tags).
 *
 * @author Amber Stubbs
 * @revised Keigh Rim
 *
 */


class ElemExtent extends Elem{

    // mod by krim: start, end --> spans
    private String mSpans;

    ElemExtent(String name, String pre){
        setName(name);
        //extent tags always have id, start, and end
        AttID id = new AttID("id", pre, true);
        addAttribute(id);


        // mod by krim: start, end --> spans
        AttData spans = new AttData("spans", true);
        addAttribute(spans);

        AttData text = new AttData("text", false);
        addAttribute(text);
    }

    /* mod by krim: these setters and getters are no longer used
    public void setStart(int s) {
        start = s;
    }

    public int getStart(){
        return start;
    }

    public void setEnd(int e){
        end=e;
    }

    public int getEnd(){
        return end;
    }
    */

    public void setSpans(String spans) {
        this.mSpans = spans;
    }

    public String getSpans() {
        return mSpans;
    }

    public void printInfo(){
        System.out.println("\tname = " + getName());

        // mod by krim: start, end --> spans
        System.out.println("\tspans = " + getSpans());
    }
}

