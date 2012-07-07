/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package es.jmfrancofraiz.sleepplayer;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

/**
 * La Activity de Historial gestiona las reproducciones anteriores,
 * dando la posibilidad de retomar una reproduccion en el segundo en el que
 * se dejo.
 * 
 * @author jmffraiz
 *
 */
public class HistoryActivity extends ListActivity {

    private DatabaseHelper dbHelper;
    
    private MyCursorAdapter mca;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list);
        this.getListView().setDividerHeight(2);
        
        //lanza la query y establece el cursor
        dbHelper = new DatabaseHelper(this);
        dbHelper.open();
        Cursor cursor = dbHelper.query();
        startManagingCursor(cursor);
                
        mca = new MyCursorAdapter(this, R.layout.row, cursor);
        setListAdapter(mca);
        
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        
        // Obtiene el elemento que fue clicado
        SQLiteCursor o = (SQLiteCursor)this.getListAdapter().getItem(position);
        String url = o.getString(o.getColumnIndex(DatabaseHelper.FIELD_URL));
        String posicion = o.getString(o.getColumnIndex(DatabaseHelper.FIELD_POSICION));
        
        //lanza la activity principal y le pasa la url
        Uri uri = Uri.parse(url);
        if (posicion != null) uri = uri.buildUpon().fragment(posicion).build();
        Intent intent = new Intent("android.intent.action.VIEW",uri,HistoryActivity.this,SleepPlayerActivity.class);
        startActivity(intent);

    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_historial, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {
        
        case R.id.limpiar_historial:
            limpiarHistorial();
            return true;
                        
        default:
            return super.onOptionsItemSelected(item);
            
        }
        
        
    }

    private void limpiarHistorial() {
    	//borra todos los elementos del historial
        dbHelper.deleteAll();
        Cursor cursor = dbHelper.query();
        mca.changeCursor(cursor);
        mca.notifyDataSetChanged();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
    
}
