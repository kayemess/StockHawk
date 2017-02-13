package com.udacity.stockhawk.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

/**
 * Created by kristenwoodward on 1/24/17.
 */

public class StockDetail extends AppCompatActivity {

    private Uri mStockUri;
    private String mStockSymbol = "";

    final private static int DATE_POSITION = 0;
    final private static int STOCK_CLOSE_POSITION = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_detail);

        if(savedInstanceState == null){
            Bundle intentExtras = getIntent().getExtras();
            if(intentExtras.getString(Contract.Quote.COLUMN_SYMBOL,"") != ""){
                mStockSymbol = intentExtras.getString(Contract.Quote.COLUMN_SYMBOL);
                setTitle(mStockSymbol);
            }
            if(intentExtras.getString(Contract.Quote.COLUMN_HISTORY,"") != ""){
                LineChart stockChart = (LineChart) findViewById(R.id.chart);
                List<Entry> stockDataEntries = new ArrayList<>();

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("M/d/yy");

                String stockHistory = intentExtras.getString(Contract.Quote.COLUMN_HISTORY);
                String[] stockHistoryStrings = stockHistory.split("\n");
                String[] oneDayOfStockData = new String[]{};

                // convert String[] to an ArrayList so that it can be reversed
                // reverse array so that it goes into the stock data entries array in the correct order
                List<String> stockHistoryArray = new ArrayList<>(Arrays.asList(stockHistoryStrings));
                Collections.reverse(stockHistoryArray);

                final String[] datesForXAxis = new String[stockHistoryArray.size()];

                int i = 0;
                for(String s : stockHistoryArray){
                    oneDayOfStockData = s.split(",");


                    Long dateInMillis = Long.valueOf(oneDayOfStockData[DATE_POSITION]);
                    Date date = new Date(dateInMillis);

                    String dateString = simpleDateFormat.format(date);
                    Timber.d("date ",dateString);
                    datesForXAxis[i] = dateString;

                    Float stockClose = Float.valueOf(oneDayOfStockData[STOCK_CLOSE_POSITION]);

                    stockDataEntries.add(new Entry(i,stockClose));
                    i++;
                }

                LineDataSet stockDataSet = new LineDataSet(stockDataEntries, "Stock Close Prices");
                stockDataSet.setColor(R.color.colorAccent);

                LineData stockData = new LineData(stockDataSet);
                stockChart.setData(stockData);

                IAxisValueFormatter formatter = new IAxisValueFormatter() {
                    @Override
                    public String getFormattedValue(float value, AxisBase axis) {
                        return datesForXAxis[(int) value];
                    }
                };

                // format x-axis to show dates, align to bottom, zoom to granularity of one day
                XAxis xAxis = stockChart.getXAxis();
                xAxis.setGranularity(1f);
                xAxis.setValueFormatter(formatter);
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

                // set description on chart
                Description chartDescription = new Description();
                chartDescription.setText(mStockSymbol + " closing prices");
                stockChart.setDescription(chartDescription);

                Legend legend = stockChart.getLegend();
                legend.setEnabled(false);

                stockChart.invalidate();
            }
        }

    }
}
