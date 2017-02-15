package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import static com.udacity.stockhawk.R.id.change;

/**
 * Created by kristenwoodward on 2/8/17.
 */

public class StockWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        return new RemoteViewsFactory() {
            private Cursor stockData = null;
            private DecimalFormat dollarFormatWithPlus;
            //private DecimalFormat dollarFormat;
            private DecimalFormat percentageFormat;

            @Override
            public void onCreate() {
                //dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
                dollarFormatWithPlus.setPositivePrefix("+$");
                percentageFormat = (DecimalFormat) NumberFormat.getPercentInstance(Locale.getDefault());
                percentageFormat.setMaximumFractionDigits(2);
                percentageFormat.setMinimumFractionDigits(2);
                percentageFormat.setPositivePrefix("+");
            }

            @Override
            public void onDataSetChanged() {

                if(stockData != null){
                    stockData.close();;
                }

                final long identityToken = Binder.clearCallingIdentity();
                stockData = getContentResolver().query(Contract.Quote.URI,null,null,null,null,null);
                Binder.restoreCallingIdentity(identityToken);

            }

            @Override
            public void onDestroy() {

                if (stockData != null) {
                    stockData.close();
                    stockData = null;
                }
            }

            @Override
            public int getCount() {
                return stockData == null ? 0 : stockData.getCount();
            }


            // Given the position (index) of a WidgetItem in the array, use the item's text values in
            // combination with the app widget item XML file to construct a RemoteViews object.
            @Override
            public RemoteViews getViewAt(int position) {
                // First, check that the position is valid and that the stockData Cursor is not null
                if (position == AdapterView.INVALID_POSITION ||
                        stockData == null || !stockData.moveToPosition(position)) {
                    return null;
                }

                stockData.moveToPosition(position);

                // Construct a RemoteViews item based on the app widget item XML file, and set the
                // text based on the position.
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.stock_widget_list_item);

                String symbol = stockData.getString(Contract.Quote.POSITION_SYMBOL);
                views.setTextViewText(R.id.widget_symbol,symbol);

                String price = stockData.getString(Contract.Quote.POSITION_PRICE);
                views.setTextViewText(R.id.widget_price,price);

                float rawAbsoluteChange = stockData.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = stockData.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                // Format the percent change for appropriate display in TextView
                String percentage = percentageFormat.format(percentageChange / 100);
                views.setTextViewText(R.id.widget_change, percentage);

                // Define colors from colors.xml to more easily assign pill color in widget
                int colorGreen = getColor(R.color.material_green_700);
                int colorRed = getColor(R.color.material_red_700);

                // Assign pill color for stock depending on whether the percent change was positive or negative
                if (rawAbsoluteChange > 0) {
                    views.setInt(R.id.widget_change,"setBackgroundColor", colorGreen);
                } else {
                    views.setInt(R.id.widget_change,"setBackgroundColor",colorRed);
                }

                // Next, set a fill-intent, which will be used to fill in the pending intent template
                // that is set on the collection view in StockWidgetProvider.
                Intent fillIntent = new Intent();
                fillIntent.putExtra(Contract.Quote.COLUMN_SYMBOL,symbol);

                String history = stockData.getString(Contract.Quote.POSITION_HISTORY);
                fillIntent.putExtra(Contract.Quote.COLUMN_HISTORY,history);

                // Make it possible to distinguish the individual on-click
                // action of a given item using fillIntent w/ Extras
                views.setOnClickFillInIntent(R.id.widget_list_item,fillIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.stock_widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if(stockData.moveToPosition(position)){
                    return stockData.getInt(Contract.Quote.POSITION_ID);
                } return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
