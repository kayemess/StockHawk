package com.udacity.stockhawk.sync;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import com.udacity.stockhawk.R;
import com.udacity.stockhawk.data.Contract;
import com.udacity.stockhawk.data.PrefUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import timber.log.Timber;

import com.udacity.stockhawk.widget.StockWidgetProvider;

import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.HistoricalQuote;
import yahoofinance.histquotes.Interval;
import yahoofinance.quotes.stock.StockQuote;

public final class QuoteSyncJob {

    private static final int ONE_OFF_ID = 2;
    public static final String ACTION_DATA_UPDATED = "com.udacity.stockhawk.ACTION_DATA_UPDATED";
    private static final int PERIOD = 300000;
    private static final int INITIAL_BACKOFF = 10000;
    private static final int PERIODIC_ID = 1;
    private static final int YEARS_OF_HISTORY = 2;

    private QuoteSyncJob() {
    }

    static void getQuotes(Context context, @Nullable String stockSymbol) {

        Timber.d("Running sync job");

        Cursor savedStocks;
        String[] stockArray;
        Set<String> stockCopy = new HashSet<>();
        Set<String> addedStocks = new HashSet<>();

        Calendar from = Calendar.getInstance();
        Calendar to = Calendar.getInstance();
        from.add(Calendar.YEAR, -YEARS_OF_HISTORY);

        try {
            // Check to see if getQuotes request was for a single stock, or all in DB
            // If for all stocks, query DB
            if (stockSymbol == null) {
                savedStocks = context.getContentResolver().query(Contract.Quote.URI, null, null, null, null);
                // If DB is empty, load default stocks from shared preferences
                if (savedStocks == null || !savedStocks.moveToPosition(0)) {
                    addedStocks = PrefUtils.getStocks(context);
                    stockArray = addedStocks.toArray(new String[addedStocks.size()]);

                }
                // Otherwise, populate HashSet w/ stocks from DB
                else {
                    stockArray = new String[savedStocks.getCount()];
                    for (int i = 0; i < savedStocks.getCount(); i++) {
                        savedStocks.moveToPosition(i);
                        stockArray[i] = savedStocks.getString(Contract.Quote.POSITION_SYMBOL);
                    }
                    addedStocks = new HashSet<>(Arrays.asList(stockArray));
                }
            }
            // Otherwise, if for a single stock, populate HashSet with single stock
            else {
                stockArray = new String[]{stockSymbol};
                addedStocks.addAll(Arrays.asList(stockArray));
            }

            if (stockArray.length == 0) {
                return;
            }

            Map<String, Stock> quotes = YahooFinance.get(stockArray);

            // copy HashSet to use for iterating
            stockCopy.addAll(addedStocks);
            Iterator<String> iterator = stockCopy.iterator();

            ArrayList<ContentValues> quoteCVs = new ArrayList<>();

            while (iterator.hasNext()) {
                String symbol = iterator.next();


                Stock stock = quotes.get(symbol);
                StockQuote quote = stock.getQuote();

                // check to see if the quote is valid, if it's not valid getPrice will be null
                // and the invalid quote symbol will be removed from sharedPreferences and from quotes map
                if (quote.getPrice() == null) {
                    //PrefUtils.removeStock(context,symbol);
                    quotes.remove(symbol);
                    break;
                }

                float price = quote.getPrice().floatValue();
                float change = quote.getChange().floatValue();
                float percentChange = quote.getChangeInPercent().floatValue();

                // WARNING! Don't request historical data for a stock that doesn't exist!
                // The request will hang forever X_x
                List<HistoricalQuote> history = stock.getHistory(from, to, Interval.WEEKLY);

                StringBuilder historyBuilder = new StringBuilder();

                for (HistoricalQuote it : history) {
                    historyBuilder.append(it.getDate().getTimeInMillis());
                    historyBuilder.append(", ");
                    historyBuilder.append(it.getClose());
                    historyBuilder.append("\n");
                }

                ContentValues quoteCV = new ContentValues();
                quoteCV.put(Contract.Quote.COLUMN_SYMBOL, symbol);
                quoteCV.put(Contract.Quote.COLUMN_PRICE, price);
                quoteCV.put(Contract.Quote.COLUMN_PERCENTAGE_CHANGE, percentChange);
                quoteCV.put(Contract.Quote.COLUMN_ABSOLUTE_CHANGE, change);


                quoteCV.put(Contract.Quote.COLUMN_HISTORY, historyBuilder.toString());

                quoteCVs.add(quoteCV);

            }

            context.getContentResolver()
                    .bulkInsert(
                            Contract.Quote.URI,
                            quoteCVs.toArray(new ContentValues[quoteCVs.size()]));

            Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
            context.sendBroadcast(dataUpdatedIntent);

            Intent intent = new Intent(context, StockWidgetProvider.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);  //Adding latest data to the intent
            context.sendBroadcast(intent);

        } catch (IOException exception) {
            Timber.e(exception, "Error fetching stock quotes");
        }
    }

    private static void schedulePeriodic(Context context) {
        Timber.d("Scheduling a periodic task");


        JobInfo.Builder builder = new JobInfo.Builder(PERIODIC_ID, new ComponentName(context, QuoteJobService.class));


        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD)
                .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        scheduler.schedule(builder.build());
    }


    public static synchronized void initialize(final Context context) {

        schedulePeriodic(context);
        syncImmediately(context, null);

    }

    public static synchronized void syncImmediately(Context context, @Nullable String stockSymbol) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            Intent nowIntent = new Intent(context, QuoteIntentService.class);
            if (stockSymbol != null) {
                nowIntent.putExtra(Contract.Quote.COLUMN_SYMBOL, stockSymbol);
            }
            context.startService(nowIntent);
        } else {

            JobInfo.Builder builder = new JobInfo.Builder(ONE_OFF_ID, new ComponentName(context, QuoteJobService.class));


            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(INITIAL_BACKOFF, JobInfo.BACKOFF_POLICY_EXPONENTIAL);


            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            scheduler.schedule(builder.build());


        }
    }

}
