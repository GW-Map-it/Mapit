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
    String[] mHash = null;
    int[] iNum = null;
    long[] lNum = null;

    TextView tv_number;
    TextView tv_hash;
    TextView tv_num;
    int count_num = 1;

    private int nListCount = 0;

    public ListAdapter(Context context, String[] hashmap, int[] num)
    {
        this.mContext = context;
        this.mHash = hashmap;
        this.iNum = num;

        //String 배열 크기
        for(int i=0;i<hashmap.length;i++) {
            if(hashmap[i] != null) {
                nListCount++;
            }
        }
    }

    public ListAdapter(Context context, String[] hashmap, long[] num)
    {
        this.mContext = context;
        this.mHash = hashmap;
        this.lNum = num;

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

        //(Popular)int형 배열을 받았을 때
        if(iNum != null) {
            tv_num.setText(iNum[position] + "개");
        }
        //(Recent)long형 배열을 받았을 때
        if(lNum != null) {
            if((lNum[position]/60) == 0) {
                tv_num.setText(lNum[position] + "분 전");
            }
            else {
                tv_num.setText((lNum[position]/60) + "시간 " + (lNum[position]%60) + "분 전");
            }
        }

        return convertView;
    }
}
