package com.kw.mapit;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListAdapter extends BaseAdapter {
    Context mContext;
    LayoutInflater inflater = null;
    String[] mHash;
    int[] mNum;

    TextView tv_number;
    TextView tv_hash;
    TextView tv_num;
    int count_num = 1;

    private int nListCount = 0;

    public ListAdapter(Context context, String[] hashmap, int[] num)
    {
        this.mContext = context;
        this.mHash = hashmap;
        this.mNum = num;

        //String 배열 크기
        for(int i=0;i<hashmap.length;i++) {
            if(hashmap[i] != null) {
                nListCount++;
            }
        }
    }

    @Override
    public int getCount() {
        return nListCount;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null)
        {
            final Context context = parent.getContext();
            if(inflater == null) {
                inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            }
            convertView = inflater.inflate(R.layout.listview_hashtag, parent, false);
        }

        convertView.setTag(position);

        tv_number = (TextView)convertView.findViewById(R.id.tv_number);
        tv_hash = (TextView)convertView.findViewById(R.id.tv_hash);
        tv_num = (TextView)convertView.findViewById(R.id.tv_num);

        //인기 Hashtag 출력
        tv_number.setText(position+1 + ". ");
        tv_hash.setText(mHash[position]);
        tv_num.setText(mNum[position] + "개");

        return convertView;
    }
}
