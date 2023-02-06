package com.example.homeworknine;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import androidx.appcompat.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private PortfolioFavoritesAdapter portfolio_list_adapter;
    private PortfolioFavoritesAdapter favorites_list_adapter;

    private ArrayList<StockData> portfolio_data;
    private ArrayList<StockData> favorties_data;
    private Float cash_balance;
    private Float net_worth;
    SharedPreferences shared_prefs;
    SharedPreferences.Editor sp_editor;

    PortfolioFavoritesAdapter portfolio_adapter;
    PortfolioFavoritesAdapter favorties_adapter;

    private ArrayList<StockData> portfolio_display_list;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.MyAppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar main_toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(main_toolbar);
        ActionBar action_bar = getSupportActionBar();
        action_bar.setDisplayShowTitleEnabled(false);

        shared_prefs = getSharedPreferences("shared_prefs", Context.MODE_PRIVATE);
        sp_editor = shared_prefs.edit();
        String portfolio_list_str = shared_prefs.getString("portfolio_list", "");
        String favorites_list_str = shared_prefs.getString("favorites_list", "");
        cash_balance = shared_prefs.getFloat("cash_holding", 25000);
        net_worth = new Float(cash_balance);
        portfolio_data = new ArrayList(StockData.fromString(portfolio_list_str));
        favorties_data = new ArrayList(StockData.fromString(favorites_list_str));

        TextView finnhub_link = (TextView) findViewById(R.id.finnhub_link) ;
        finnhub_link.setLinkTextColor(Color.DKGRAY);

        TextView date_header = (TextView) findViewById(R.id.date_header) ;
        String date = new SimpleDateFormat("dd MMMM yyyy").format(new Date());
        date_header.setText(date);

        //Reset for testing
