package com.example.homeworknine;

import java.util.ArrayList;

public class StockData {
    String ticker;
    String price;
    String compOrShares;
    String change;
    String daily_change_amt;
    Boolean isPortfolioItem;
    Float pricePerShare;
    Boolean is_net_worth_card = false;


    public StockData(Boolean is_net_worth_card){
        this.ticker = "";
        this.price = "";
        this.compOrShares = "";
        this.change = "";
        this.daily_change_amt = "";
        this.isPortfolioItem = false;
        this.pricePerShare = new Float(0);
        this.is_net_worth_card = is_net_worth_card;
    }

    public StockData(String ticker, String price, String cOrS, String change){
        this.ticker = ticker;
        this.price = price;
        this.compOrShares = cOrS;
        this.change = change;

        this.daily_change_amt = "";
        this.isPortfolioItem = false;
        this.pricePerShare = new Float(0);
        this.is_net_worth_card = false;
    }

    public static ArrayList<StockData> fromString(String string_input){
        ArrayList<StockData> object_list = new ArrayList<StockData>();
        if (string_input == ""){
            return new ArrayList<StockData>();
        }

        String[] string_list = string_input.split(",");
        for (int j=0; j<string_list.length; j++) {
            String obj_list = string_list[j];
            String[] str_data_objs = obj_list.split(":");


            StockData data_obj = null;
            for (int i = 0; i < str_data_objs.length; i++) {

                if(str_data_objs.length == 3){
                    data_obj = new StockData(
                            str_data_objs[0], //ticker
                            str_data_objs[1], //price
                            str_data_objs[2], //cOrS
                            ""); //change
                } else {
                    data_obj = new StockData(
                            str_data_objs[0], //ticker
                            str_data_objs[1], //price
                            str_data_objs[2], //cOrS
                            str_data_objs[3]); //change
                }
            }
            object_list.add(data_obj);
        }
        return object_list;
    }

    public static String toString(ArrayList<StockData> data_list){
        String output_string = "";
        for (int i=0; i<data_list.size(); i++) {
            StockData object_item = data_list.get(i);
            String str_item = "";
            str_item = str_item + object_item.ticker + ":";
            str_item = str_item + object_item.price + ":";
            str_item = str_item + object_item.compOrShares + ":";
            str_item = str_item + object_item.change;

            if(output_string == ""){
                output_string = str_item;
            } else {
                output_string = output_string + "," + str_item;
            }

        }

        return output_string;
    }

    public static StockData getItemByTicker(ArrayList<StockData> data_list, String tick){
        for (int i=0; i<data_list.size(); i++) {
            if(data_list.get(i).ticker.equals(tick) ){
                return data_list.get(i);
            }
        }
        return null;
    }

    public static StockData deleteItemByTicker(ArrayList<StockData> data_list, String tick){
        for (int i=0; i<data_list.size(); i++) {
            if(data_list.get(i).ticker.equals(tick)){
                return data_list.remove(i);
            }
        }
        return null;
    }

}
