package es.jmfrancofraiz.sleepplayer;

import kankan.wheel.widget.adapters.NumericWheelAdapter;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MinutesWheelAdapter extends NumericWheelAdapter {
	
	protected MinutesWheelAdapter(Context context, int minValue, int maxValue, String format) {
		super(context, R.layout.minutos, NO_RESOURCE, minValue,maxValue,format);       
        setItemTextResource(R.id.txtMinutos);
    }
	
    @Override
    public View getItem(int index, View cachedView, ViewGroup parent) {
        
        View view = super.getItem(index, cachedView, parent);

        TextView txtMinutos = (TextView) view.findViewById(R.id.txtMinutos);
        txtMinutos.setText(getItemText(index));
        txtMinutos.setTextColor(0xFF111111);

        return view;
    }


}