//        portfolio_data.clear();
//        favorties_data.clear();
//        sp_editor.putString("favorites_list", StockData.toString(favorties_data));
//        sp_editor.putString("portfolio_list", StockData.toString(portfolio_data));
//        sp_editor.apply();

        portfolio_display_list = new ArrayList<StockData>();
        portfolio_adapter = new PortfolioFavoritesAdapter(portfolio_display_list, true);
        favorties_adapter = new PortfolioFavoritesAdapter(favorties_data, false);
        portfolio_adapter.context = this;
        favorties_adapter.context = this;

        RecyclerView portfolio_rec_view = (RecyclerView) findViewById(R.id.portfolio_list);
        RecyclerView favorites_rec_view = (RecyclerView) findViewById(R.id.favorites_list);

        portfolio_rec_view.addItemDecoration(new DividerItemDecoration(portfolio_rec_view.getContext(), DividerItemDecoration.VERTICAL));
        favorites_rec_view.addItemDecoration(new DividerItemDecoration(favorites_rec_view.getContext(), DividerItemDecoration.VERTICAL));

        portfolio_rec_view.setAdapter(portfolio_adapter);
        favorites_rec_view.setAdapter(favorties_adapter);

        ReorderHelperCallback portfolio_callback = new ReorderHelperCallback();
        ReorderItemHelper portfolioTouchHelper = new ReorderItemHelper(portfolio_callback);
        portfolioTouchHelper.attachToRecyclerView(portfolio_rec_view);
        //

        ReorderHelperCallback favorites_callback = new ReorderHelperCallback();
        ReorderItemHelper favoritesTouchHelper = new ReorderItemHelper(favorites_callback);
        favoritesTouchHelper.attachToRecyclerView(favorites_rec_view);
        //
        SwipeController swipeController_favorites = new SwipeController(this);
        ItemTouchHelper itemTouchhelper_favorites = new ItemTouchHelper(swipeController_favorites);
        itemTouchhelper_favorites.attachToRecyclerView(favorites_rec_view);

        //TODO: No constraint layout manager? Have to wrap with linear layer manager?
        portfolio_rec_view.setLayoutManager(new LinearLayoutManager(this));
        favorites_rec_view.setLayoutManager(new LinearLayoutManager(this));

        RequestQueue req_queue = Volley.newRequestQueue(this);
        req_queue.getCache().clear();
        populateListsWithJSON(req_queue, portfolio_data, true);
        populateListsWithJSON(req_queue, favorties_data, false);

        portfolio_adapter.notifyDataSetChanged();
        favorties_adapter.notifyDataSetChanged();

        //Hide All
        ConstraintLayout main_layout = (ConstraintLayout)findViewById(R.id.main_layout);
        for (int i=0; i<main_layout.getChildCount(); i++) {
            View subview = main_layout.getChildAt(i);
            if(!(subview instanceof ProgressBar)){
                subview.setVisibility(View.GONE);
            }
        }

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                populateListsWithJSON(req_queue, portfolio_data, true);
                populateListsWithJSON(req_queue, favorties_data, false);
            }
        }, 0, 15000);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void populateListsWithJSON(RequestQueue req_queue, ArrayList<StockData> list_of_stocks, Boolean isPortfolioFlag) {

        portfolio_display_list.clear();
        portfolio_display_list.add(new StockData(true));

        if(isPortfolioFlag){
            HashMap<String, Float[]> stock_totals = new HashMap<String, Float[]>();
            for (int i = 0; i < list_of_stocks.size(); i++) {
                Float[] cur_totVal_totOwned = null;
                if(stock_totals.containsKey(list_of_stocks.get(i).ticker)){
                    cur_totVal_totOwned = stock_totals.get(list_of_stocks.get(i).ticker);
                } else {
                    cur_totVal_totOwned = new Float[]{new Float(0), new Float(0)};
                }

                Float cost = Float.valueOf(list_of_stocks.get(i).price);
                Float shares = Float.valueOf(list_of_stocks.get(i).compOrShares);
                cur_totVal_totOwned[0] = cur_totVal_totOwned[0] + (shares * cost);
                cur_totVal_totOwned[1] = cur_totVal_totOwned[1] + shares;

                stock_totals.put(list_of_stocks.get(i).ticker, cur_totVal_totOwned);
            }

            ArrayList<String> ticker_list = new ArrayList<String>(stock_totals.keySet());
            String query_string = String.join("+", ticker_list);
            String url = "http://192.168.1.68:3000/server/wlprice/" + query_string;

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                for (int i=0; i<ticker_list.size(); i++) {
                                    String cur_price = response.getJSONObject(ticker_list.get(i)).getString("c");

                                    Float total_val = stock_totals.get(ticker_list.get(i))[0];
                                    Float shares_owned = stock_totals.get(ticker_list.get(i))[1];
                                    Float price_pr_share = total_val / shares_owned;
                                    Float float_price = Float.valueOf(cur_price);

                                    Float market_val = shares_owned * float_price;
                                    net_worth = net_worth + market_val;
                                    Float chg_prc_frm_tot_cst = (float_price - price_pr_share) * shares_owned;
                                    Float chg_prc_frm_tot_cst_percent = (chg_prc_frm_tot_cst / total_val) * 100;

                                    String change_str = "$ " + String.format("%.2f",chg_prc_frm_tot_cst) + " (" + String.format("%.2f",chg_prc_frm_tot_cst_percent) + "%)";

                                    StockData dat_obj = new StockData(ticker_list.get(i), market_val.toString(), String.valueOf(shares_owned.intValue()), change_str);
                                    dat_obj.daily_change_amt = chg_prc_frm_tot_cst.toString();
                                    dat_obj.pricePerShare = price_pr_share;
                                    dat_obj.isPortfolioItem = true;

                                    portfolio_display_list.add(dat_obj);

                                }
                                portfolio_adapter.notifyDataSetChanged();

                                ProgressBar spinner = (ProgressBar) findViewById(R.id.spinner);
                                spinner.setVisibility(View.GONE);
                                ConstraintLayout main_layout = (ConstraintLayout)findViewById(R.id.main_layout);
                                for (int i=0; i<main_layout.getChildCount(); i++) {
                                    View subview = main_layout.getChildAt(i);
                                    if(!(subview instanceof ProgressBar)){
                                        subview.setVisibility(View.VISIBLE);
                                    }
                                }

                            } catch (JSONException jsonException) {
                                jsonException.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            System.out.println("ERROR_1!!!\n" + error.getStackTrace().toString());
                        }
                    });

                req_queue.add(jsonObjectRequest);


        } else {

            for (int i = 0; i < list_of_stocks.size(); i++) {
                String current_ticker = list_of_stocks.get(i).ticker;
                String url = "http://192.168.1.68:3000/server/summprice/" + current_ticker;

                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                        (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    String tick = response.getJSONObject("profile").getString("ticker");
                                    String raw_change = String.format("%.2f", Float.valueOf(response.getJSONObject("price").getString("d")));
                                    String raw_change_precent = String.format("%.2f", Float.valueOf(response.getJSONObject("price").getString("dp")));
                                    String change_str = "$ " + raw_change + " (" + raw_change_precent + "%)";
                                    String cur_price = response.getJSONObject("price").getString("c");
                                    String daily_change = response.getJSONObject("price").getString("d");
                                    //String compOrShares = response.getJSONObject("profile").getString("name");

                                    for (int j = 0; j < favorties_data.size(); j++) {
                                        StockData item_to_modify = favorties_data.get(j);
                                        if (item_to_modify.ticker.equals(tick)) {
                                            item_to_modify.change = change_str;
                                            item_to_modify.daily_change_amt = daily_change;
                                            item_to_modify.isPortfolioItem = false;

//                                            Float total_held = new Float(0);
//                                            Float total_value = new Float(0);
//                                            for (int i = 0; i < favorties_data.size(); i++) {
//                                                if (favorties_data.get(i).ticker.equals(tick)) {
//                                                    Float item_amount = Float.valueOf(favorties_data.get(i).compOrShares);
//                                                    Float item_price = Float.valueOf(favorties_data.get(i).price);
//                                                    total_held = total_held + item_amount;
//                                                    total_value = total_value + item_price;
//
//                                                }
//                                            }
//                                            item_to_modify.pricePerShare = new Float(total_value / total_held);
                                        }
                                    }

                                    favorties_adapter.notifyDataSetChanged();
                                    sp_editor.putString("favorites_list", StockData.toString(favorties_data));
                                    sp_editor.apply();

                                } catch (JSONException jsonException) {
                                    jsonException.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                System.out.println("ERROR_2!!!\n" + error.getStackTrace().toString());
                            }
                        });

                req_queue.add(jsonObjectRequest);
            }
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.header_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(true);


        TextView toolbar_text = (TextView) findViewById(R.id.toolbar_text);
        ImageView toolbar_close_search = (ImageView) findViewById(R.id.toolbar_close_search);
        toolbar_text.setVisibility(View.VISIBLE);
        toolbar_close_search.setVisibility(View.GONE);

        SearchView.SearchAutoComplete autocomplete = (SearchView.SearchAutoComplete) searchView.findViewById(androidx.appcompat.R.id.search_src_text);

        //dropdown_options = new String[]{"Apple", "Amazon", "Amd", "Microsoft", "Microwave", "MicroNews", "Intel", "Intelligence"};
        //autocomplete.setDropDownBackgroundResource(android.R.color.holo_blue_light);
        ArrayList<String> dropdown_options = new ArrayList<String>();
        ArrayAdapter<String> dropdown_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, dropdown_options);
        autocomplete.setAdapter(dropdown_adapter);
        autocomplete.setThreshold(1);


        autocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int itemIndex, long id) {
                String queryString=(String)adapterView.getItemAtPosition(itemIndex);
                String[] queryString_val = queryString.split(" ");
                autocomplete.setText("" + queryString_val[0]);
                searchView.setEnabled(true);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(searchView.isEnabled()){
                    Intent intent = new Intent(getBaseContext(), SearchActivity.class);
                    intent.putExtra(SearchManager.QUERY, query);
                    intent.setAction(Intent.ACTION_SEARCH);
                    startActivity(intent);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String partial_query) {
                searchView.setEnabled(false);
                String url = "http://192.168.1.68:3000/server/autocomplete/" + partial_query;
                RequestQueue req_queue = Volley.newRequestQueue(getBaseContext());
                req_queue.getCache().clear();

                if(partial_query.length() > 1) {
                    JsonArrayRequest jsonObjectRequest = new JsonArrayRequest
                            (Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                                @Override
                                public void onResponse(JSONArray response) {
                                    try {
                                        dropdown_adapter.clear();
                                        //dropdown_options = new String[20];
                                        for (int i = 0; i < response.length(); i++) {
                                            if (i == 20) {
                                                break;
                                            }
                                            String match = (String) response.get(i);
                                            //dropdown_options[i] = match;
                                            dropdown_adapter.add(match);
                                            //dropdown_options.add(match);
                                        }
                                        dropdown_adapter.notifyDataSetChanged();
                                        //dropdown_adapter.notifyAll();
                                    } catch (Exception jsonException) {
                                        jsonException.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    System.out.println("ERROR_3!!!\n" + error.getStackTrace().toString());
                                }
                            });
                    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                            10000,
                            2,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
                    req_queue.add(jsonObjectRequest);
                }
                return true;
            }
        });




        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolbar_text.setVisibility(View.GONE);
                toolbar_close_search.setVisibility(View.VISIBLE);
            }
        });

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                toolbar_text.setVisibility(View.VISIBLE);
                toolbar_close_search.setVisibility(View.GONE);
                return false;
            }
        });

        toolbar_close_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchView.setIconified(true);
            }
        });
        return true;
    }

    public class PortfolioFavoritesAdapter extends RecyclerView.Adapter<PortfolioFavoritesAdapter.LineItemContainer> {

        private Boolean isPorfolioFlag;
        private Context context;
        private ArrayList<StockData> list_of_stocks;


        public PortfolioFavoritesAdapter(ArrayList<StockData> list_of_stocks, Boolean isPorfolioFlag) {
            this.context = context;
            this.list_of_stocks = list_of_stocks;
            this.isPorfolioFlag = isPorfolioFlag;
        }

        public class LineItemContainer extends RecyclerView.ViewHolder {
            TextView ticker;
            TextView price;
            TextView compOrShares;
            TextView change;
            ImageView trend_icon;
            ImageView into_button;

            public LineItemContainer(View view) {
                super(view);

                ticker = (TextView) view.findViewById(R.id.ticker);
                price = (TextView) view.findViewById(R.id.price);
                compOrShares = (TextView) view.findViewById(R.id.compOrShares);
                change = (TextView) view.findViewById(R.id.change);
                trend_icon = (ImageView) view.findViewById(R.id.trending_img);
                into_button = (ImageView) view.findViewById(R.id.button_into_stock);
            }
        }

        @NonNull
        @Override
        public LineItemContainer onCreateViewHolder(@NonNull ViewGroup view_grp, int type) {
            View view = LayoutInflater.from(view_grp.getContext())
                    .inflate(R.layout.stock_card_template, view_grp, false);
            return new LineItemContainer(view);
        }

        @Override
        public void onBindViewHolder(@NonNull LineItemContainer holder, int pos) {
            StockData stock_data = list_of_stocks.get(pos);

            if(stock_data.is_net_worth_card){
                holder.ticker.setText("Net Worth");
                holder.price.setText("Cash Balance");
                holder.compOrShares.setText(String.format("%.2f",net_worth));
                holder.change.setText(String.format("%.2f", cash_balance));

                holder.ticker.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                holder.price.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                holder.compOrShares.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                holder.change.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);

                holder.compOrShares.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                holder.change.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));

                holder.ticker.setTextColor(Color.GRAY);
                holder.price.setTextColor(Color.GRAY);
                holder.compOrShares.setTextColor(Color.GRAY);
                holder.change.setTextColor(Color.GRAY);

                holder.trend_icon.setVisibility(View.GONE);
                holder.into_button.setVisibility(View.GONE);
            } else {

                holder.into_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, SearchActivity.class);
                        intent.putExtra(SearchManager.QUERY, stock_data.ticker);
                        intent.setAction(Intent.ACTION_SEARCH);
                        startActivity(intent);
                    }
                });

                holder.ticker.setText(stock_data.ticker);
                holder.price.setText(stock_data.price);
                holder.change.setText(stock_data.change);

                if (stock_data.isPortfolioItem) {
                    holder.compOrShares.setText(stock_data.compOrShares + " shares");
                } else {
                    holder.compOrShares.setText(stock_data.compOrShares);
                }

                if (!stock_data.daily_change_amt.equals("")) {
                    if (Float.valueOf(stock_data.daily_change_amt) < new Float(0)) {
                        holder.change.setTextColor(Color.RED);
                        holder.trend_icon.setColorFilter(Color.RED);
                    } else if (Float.valueOf(stock_data.daily_change_amt) == new Float(0)) {
                        holder.change.setTextColor(Color.DKGRAY);
                        holder.trend_icon.setColorFilter(Color.GRAY);
                    } else {
                        holder.change.setTextColor(Color.rgb(23, 123, 53));
                        holder.trend_icon.setColorFilter(Color.rgb(23, 123, 53));
                        holder.trend_icon.setImageResource(R.drawable.trending_up);
                    }
                }
            }
        }

        @Override
        public int getItemCount() {
             return list_of_stocks.size();
        }
    }

    class ReorderItemHelper extends ItemTouchHelper {
        public ReorderItemHelper(@NonNull ReorderHelperCallback callback) {
            super(callback);
        }
    }


    class ReorderHelperCallback extends ItemTouchHelper.Callback {
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public boolean isItemViewSwipeEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.END | ItemTouchHelper.START | ItemTouchHelper.LEFT | 0 | ItemTouchHelper.RIGHT;
            return makeMovementFlags(dragFlags, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            recyclerView.getAdapter().notifyItemMoved(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            viewHolder.getBindingAdapter().notifyItemRemoved(position);

        }
    }

    class SwipeController extends ItemTouchHelper.Callback {
        private boolean swipeBack = false;
        private static final float buttonWidth = 300;

        Context mContext;
        private Paint mClearPaint;
        private ColorDrawable mBackground;
        private int backgroundColor;
        private Drawable deleteDrawable;
        private int intrinsicWidth;
        private int intrinsicHeight;


        SwipeController(Context context) {
            mContext = context;
            mBackground = new ColorDrawable();
            backgroundColor = Color.parseColor("#b80f0a");
            mClearPaint = new Paint();
            mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            deleteDrawable = ContextCompat.getDrawable(mContext, R.drawable.ic_clock);
            intrinsicWidth = deleteDrawable.getIntrinsicWidth();
            intrinsicHeight = deleteDrawable.getIntrinsicHeight();
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(0, ItemTouchHelper.LEFT);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder viewHolder1) {
            return false;
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);

            View itemView = viewHolder.itemView;
            int itemHeight = itemView.getHeight();

            if (dX == 0 && !isCurrentlyActive) {
                clearCanvas(c, itemView.getRight() + dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom());
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                return;
            }

            mBackground.setColor(backgroundColor);
            mBackground.setBounds(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
            mBackground.draw(c);

            int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
            int deleteIconMargin = (itemHeight - intrinsicHeight) / 2;
            int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicWidth;
            int deleteIconRight = itemView.getRight() - deleteIconMargin;
            int deleteIconBottom = deleteIconTop + intrinsicHeight;


            deleteDrawable.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
            deleteDrawable.draw(c);

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);


        }

        private void clearCanvas(Canvas c, Float left, Float top, Float right, Float bottom) {
            c.drawRect(left, top, right, bottom, mClearPaint);

        }

        @Override
        public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
            return 0.7f;
        }

        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

            ArrayList<StockData> list_of_items = ((PortfolioFavoritesAdapter) viewHolder.getBindingAdapter()).list_of_stocks;
            int idx_to_delete = viewHolder.getBindingAdapterPosition();

            for (int j=0; j<list_of_items.size(); j++) {
                if(list_of_items.get(j).ticker.equals(((PortfolioFavoritesAdapter.LineItemContainer) viewHolder).ticker.getText())){
                    list_of_items.remove(j);
                }
            }
            ((PortfolioFavoritesAdapter) viewHolder.getBindingAdapter()).notifyDataSetChanged();
            sp_editor.putString("favorites_list", StockData.toString(favorties_data));
            sp_editor.apply();
        }
    }




}