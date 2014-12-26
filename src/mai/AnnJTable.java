
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

import javax.swing.JTable;
import javax.swing.table.*;

/**
  * 
  * AnnJModel creates a JTable that 
  * can have a button in the desired location.
  * 
  * @author Amber Stubbs
  */

class AnnJTable extends JTable{

	private static final long serialVersionUID = -436162326023724236L;//generated by Eclipse

AnnJTable(){
    setModel(new AnnTableModel());
}

AnnJTable(AnnTableModel model){
    setModel(model);
}

public TableCellRenderer getCellRenderer(int row, int column){
   DefaultTableModel tableModel = (DefaultTableModel)getModel();
   if (column==tableModel.getColumnCount()-1){
      return new ButtonRenderer();
   }
   else{
      return super.getCellRenderer(row,column);
   }

}



}
