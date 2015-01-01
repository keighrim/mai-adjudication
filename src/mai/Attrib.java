
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

class Attrib extends Object{

/**
 * The parent class for tag attributes
 * 
 * @author Amber Stubbs
 *
 */

Attrib(){
}

public String getName(){
    return name;
}

public void setName(String n){
    name=n;
}

public boolean getRequired(){
    return required;
}

public void setRequired(boolean r){
    required=r;
}

public String getDefaultValue(){
    return defaultValue;
}

public void setDefaultValue(String d){
    defaultValue = d;
}

public boolean hasDefaultValue(){
    return defaultValue.length() != 0;
}

public void printInfo(){
    System.out.println("Attribute name =" + getName() + " , required = " + getRequired());
}

public String toString(){
    return("Attribute name =" + getName() + " , required = " + getRequired() );
}

private String name;
private boolean required;
private String defaultValue;
}