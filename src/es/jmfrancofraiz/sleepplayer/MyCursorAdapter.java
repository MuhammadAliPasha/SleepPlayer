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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

/**
 * Adaptador de cursor para presentar cada elemento del historial
 * en el formato que quiero
 * 
 * @author jmffraiz
 *
 */
public class MyCursorAdapter extends ResourceCursorAdapter {

    public MyCursorAdapter(Context context, int layout, Cursor c) {
        super(context, layout, c);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.row, parent, false);
        return view;
    }
    
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        
        TextView rFecha = (TextView) view.findViewById(R.id.rfecha);
        String playTime = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIELD_PLAYTIME));
        rFecha.setText(calcularFecha(playTime,context));

        TextView rUrl = (TextView) view.findViewById(R.id.rurl);
        String url = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIELD_URL));
        rUrl.setText(url);

        TextView rPosicion = (TextView) view.findViewById(R.id.rposicion);
        String posicion = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIELD_POSICION));
        rPosicion.setText(posicion);

        TextView rDuracion = (TextView) view.findViewById(R.id.rduracion);
        String duracion = cursor.getString(cursor.getColumnIndex(DatabaseHelper.FIELD_DURACION));
        rDuracion.setText(duracion);

    }
    
    private String calcularFecha(String playTime, Context ctx) {

        StringBuilder texto = new StringBuilder();
        
        Calendar ahora = Calendar.getInstance();
        Calendar fecha = null;
        
        try {
            SimpleDateFormat formatoDelTexto = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = formatoDelTexto.parse(playTime);
            fecha = Calendar.getInstance();
            fecha.setTime(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return texto.toString();
        }
        
        long diffHours = (ahora.getTimeInMillis() - fecha.getTimeInMillis()) / (60 * 60 * 1000);
        
        
        if ((diffHours / 24) <= 1) {
            
            if ((diffHours / 24) == 0) {
                
                if ((diffHours % 24) < ahora.get(Calendar.HOUR_OF_DAY)) {
                    texto.append(ctx.getString(R.string.hoy));
                } else {
                    texto.append(ctx.getString(R.string.ayer));
                }
                                
            } else {
                
                texto.append(ctx.getString(R.string.ayer));

            }
            
        } else {
        
            if (diffHours < 168) {
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE");
                texto.append(sdf.format(fecha.getTime()));
            } else {
                DateFormat dateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
                texto.append(dateFormatter.format(fecha.getTime()));
            }
            
        }
    
        texto.append(" " + ctx.getString(R.string.alas) + " ");
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        texto.append(sdf.format(fecha.getTime()));
        
        return texto.toString();
        
    }

}
