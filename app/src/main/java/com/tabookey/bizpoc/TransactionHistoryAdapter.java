package com.tabookey.bizpoc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


class TransactionHistoryAdapter extends BaseAdapter {

    private final ExchangeRate mExchangeRate;

    private void fillHistoryViewHolder(Transfer transfer, ViewHolder viewHolder) {
        String dateFormat = DateFormat.format("dd/MM/yy, hh:mm a", transfer.date).toString();
        viewHolder.dateTextView.setText(dateFormat);
        double value = Utils.integerStringToCoinDouble(transfer.valueString, transfer.token.decimalPlaces);
        viewHolder.valueTextView.setText(String.format(Locale.US, "%.6f %s", value, transfer.coin.toUpperCase()));
        if ( transfer.usd!=null ) {
            String usd = transfer.usd.replaceAll("-", "");
            viewHolder.dollarTextView.setText(String.format(Locale.US, "$%s", usd));
        }

        viewHolder.remoteTextView.setText(transfer.remoteAddress);
        boolean isOutgoingTx = transfer.valueString.contains("-");
        viewHolder.transactionStatus.setText(transfer.isRejected ? "Rejected" : isOutgoingTx ? "Sent" : "Received");
        viewHolder.transactionStatusIcon.setImageResource(transfer.isRejected ? android.R.drawable.ic_menu_close_clear_cancel :
                 isOutgoingTx ? R.drawable.ic_arrow_upward_black_24dp : R.drawable.ic_arrow_downward_black_24dp);
    }

    private void fillPendingViewHolder(PendingApproval pending, ViewHolder viewHolder) {
        String dateFormat = DateFormat.format("MMMM ddQQQQ yyyy, hh:mm:ss a", pending.createDate).toString();
        dateFormat = dateFormat.replaceAll("QQQQ", Utils.getDayOfMonthSuffix(pending.createDate));
        viewHolder.dateTextView.setText(dateFormat);
        double value = Utils.integerStringToCoinDouble(pending.amount, pending.token.decimalPlaces);
        viewHolder.valueTextView.setText(String.format(Locale.US, "%.6f %s", value, pending.coin.toUpperCase()));
        viewHolder.dollarTextView.setText(String.format(Locale.US, "$%.2f", value * mExchangeRate.average24h));
        viewHolder.remoteTextView.setText(pending.recipientAddr);
        viewHolder.transactionComment.setText(String.format("Memo %s", pending.comment));
    }

    private static final int TYPE_ITEM_PENDING = 0;
    private static final int TYPE_ITEM_HISTORY = 1;
    private static final int TYPE_SEPARATOR = 2;

    private ArrayList<Object> mData = new ArrayList<>();
//    private TreeSet<Integer> sectionHeader = new TreeSet<>();

    private LayoutInflater mInflater;

    TransactionHistoryAdapter(Context context, ExchangeRate exchangeRate) {
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mExchangeRate = exchangeRate;
    }

    void addItems(List item) {
        mData.addAll(item);
        notifyDataSetChanged();
    }


    void addItem(final String item) {
        mData.add(item);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mData.get(position) instanceof PendingApproval) {
            return TYPE_ITEM_PENDING;
        } else if (mData.get(position) instanceof Transfer) {
            return TYPE_ITEM_HISTORY;
        } else {
            return TYPE_SEPARATOR;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("InflateParams")
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        int rowType = getItemViewType(position);

        if (convertView == null) {
            holder = new ViewHolder();
            switch (rowType) {
                case TYPE_ITEM_HISTORY:
                    convertView = mInflater.inflate(R.layout.transaction_line, null);
                    holder.dateTextView = convertView.findViewById(R.id.transactionDate);
                    holder.valueTextView = convertView.findViewById(R.id.transactionValue);
                    holder.dollarTextView = convertView.findViewById(R.id.transactionDollarValue);
                    holder.remoteTextView = convertView.findViewById(R.id.transactionRemoteAddress);
                    holder.transactionStatus = convertView.findViewById(R.id.transactionStatus);
                    holder.transactionStatusIcon = convertView.findViewById(R.id.transactionStatusIcon);
                    break;
                case TYPE_ITEM_PENDING:
                    convertView = mInflater.inflate(R.layout.pending_transaction_line, null);
                    holder.guardiansListView = convertView.findViewById(R.id.guardiansListView);
                    holder.dateTextView = convertView.findViewById(R.id.transactionDate);
                    holder.valueTextView = convertView.findViewById(R.id.transactionValue);
                    holder.dollarTextView = convertView.findViewById(R.id.transactionDollarValue);
                    holder.remoteTextView = convertView.findViewById(R.id.transactionRemoteAddress);
                    holder.transactionComment = convertView.findViewById(R.id.transactionComment);
                    break;
                case TYPE_SEPARATOR:
                default:
                    convertView = mInflater.inflate(R.layout.section_header, null);
                    holder.idTextView = convertView.findViewById(R.id.textSeparator);
                    break;
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        switch (rowType) {
            case TYPE_ITEM_HISTORY:
                Transfer transfer = (Transfer) mData.get(position);
                fillHistoryViewHolder(transfer, holder);
                break;

            case TYPE_SEPARATOR:
                String separator = (String) mData.get(position);
                holder.idTextView.setText(separator);
                break;
            case TYPE_ITEM_PENDING:
                PendingApproval pendingApproval = (PendingApproval) mData.get(position);
                fillPendingViewHolder(pendingApproval, holder);
                break;
        }

        return convertView;
    }


    public static class ViewHolder {
        TextView dateTextView, idTextView, valueTextView, dollarTextView, remoteTextView, transactionComment, transactionStatus;
        ListView guardiansListView;
        ImageView transactionStatusIcon;
    }

}

