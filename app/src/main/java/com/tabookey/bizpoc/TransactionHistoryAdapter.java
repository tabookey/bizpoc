package com.tabookey.bizpoc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tabookey.bizpoc.api.BitgoUser;
import com.tabookey.bizpoc.api.ExchangeRate;
import com.tabookey.bizpoc.api.PendingApproval;
import com.tabookey.bizpoc.api.Transfer;
import com.tabookey.bizpoc.impl.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


class TransactionHistoryAdapter extends BaseAdapter {

    private final ExchangeRate mExchangeRate;
    private final Context mContext;
    List<BitgoUser> mGuardians;

    private void fillHistoryViewHolder(Transfer transfer, ViewHolder viewHolder) {
        String dateFormat = DateFormat.format("MMMM dd, yyyy", transfer.date).toString();
        viewHolder.dateTextView.setText(dateFormat);
        double value = Math.abs(Utils.integerStringToCoinDouble(transfer.valueString, transfer.token.decimalPlaces));
        String valueFormat = String.format(Locale.US, "%.3f %s", value, transfer.coin.toUpperCase());
        if (transfer.usd != null) {
            String usd = transfer.usd.replaceAll("-", "");
            valueFormat += String.format(Locale.US, " | %s USD", usd);
        }
        viewHolder.valueTextView.setText(valueFormat);

        boolean isOutgoingTx = transfer.valueString.contains("-");
        String label;
        int drawable;
        int labelColor;
        switch (transfer.state) {
            case REJECTED:
                labelColor = mContext.getColor(R.color.reddish_brown);
                label = "Declined";
                drawable = R.drawable.ic_declined;
                break;
            case CANCELLED:
                labelColor = mContext.getColor(R.color.reddish_brown);
                label = "Cancelled";
                drawable = R.drawable.ic_cancelled;
                break;
            case APPROVED:
            default:
                labelColor = mContext.getColor(R.color.black54);
                if (isOutgoingTx) {
                    label = "Sent";
                    drawable = R.drawable.ic_sent;
                } else {
                    label = "Received";
                    drawable = R.drawable.ic_recieved;
                }
                break;

        }

        viewHolder.transactionStatus.setTextColor(labelColor);
        viewHolder.transactionStatus.setText(label);
        viewHolder.transactionStatusIcon.setImageResource(drawable);
    }

    private void fillPendingViewHolder(PendingApproval pending, ViewHolder viewHolder) {
        String dateFormat = DateFormat.format("MMMM dd, yyyy", pending.createDate).toString();
        viewHolder.dateTextView.setText(dateFormat);
        double value = Utils.integerStringToCoinDouble(pending.amount, pending.token.decimalPlaces);

        String valueFormat = String.format(Locale.US, "%.3f %s", value, pending.coin.toUpperCase());
        valueFormat += String.format(Locale.US, " | %.2f USD", value * mExchangeRate.average24h);
        viewHolder.valueTextView.setText(valueFormat);
        viewHolder.remoteTextView.setText(pending.recipientAddr);
        viewHolder.transactionComment.setText(String.format("%s", pending.comment));

        viewHolder.guardiansRecyclerView.setHasFixedSize(true);
        viewHolder.guardiansRecyclerView.setLayoutManager(new GridLayoutManager(mContext, 2));
        viewHolder.guardiansRecyclerView.setAdapter(new ApprovalsRecyclerAdapter(pending.getApprovals(mGuardians), ApprovalsRecyclerAdapter.State.NORMAL));
    }

    private static final int TYPE_ITEM_PENDING = 0;
    private static final int TYPE_ITEM_HISTORY = 1;
    private static final int TYPE_SEPARATOR = 2;

    private ArrayList<Object> mData = new ArrayList<>();
//    private TreeSet<Integer> sectionHeader = new TreeSet<>();

    private LayoutInflater mInflater;

    TransactionHistoryAdapter(Context context, ExchangeRate exchangeRate, List<BitgoUser> guardians) {
        mContext = context;
        mInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mExchangeRate = exchangeRate;
        this.mGuardians = guardians;
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
                    holder.transactionStatus = convertView.findViewById(R.id.transactionStatus);
                    holder.transactionStatusIcon = convertView.findViewById(R.id.transactionStatusIcon);
                    break;
                case TYPE_ITEM_PENDING:
                    convertView = mInflater.inflate(R.layout.pending_transaction_line, null);
                    holder.guardiansRecyclerView = convertView.findViewById(R.id.guardiansRecyclerView);
                    holder.dateTextView = convertView.findViewById(R.id.transactionDate);
                    holder.valueTextView = convertView.findViewById(R.id.transactionValue);
//                    holder.dollarTextView = convertView.findViewById(R.id.transactionDollarValue);
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
        TextView dateTextView, idTextView, valueTextView, remoteTextView, transactionComment, transactionStatus;
        RecyclerView guardiansRecyclerView;
        ImageView transactionStatusIcon;
    }

}

