package com.scorfield.barfinder.beans;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.scorfield.barfinder.MapsActivity;
import com.scorfield.barfinder.R;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class BarAdapter extends RecyclerView.Adapter<BarAdapter.ViewHolder> {

    private ArrayList<BarBean> barBeans;
    DecimalFormat df = new DecimalFormat("0.0");

    public BarAdapter(ArrayList<BarBean> barBeans) {
        this.barBeans = barBeans;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem = layoutInflater.inflate(R.layout.list_item, parent, false);
        return new ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BarBean barBean = barBeans.get(position);
        holder.txt_shop.setText(barBean.getShop());
        holder.txt_address.setText(barBean.getAddress());
        holder.txt_distance.setText(df.format(barBean.getDistance()) + "km");
        holder.ratingBar.setRating(barBean.getRating());
        if (barBean.isOpen()) {
            holder.open.setImageResource(R.drawable.ic_open_now);
        } else {
            holder.open.setImageResource(R.drawable.ic_space);
        }
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent in = new Intent(view.getContext(), MapsActivity.class);
                in.putExtra("me", barBean.getMyLatLng());
                in.putExtra("store", barBean.getStoreLatLng());
                in.putExtra("placeId",barBean.getPlaceId());
                view.getContext().startActivity(in);
            }
        });
    }

    @Override
    public int getItemCount() {
        return barBeans.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txt_shop, txt_address, txt_distance;
        RatingBar ratingBar;
        ImageView open;
        LinearLayout linearLayout;

        ViewHolder(View itemView) {
            super(itemView);
            this.txt_shop = itemView.findViewById(R.id.txt_shop);
            this.txt_address = itemView.findViewById(R.id.txt_address);
            this.txt_distance = itemView.findViewById(R.id.txt_distance);
            this.ratingBar = itemView.findViewById(R.id.ratingBar);
            this.open = itemView.findViewById(R.id.open);
            linearLayout = itemView.findViewById(R.id.linearLayout);
        }
    }
}
