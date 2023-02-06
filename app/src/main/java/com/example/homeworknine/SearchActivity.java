package com.example.homeworknine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;


public class SearchActivity extends AppCompatActivity {
    SearchActivity search_context;

    private ArrayList<StockData> portfolio_data;
    private ArrayList<StockData> favorties_data;
    private Float cash_holding;
    SharedPreferences shared_prefs;
    SharedPreferences.Editor sp_editor;

    String ticker;
    String price;
    String compName;
    String change;
    private AlertDialog alertDialog;

    NewsAdapter news_adapter;
    ArrayList<NewsData> news_items = new ArrayList<NewsData>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        search_context = this;

        ProgressBar spinner = (ProgressBar) findViewById(R.id.spinner);
        spinner.setVisibility(View.VISIBLE);
        ConstraintLayout search_layout = (ConstraintLayout)findViewById(R.id.search_layout);
        for (int i=0; i<search_layout.getChildCount(); i++) {
            View subview = search_layout.getChildAt(i);
            if(!(subview instanceof ProgressBar)){
                subview.setVisibility(View.GONE);
            }
        }

        Toolbar search_toolbar = (Toolbar) findViewById(R.id.search_toolbar);
        setSupportActionBar(search_toolbar);
        ActionBar action_bar = getSupportActionBar();
        action_bar.setDisplayHomeAsUpEnabled(true);
        action_bar.setDisplayShowTitleEnabled(false);

        shared_prefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        sp_editor = shared_prefs.edit();
        String portfolio_list_str = shared_prefs.getString("portfolio_list", "");
        String favorites_list_str = shared_prefs.getString("favorites_list", "");
        cash_holding = shared_prefs.getFloat("cash_holding", 25000);
        portfolio_data = new ArrayList(StockData.fromString(portfolio_list_str));
        favorties_data = new ArrayList(StockData.fromString(favorites_list_str));


        news_adapter = new NewsAdapter(news_items);
        news_adapter.context = this;

        RecyclerView news_rec_view = (RecyclerView) findViewById(R.id.news_cards_list);
        news_rec_view.setAdapter(news_adapter);

