package com.udacity.stockhawk.sync;

import android.app.IntentService;
import android.content.Intent;

import com.udacity.stockhawk.data.Contract;

import timber.log.Timber;


public class QuoteIntentService extends IntentService {

    public QuoteIntentService() {
        super(QuoteIntentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Timber.d("Intent handled");
        String stockSymbol = null;
        if(intent.hasExtra(Contract.Quote.COLUMN_SYMBOL)){
            stockSymbol = intent.getStringExtra(Contract.Quote.COLUMN_SYMBOL);
        }
        QuoteSyncJob.getQuotes(getApplicationContext(), stockSymbol);
    }
}
