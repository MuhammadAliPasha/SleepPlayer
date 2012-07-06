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

public class HistoryActivity extends ListActivity {

	private DatabaseHelper dbHelper;
	
	private MyCursorAdapter mca;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.list);
		this.getListView().setDividerHeight(2);
		
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
		
		// Get the item that was clicked
		SQLiteCursor o = (SQLiteCursor)this.getListAdapter().getItem(position);
		String url = o.getString(o.getColumnIndex(DatabaseHelper.FIELD_URL));
		String posicion = o.getString(o.getColumnIndex(DatabaseHelper.FIELD_POSICION));
		
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
