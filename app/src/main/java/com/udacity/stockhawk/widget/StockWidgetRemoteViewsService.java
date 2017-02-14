package com.udacity.stockhawk.widget;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Binder;
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
            private DecimalFormat dollarFormat;
            private DecimalFormat percentageFormat;

            @Override
            public void onCreate() {
                dollarFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(Locale.US);
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

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        stockData == null || !stockData.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.stock_widget_list_item);

                String symbol = stockData.getString(Contract.Quote.POSITION_SYMBOL);
                String price = stockData.getString(Contract.Quote.POSITION_PRICE);

                float rawAbsoluteChange = stockData.getFloat(Contract.Quote.POSITION_ABSOLUTE_CHANGE);
                float percentageChange = stockData.getFloat(Contract.Quote.POSITION_PERCENTAGE_CHANGE);

                String percentage = percentageFormat.format(percentageChange / 100);
                views.setTextViewText(R.id.widget_change, percentage);

                int colorGreen = getColor(R.color.material_green_700);
                int colorRed = getColor(R.color.material_red_700);

                if (rawAbsoluteChange > 0) {
                    views.setInt(R.id.widget_change,"setBackgroundColor", colorGreen);
                } else {
                    views.setInt(R.id.widget_change,"setBackgroundColor",colorRed);
                }

                views.setTextViewText(R.id.widget_symbol,symbol);
                views.setTextViewText(R.id.widget_price,price);

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
