package es.jmfrancofraiz.sleepplayer;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

public class HoursWheelAdapter extends NumericWheelAdapter  {

	protected HoursWheelAdapter(Context context, int minValue, int maxValue, String format) {
		super(context, R.layout.horas, NO_RESOURCE, minValue,maxValue,format);   
        setItemTextResource(R.id.txtHoras);
    }
	
	@Override
    public View getItem(int index, View cachedView, ViewGroup parent) {
        
        View view = super.getItem(index, cachedView, parent);

        TextView txtHoras = (TextView) view.findViewById(R.id.txtHoras);
        txtHoras.setText(getItemText(index));
        txtHoras.setTextColor(0xFF111111);

        return view;
    }


}