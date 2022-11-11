package com.wevois.surveyapproval.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.wevois.surveyapproval.SubHouseModel;
import com.wevois.surveyapproval.databinding.ParisarListdataLayoutBinding;

import java.util.ArrayList;

public class ParisarAdapter extends RecyclerView.Adapter<ParisarAdapter.ParentViewHolder> {

    Context context;
    int no,pos;
    SubFormClick subFormClick;
    ArrayList<SubHouseModel> list = new ArrayList<>();

    public ParisarAdapter(Context context, SubFormClick subFormClick) {
        this.no = no;
        this.context = context;
        this.subFormClick = subFormClick;
    }

    public ParisarAdapter(Context context, ArrayList<SubHouseModel> list, SubFormClick subFormClick) {
        this.no = no;
        this.context = context;
        this.list = list;
        this.subFormClick = subFormClick;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        ParisarListdataLayoutBinding binding = ParisarListdataLayoutBinding.inflate(layoutInflater, parent, false);
        return new ParentViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {

        holder.binding.imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                subFormClick.onClickForm(holder.getAdapterPosition());
            }
        });

        if (list.size() > 0)
            holder.binding.etsubHouse.setText(list.get(holder.getAdapterPosition()).name);

    }

    public void addItem(SubHouseModel item, int pos) {
        list.add(item);
        this.pos = pos;
        notifyDataSetChanged();
    }

    public void editItem(SubHouseModel item, int pos) {
        list.set(pos,item);
        this.pos = pos;
        notifyDataSetChanged();
    }

    public SubHouseModel getItemRow(int rowpos) {
        return list.get(rowpos);
    }

    public ArrayList<SubHouseModel> getItemRowdata() {
        return list;
    }


    @Override
    public int getItemCount() {

        Log.e("Size","list add more row"+list.size());
        if (list.isEmpty())
            return 0;
        else
            return list.size();
    }

    class ParentViewHolder extends RecyclerView.ViewHolder {
        ParisarListdataLayoutBinding binding;

        public ParentViewHolder(ParisarListdataLayoutBinding itemVeiw) {
            super(itemVeiw.getRoot());
            this.binding = itemVeiw;
        }
    }

    public interface SubFormClick {
        public void onClickForm(int pos);
    }
}