        news_rec_view.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        String query = "";
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY).toUpperCase();
        }

        WebView hourly_charts_webview = (WebView) findViewById(R.id.hourly_charts_webview);
        hourly_charts_webview.getSettings().setJavaScriptEnabled(true);
        hourly_charts_webview.loadUrl("http://192.168.1.68:3000/charts/hourly?ticker=" + query);

        WebView historial_charts_webview = (WebView) findViewById(R.id.historial_charts_webview);
        historial_charts_webview.getSettings().setJavaScriptEnabled(true);
        historial_charts_webview.loadUrl("http://192.168.1.68:3000/charts/hist?ticker=" + query);

        WebView recomendation_trends_webview = (WebView) findViewById(R.id.recomendation_trends_webview);
        recomendation_trends_webview.getSettings().setJavaScriptEnabled(true);
        recomendation_trends_webview.loadUrl("http://192.168.1.68:3000/charts/rec?ticker=" + query);

        WebView historical_eps_webview = (WebView) findViewById(R.id.historical_eps_webview);
        historical_eps_webview.getSettings().setJavaScriptEnabled(true);
        historical_eps_webview.loadUrl("http://192.168.1.68:3000/charts/eps?ticker=" + query);

        String ticker = query;
        getJSONData(ticker);

        ImageView star_button = (ImageView) findViewById(R.id.favorited_star);
        star_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View img_view) {
                favoriteStarAction(star_button);
            }
        });
        StockData portfolio_result = StockData.getItemByTicker(favorties_data, ticker);
        if(portfolio_result == null){
            ((ImageView)findViewById(R.id.favorited_star)).setImageResource(R.drawable.star_outline);
        } else {
            ((ImageView)findViewById(R.id.favorited_star)).setImageResource(R.drawable.star);
        }

        Button trade_button = (Button) findViewById(R.id.trade_button);
        trade_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View img_view) {
                openBuyDialogue();
            }
        });

        ImageView hourly_button = (ImageView) findViewById(R.id.charts_hourly_button);
        ImageView historical_button = (ImageView) findViewById(R.id.charts_historical_button);

        hourly_charts_webview.setVisibility(View.VISIBLE);
        historial_charts_webview.setVisibility(View.GONE);

        hourly_button.setColorFilter(Color.BLUE);
        ImageView hourly_underline = (ImageView) findViewById(R.id.charts_hourly_button_underline);
        hourly_underline.setBackgroundColor(Color.BLUE);

        hourly_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View img_view) {
                WebView hourly_charts_webview = (WebView) findViewById(R.id.hourly_charts_webview);
                WebView historial_charts_webview = (WebView) findViewById(R.id.historial_charts_webview);

                hourly_charts_webview.setVisibility(View.VISIBLE);
                historial_charts_webview.setVisibility(View.GONE);

                hourly_button.setColorFilter(Color.BLUE);
                historical_button.setColorFilter(Color.BLACK);
                ImageView historical_underline = (ImageView) findViewById(R.id.charts_historical_button_underline);
                ImageView hourly_underline = (ImageView) findViewById(R.id.charts_hourly_button_underline);
                historical_underline.setBackgroundColor(Color.WHITE);
                hourly_underline.setBackgroundColor(Color.BLUE);
            }
        });

        historical_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View img_view) {
                WebView hourly_charts_webview = (WebView) findViewById(R.id.hourly_charts_webview);
                WebView historial_charts_webview = (WebView) findViewById(R.id.historial_charts_webview);

                hourly_charts_webview.setVisibility(View.GONE);
                historial_charts_webview.setVisibility(View.VISIBLE);

                historical_button.setColorFilter(Color.BLUE);
                hourly_button.setColorFilter(Color.BLACK);
                ImageView historical_underline = (ImageView) findViewById(R.id.charts_historical_button_underline);
                ImageView hourly_underline = (ImageView) findViewById(R.id.charts_hourly_button_underline);
                historical_underline.setBackgroundColor(Color.BLUE);
                hourly_underline.setBackgroundColor(Color.WHITE);
            }
        });

    }

    private void getJSONData(String tick) {
        String url = "http://192.168.1.68:3000/server/detailsdata/" + tick;
        RequestQueue req_queue = Volley.newRequestQueue(this);
        req_queue.getCache().clear();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            ticker = tick;
                            price = response.getJSONObject("price").getString("c");
                            compName = response.getJSONObject("profile").getString("name");

                            ((ImageView)findViewById(R.id.icon_image)).setImageURI(Uri.parse(response.getJSONObject("profile").getString("logo")));
                            ((TextView)findViewById(R.id.ticker)).setText(ticker);
                            ((TextView)findViewById(R.id.comp_name)).setText(compName);
                            ((TextView)findViewById(R.id.price)).setText(price);

                            String raw_change_str = response.getJSONObject("price").getString("d");
                            String raw_change_pc = response.getJSONObject("price").getString("dp");

                            String change_str = "$ " + raw_change_str + " (" + raw_change_pc + "%)";
                            TextView change_view = ((TextView) findViewById(R.id.change));
                            change_view.setText(change_str);
                            change = change_str;

                            Glide.with(search_context)
                                    .asBitmap()
                                    .load(response.getJSONObject("profile").getString("logo"))
                                    .into((ImageView)findViewById(R.id.icon_image));

                            ((TextView)findViewById(R.id.s_open_price)).setText(response.getJSONObject("price").getString("o"));
                            ((TextView)findViewById(R.id.s_high_price)).setText(response.getJSONObject("price").getString("h"));
                            ((TextView)findViewById(R.id.s_low_price)).setText(response.getJSONObject("price").getString("l"));
                            ((TextView)findViewById(R.id.s_prev_close)).setText(response.getJSONObject("price").getString("pc"));

                            ((TextView)findViewById(R.id.a_ipo_start)).setText(response.getJSONObject("profile").getString("ipo"));
                            ((TextView)findViewById(R.id.s_industry)).setText(response.getJSONObject("profile").getString("finnhubIndustry"));

                            TextView web_page_link = ((TextView)findViewById(R.id.a_webpage));
                            String company_page = String.format("<a href=\"%s\">" + response.getJSONObject("profile").getString("weburl") + "</a> ",
                                    response.getJSONObject("profile").getString("weburl"));

                            web_page_link.setText(Html.fromHtml(company_page));
                            web_page_link.setTextColor(Color.BLUE);
                            web_page_link.setMovementMethod(LinkMovementMethod.getInstance());

                            LinearLayout peers_main = ((LinearLayout)findViewById(R.id.a_peers));

                            int peer_count = 0;
//                            LinearLayout cur_line = null;
                            for (int i=0; i<response.getJSONArray("peers").length(); i++) {
                                if (peer_count == 5)
                                {
                                    break;
                                }
//                                if (peer_count % 5 == 0){
//                                    LinearLayout new_line = new LinearLayout(search_context);
//                                    new_line.setId(ThreadLocalRandom.current().nextInt());
//                                    new_line.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//                                    new_line.setOrientation(LinearLayout.HORIZONTAL);
//                                    new_line.setGravity(Gravity.LEFT);
//
//                                    if(cur_line != null){
//                                        ConstraintSet c_set = new ConstraintSet();
//                                        c_set.connect(new_line.getId(), ConstraintSet.TOP, cur_line.getId(), ConstraintSet.BOTTOM);
//                                        c_set.applyTo(peers_main);
//                                    }
//
//                                    cur_line = new_line;
//                                    peers_main.addView(cur_line);
//                                }
                                peer_count = peer_count + 1;

                                TextView peer_text_view = new TextView(search_context);
                                String peer = response.getJSONArray("peers").getString(i);
                                peer_text_view.setPadding(0,0,20,0);
                                peer_text_view.setTextColor(Color.BLUE);

                                SpannableString content = new SpannableString(peer);
                                content.setSpan(new UnderlineSpan(), 0, peer.length(), 0);
                                peer_text_view.setText(content);

                                peer_text_view.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        Intent intent = new Intent(search_context, SearchActivity.class);
                                        intent.putExtra(SearchManager.QUERY, peer);
                                        intent.setAction(Intent.ACTION_SEARCH);
                                        startActivity(intent);
                                    }
                                });
                                //cur_line.addView(peer_text_view);
                                peers_main.addView(peer_text_view);
                            }





                            Integer i_tot_reddit = 0;
                            Integer i_tot_twitter = 0;
                            Integer i_pos_reddit = 0;
                            Integer i_pos_twitter = 0;
                            Integer i_neg_reddit = 0;
                            Integer i_neg_twitter = 0;

                            for (int i=0; i<response.getJSONObject("socsent").getJSONArray("twitter").length(); i++){
                                JSONObject day_twitter_data = (JSONObject)response.getJSONObject("socsent").getJSONArray("twitter").get(i);
                                JSONObject day_reddit_data = (JSONObject)response.getJSONObject("socsent").getJSONArray("reddit").get(i);

                                i_tot_reddit = i_tot_reddit + Integer.parseInt(day_reddit_data.getString("mention"));
                                i_tot_twitter = i_tot_twitter + Integer.parseInt(day_twitter_data.getString("mention"));
                                i_pos_reddit = i_pos_reddit + Integer.parseInt(day_reddit_data.getString("positiveMention"));
                                i_pos_twitter = i_pos_twitter + Integer.parseInt(day_twitter_data.getString("positiveMention"));
                                i_neg_reddit = i_neg_reddit + Integer.parseInt(day_reddit_data.getString("negativeMention"));
                                i_neg_twitter = i_neg_twitter + Integer.parseInt(day_twitter_data.getString("negativeMention"));

                            }

                            ((TextView)findViewById(R.id.i_comp_name)).setText(response.getJSONObject("profile").getString("name"));
                            ((TextView)findViewById(R.id.i_tot_reddit)).setText(i_tot_reddit.toString());
                            ((TextView)findViewById(R.id.i_tot_twitter)).setText(i_tot_twitter.toString());
                            ((TextView)findViewById(R.id.i_pos_reddit)).setText(i_pos_reddit.toString());
                            ((TextView)findViewById(R.id.i_pos_twitter)).setText(i_pos_twitter.toString());
                            ((TextView)findViewById(R.id.i_neg_reddit)).setText(i_neg_reddit.toString());
                            ((TextView)findViewById(R.id.i_neg_twitter)).setText(i_neg_twitter.toString());

                            ProgressBar spinner = (ProgressBar) findViewById(R.id.spinner);
                            spinner.setVisibility(View.GONE);
                            ConstraintLayout search_layout = (ConstraintLayout)findViewById(R.id.search_layout);
                            for (int i=0; i<search_layout.getChildCount(); i++) {
                                View subview = search_layout.getChildAt(i);
                                if(!(subview instanceof ProgressBar)){
                                    subview.setVisibility(View.VISIBLE);
                                }
                            }

                            calculatePortfolioChanges();


                            Float raw_change = Float.valueOf(raw_change_str);
                            ImageView trend_icon = (ImageView) findViewById(R.id.trending_img_search);
                            if (raw_change < new Float(0)) {
                                change_view.setTextColor(Color.RED);
                                trend_icon.setColorFilter(Color.RED);
                            } else if (raw_change == new Float(0)) {
                                change_view.setTextColor(Color.DKGRAY);
                                trend_icon.setColorFilter(Color.GRAY);
                            } else {
                                change_view.setTextColor(Color.rgb(23, 123, 53));
                                trend_icon.setColorFilter(Color.rgb(23, 123, 53));
                                trend_icon.setImageResource(R.drawable.trending_up);
                            }

                            JSONArray news_json = response.getJSONArray("news");
                            int count = 0;
                            for (int i=0; i<news_json.length(); i++) {
                                if(count == 5){
                                    break;
                                }

                                JSONObject item = news_json.getJSONObject(i);
                                if(!item.getString("image").isEmpty()){
                                    NewsData news_data = new NewsData();
                                    news_data.title = item.getString("headline");
                                    news_data.card_image_src = item.getString("image");
                                    news_data.summary = item.getString("summary");
                                    news_data.source = item.getString("source");
                                    news_data.timestamp = item.getString("datetime");
                                    news_data.article_link = item.getString("url");
                                    news_items.add(news_data);
                                    count++;
                                }
                            }
                            news_adapter.notifyDataSetChanged();


                        } catch (Exception jsonException) {
                            jsonException.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("ERROR!!!\n" + error.getLocalizedMessage());
                    }
                });

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                2,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            req_queue.add(jsonObjectRequest);


    }

    private void calculatePortfolioChanges() {
        Object[] results = CalculateSell(ticker, Float.MAX_VALUE);

        Float total_val = (Float) results[3];
        Float shares_owned = (Float) results[2];
        Float price_pr_share = total_val / shares_owned;
        Float float_price = Float.valueOf(price);
        Float market_val = shares_owned * float_price;
        Float chg_prc_frm_tot_cst = (float_price - price_pr_share);
        String portfolio_change_str = "$ " + String.format("%.2f",chg_prc_frm_tot_cst);

        TextView mkt_val_view_p = ((TextView) findViewById(R.id.p_mkt_val));
        TextView change_view_p = ((TextView)findViewById(R.id.p_chng));
        ((TextView)findViewById(R.id.p_shares_owned)).setText(String.valueOf(shares_owned.intValue()));
        ((TextView)findViewById(R.id.p_avg_cost_share)).setText(String.format("%.2f",price_pr_share));
        ((TextView)findViewById(R.id.p_tot_cost)).setText(String.format("%.2f",total_val));
        change_view_p.setText(portfolio_change_str);
        mkt_val_view_p.setText(String.format("%.2f",market_val));

        if (chg_prc_frm_tot_cst > 0){
            change_view_p.setTextColor(Color.GREEN);
            mkt_val_view_p.setTextColor(Color.GREEN);
        } else if (chg_prc_frm_tot_cst == 0){
            change_view_p.setTextColor(Color.GRAY);
            mkt_val_view_p.setTextColor(Color.GRAY);
        } else {
            change_view_p.setTextColor(Color.RED);
            mkt_val_view_p.setTextColor(Color.RED);
        }
    }


    public void favoriteStarAction(View view) {
        StockData portfolio_result = StockData.getItemByTicker(favorties_data, ticker);
        if(portfolio_result == null){
            favorties_data.add(new StockData(ticker, price, compName, change));
            ((ImageView)findViewById(R.id.favorited_star)).setImageResource(R.drawable.star);
        } else {
            favorties_data.remove(portfolio_result);
            ((ImageView)findViewById(R.id.favorited_star)).setImageResource(R.drawable.star_outline);
        }


        sp_editor.putString("favorites_list", StockData.toString(favorties_data));
        sp_editor.apply();
    }

    public void openBuyDialogue()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.RoundedCornersDialog);
        builder.setView(R.layout.dialogue_trade);

        alertDialog = builder.create();
        alertDialog.show();

        TextView trade_dialog_title = (TextView) alertDialog.findViewById(R.id.trade_dialog_title);
        trade_dialog_title.setText("Trade " + compName + " shares");

        TextView display_calculation_text = (TextView) alertDialog.findViewById(R.id.trade_success_msg);
        Float cash_holding = shared_prefs.getFloat("cash_holding", 25000);
        display_calculation_text.setText("$" + String.format("%.2f", cash_holding) + " to buy " + ticker);

        EditText shares_text_input = alertDialog.findViewById(R.id.trade_dialog_inputsharestext);
        shares_text_input.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String output_text = charSequence.toString() + "*$" + price + "/share = ";
                try {
                    Float input_amount = Float.valueOf(charSequence.toString());
                    Float total = Float.valueOf(price) * input_amount;
                    output_text = output_text + String.format("%.2f", total);
                    TextView display_calculation_text = (TextView) alertDialog.findViewById(R.id.trade_dialog_displaycalculation);
                    display_calculation_text.setText(output_text);
                } catch (Exception ex){
                    //Do nothing
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });


        Button buy_button = alertDialog.findViewById(R.id.trade_dialog_buy_button);
        buy_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText shares_text_input = alertDialog.findViewById(R.id.trade_dialog_inputsharestext);
                String amount = shares_text_input.getText().toString();
                StockData purchase_item = new StockData(ticker, price, amount, "");

                try{
                    Float num_shares = Float.valueOf(amount);

                    if (num_shares <= 0){
                        Toast message = Toast.makeText(search_context, "Cannot buy non-positive shares", Toast.LENGTH_SHORT);
                        message.show();
                    }

                    Float purchase_value = num_shares * Float.valueOf(price);
                    Float cash_holding = shared_prefs.getFloat("cash_holding", 25000);

                    if(purchase_value > cash_holding){
                        Toast message = Toast.makeText(search_context, "Not enough money to buy", Toast.LENGTH_SHORT);
                        message.show();
                    } else {
                        portfolio_data.add(purchase_item);
                        sp_editor.putString("portfolio_list", StockData.toString(portfolio_data));
                        cash_holding = cash_holding - purchase_value;
                        sp_editor.putFloat("cash_holding", cash_holding);

                        calculatePortfolioChanges();
                        sp_editor.apply();
                        alertDialog.cancel();

                        AlertDialog.Builder success_builder = new AlertDialog.Builder(search_context, R.style.RoundedCornersDialog);
                        success_builder.setView(R.layout.dialogue_trade_sucess);

                        AlertDialog successDialog = success_builder.create();
                        successDialog.show();
                        
                        TextView trade_success_msg = (TextView) successDialog.findViewById(R.id.trade_success_msg);
                        trade_success_msg.setText("You have successfully bought " + String.valueOf(num_shares.intValue()) + " shares of " + ticker);
                        
                        Button trade_success_button = (Button) successDialog.findViewById(R.id.trade_success_button);
                        trade_success_button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                successDialog.cancel();
                            }
                        });
                        
                    }
                } catch(Exception ex){
                    Toast message = Toast.makeText(search_context, "Please enter a valid amount", Toast.LENGTH_SHORT);
                    message.show();
                }

            }
        });

        Button sell_button = alertDialog.findViewById(R.id.trade_success_button);
        sell_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText shares_text_input = alertDialog.findViewById(R.id.trade_dialog_inputsharestext);
                try {
                    Float amount = Float.valueOf(shares_text_input.getText().toString());

                    if(amount <= 0){
                        Toast message = Toast.makeText(search_context, "Cannot sell non-positive shares", Toast.LENGTH_SHORT);
                        message.show();
                    } else {
                        Object[] sell_info = CalculateSell(ticker, amount);

                        if((Boolean)sell_info[0]){
                            ArrayList<StockData> items_to_sell = (ArrayList<StockData>)sell_info[1];
                            Float remaining_amount = amount;
                            for (int i=0; i<items_to_sell.size(); i++) {

                                if (remaining_amount >= Float.valueOf(items_to_sell.get(i).compOrShares)) {
                                    remaining_amount = remaining_amount - Float.valueOf(items_to_sell.get(i).compOrShares);
                                    portfolio_data.remove(items_to_sell.get(i));
                                } else {
                                    StockData item = items_to_sell.get(i);
                                    item.compOrShares = String.valueOf(Float.valueOf(items_to_sell.get(i).compOrShares) - remaining_amount);
                                    remaining_amount = new Float(0);
                                }

                            }

                            calculatePortfolioChanges();

                            sp_editor.putString("portfolio_list", StockData.toString(portfolio_data));
                            sp_editor.apply();
                            alertDialog.cancel();

                            AlertDialog.Builder success_builder = new AlertDialog.Builder(search_context, R.style.RoundedCornersDialog);
                            success_builder.setView(R.layout.dialogue_trade_sucess);

                            AlertDialog successDialog = success_builder.create();
                            successDialog.show();

                            TextView trade_success_msg = (TextView) successDialog.findViewById(R.id.trade_success_msg);
                            trade_success_msg.setText("You have successfully sold " + String.valueOf(amount.intValue()) + " shares of " + ticker);

                            Button trade_success_button = (Button) successDialog.findViewById(R.id.trade_success_button);
                            trade_success_button.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    successDialog.cancel();
                                }
                            });

                        } else {
                            Toast message = Toast.makeText(search_context, "Not enough shares to sell", Toast.LENGTH_SHORT);
                            message.show();
                        }
                    }
                } catch (Exception ex){
                    Toast message = Toast.makeText(search_context, "Please enter a valid amount", Toast.LENGTH_SHORT);
                    message.show();
                }
            }
        });
    }

    public Object[] CalculateSell(String tick, Float amount){
        Float total_held = new Float(0);
        Float total_value = new Float(0);

        ArrayList<StockData> items_to_sell = new ArrayList<StockData>();

        for (int i=0; i<portfolio_data.size(); i++){
            if(portfolio_data.get(i).ticker.equals(tick)){
                Float item_amount = Float.valueOf(portfolio_data.get(i).compOrShares);
                Float item_price = Float.valueOf(portfolio_data.get(i).price);

                if(total_held < amount){
                    items_to_sell.add(portfolio_data.get(i));
                }

                total_held = total_held + item_amount;
                total_value = total_value + (item_amount * item_price);

            }
        }

        Boolean is_sellable = true;
        if(total_held < amount){
            is_sellable = false;
        }

        return new Object[]{is_sellable,items_to_sell, total_held, total_value, };
    }



    public class NewsAdapter extends RecyclerView.Adapter<SearchActivity.NewsAdapter.LineItemContainer> {

        private Context context;
        private ArrayList<NewsData> news_list;


        public NewsAdapter(ArrayList<NewsData> list_of_news) {
            this.context = context;
            this.news_list = list_of_news;
        }

        public class LineItemContainer extends RecyclerView.ViewHolder {
            CardView news_card_whole;
            ImageView card_image;
            TextView source;
            TextView timestamp;
            TextView title;

            public LineItemContainer(View view) {
                super(view);

                news_card_whole = (CardView) view.findViewById(R.id.news_card);
                card_image = (ImageView) view.findViewById(R.id.news_card_image);
                source = (TextView) view.findViewById(R.id.news_card_publisher);
                timestamp = (TextView) view.findViewById(R.id.news_card_time);
                title = (TextView) view.findViewById(R.id.news_card_description);
            }
        }

        @NonNull
        @Override
        public SearchActivity.NewsAdapter.LineItemContainer onCreateViewHolder(@NonNull ViewGroup view_grp, int type) {
            View view = LayoutInflater.from(view_grp.getContext())
                    .inflate(R.layout.news_card_template, view_grp, false);
            return new SearchActivity.NewsAdapter.LineItemContainer(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchActivity.NewsAdapter.LineItemContainer holder, int pos) {
            NewsData news_data = news_list.get(pos);

            Glide.with(search_context)
                    .asBitmap()
                    .load(news_data.card_image_src)
                    .into(holder.card_image);
            holder.source.setText(news_data.source);


            holder.title.setText(news_data.title);

            Date now = new Date();
            Date timestamp_date = new Date(Long.valueOf(news_data.timestamp) * 1000L);

            int days_ago = now.getDay() - timestamp_date.getDay();
            if (days_ago == 0){
                holder.timestamp.setText(String.valueOf(now.getHours() - timestamp_date.getHours()) + " hours ago");
            } else {
                holder.timestamp.setText(String.valueOf(days_ago) + " days ago");
            }

            holder.news_card_whole.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openNewsDialogue(news_data);
                }
            });

        }

        @Override
        public int getItemCount() {
            return news_list.size();
        }

        public void openNewsDialogue(NewsData news_data) {
            AlertDialog.Builder builder = new AlertDialog.Builder(search_context, R.style.RoundedCornersDialog);
            builder.setView(R.layout.dialogue_news);

            alertDialog = builder.create();
            alertDialog.show();

            TextView news_d_source = (TextView) alertDialog.findViewById(R.id.news_d_source);
            news_d_source.setText(news_data.source);

            TextView news_d_timestamp = (TextView) alertDialog.findViewById(R.id.news_d_timestamp);

            SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
            Date timestamp_date = new Date(Long.valueOf(news_data.timestamp) * 1000L);
            String formatted_date = format.format(timestamp_date);
            news_d_timestamp.setText(formatted_date);


            TextView news_d_title = (TextView) alertDialog.findViewById(R.id.news_d_title);
            news_d_title.setText(news_data.title);

            TextView news_d_summary = (TextView) alertDialog.findViewById(R.id.news_d_summary);
            news_d_summary.setText(news_data.summary);


            ImageView web_button = alertDialog.findViewById(R.id.web_button);
            web_button.setImageResource(R.mipmap.ic_chrome_foreground);
            web_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri uri = Uri.parse(news_data.article_link);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            });

            ImageView twitter_button = alertDialog.findViewById(R.id.twitter_button);
            web_button.setImageResource(R.mipmap.ic_twit_foreground);
            twitter_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String text = "Check out this Link: \n";
                    String twitter_post = "https://twitter.com/intent/tweet?text=" + text + "&url=" + news_data.article_link;
                    Uri uri = Uri.parse(twitter_post);
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            });

            ImageView fb_button = alertDialog.findViewById(R.id.fb_button);
            web_button.setImageResource(R.mipmap.ic_fb_foreground);
            fb_button.setOnClickListener(new View.OnClickListener() {
                @Override

                public void onClick(View view) {
                    ShareDialog shareDialog;
                    //FacebookSdk.sdkInitialize(getApplicationContext());
                    shareDialog = new ShareDialog(search_context);

                    ShareLinkContent content = new ShareLinkContent.Builder()
                            .setContentUrl(Uri.parse(news_data.article_link))
                            .build();

                    shareDialog.show(content);
                }
            });
        }
    }


}

