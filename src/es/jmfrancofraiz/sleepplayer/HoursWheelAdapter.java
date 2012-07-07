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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import kankan.wheel.widget.adapters.NumericWheelAdapter;

/**
 * Adaptacion del NumericWheelAdapter
 * @author jmffraiz
 *
 */
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